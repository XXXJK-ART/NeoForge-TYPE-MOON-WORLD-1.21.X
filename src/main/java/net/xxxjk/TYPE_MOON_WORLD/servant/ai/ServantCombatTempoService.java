package net.xxxjk.TYPE_MOON_WORLD.servant.ai;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.combat.ai.AiBrain;
import net.xxxjk.TYPE_MOON_WORLD.combat.ai.AiControl;
import net.xxxjk.TYPE_MOON_WORLD.combat.ai.AiIntent;
import net.xxxjk.TYPE_MOON_WORLD.combat.ai.ServantPlannedActionExecutor;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatSystem;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;
import net.xxxjk.TYPE_MOON_WORLD.world.terrain.TerrainImpactProfile;
import net.xxxjk.TYPE_MOON_WORLD.world.terrain.TerrainImpactService;

/** Keeps servant combat from falling into indefinite cooldown, orbit, or path stalls. */
public final class ServantCombatTempoService {
   public static final int PRESSURE_START_TICKS = 40;
   public static final int MOBILITY_ESCALATION_TICKS = 100;
   public static final int BREAKOUT_ESCALATION_TICKS = 160;
   public static final int MAX_DISCONNECTED_TICKS = 200;
   public static final int MELEE_PRESSURE_MIN = 100;
   public static final int MELEE_PRESSURE_MAX = 140;
   public static final int ORBIT_DETECTION_TICKS = 40;
   public static final int MELEE_OVERRIDE_TICKS = 100;

   private static final String PREFIX = "TypeMoonCombatTempo";
   private static final String TARGET = PREFIX + "Target";
   private static final String LAST_CONTACT = PREFIX + "LastContact";
   private static final String LAST_PROGRESS = PREFIX + "LastProgress";
   private static final String LAST_X = PREFIX + "LastX";
   private static final String LAST_Y = PREFIX + "LastY";
   private static final String LAST_Z = PREFIX + "LastZ";
   private static final String PRESSURE_UNTIL = PREFIX + "PressureUntil";
   private static final String LAST_BASIC = PREFIX + "LastBasic";
   private static final String LAST_FAILURE = PREFIX + "LastFailure";
   private static final String LAST_BREAK = PREFIX + "LastBreak";
   private static final String LAST_DISTANCE = PREFIX + "LastDistance";
   private static final String LAST_ANGLE = PREFIX + "LastAngle";
   private static final String ANGLE_TRAVEL = PREFIX + "AngleTravel";
   private static final String ORBIT_STALL = PREFIX + "OrbitStall";
   private static final String ORBIT_REVERSALS = PREFIX + "OrbitReversals";
   private static final String ORBIT_DIRECTION = PREFIX + "OrbitDirection";
   private static final String ORBITING = PREFIX + "Orbiting";
   private static final String MELEE_OVERRIDE = PREFIX + "MeleeOverride";
   private static final int BASIC_COOLDOWN = 10;
   private static final double MIN_BASIC_REACH = 2.75;
   private static final double MAX_BASIC_REACH = 4.35;
   private static final double BASIC_REACH_BASE = 2.65;
   private static final double BASIC_REACH_WIDTH_SCALE = 0.45;
   private static final double BASIC_VERTICAL_TOLERANCE = 2.5;

   private ServantCombatTempoService() { }

   public static TempoState tick(ServantEntity servant, LivingEntity target, long now) {
      if (servant == null || target == null || !target.isAlive() || servant.level() != target.level()) {
         clear(servant);
         return TempoState.EMPTY;
      }
      CompoundTag data = servant.getPersistentData();
      if (!data.hasUUID(TARGET) || !target.getUUID().equals(data.getUUID(TARGET))) {
         data.putUUID(TARGET, target.getUUID());
         data.putLong(LAST_CONTACT, now);
         data.putLong(LAST_PROGRESS, now);
         data.putDouble(LAST_X, servant.getX());
         data.putDouble(LAST_Y, servant.getY());
         data.putDouble(LAST_Z, servant.getZ());
         initializeOrbitMetrics(data, servant, target);
         data.remove(PRESSURE_UNTIL);
      }

      double dx = servant.getX() - data.getDouble(LAST_X);
      double dy = servant.getY() - data.getDouble(LAST_Y);
      double dz = servant.getZ() - data.getDouble(LAST_Z);
      if (dx * dx + dy * dy + dz * dz >= 0.04) {
         data.putLong(LAST_PROGRESS, now);
         data.putDouble(LAST_X, servant.getX());
         data.putDouble(LAST_Y, servant.getY());
         data.putDouble(LAST_Z, servant.getZ());
      }
      double distance = servant.distanceTo(target);
      updateOrbitMetrics(servant, target, data, now, distance);
      if (distance <= 4.2) {
         int pressure = pressureWindow(servant, target);
         data.putLong(PRESSURE_UNTIL, Math.max(data.getLong(PRESSURE_UNTIL), now + pressure));
      }
      long lastContact = data.getLong(LAST_CONTACT);
      long disconnected = Math.max(0L, now - lastContact);
      boolean orbiting = data.getBoolean(ORBITING);
      // Orbiting changes the movement choice immediately, but the hard melee
      // takeover remains the explicit five-second no-contact deadline.
      boolean override = disconnected >= MELEE_OVERRIDE_TICKS;
      data.putBoolean(MELEE_OVERRIDE, override);
      int stage = stageForDisconnectedTicks(disconnected);
      return new TempoState(target.getUUID(), lastContact, data.getLong(LAST_PROGRESS),
         stage, data.getLong(PRESSURE_UNTIL) > now, disconnected, orbiting, override);
   }

   public static boolean submitFallback(ServantEntity servant, LivingEntity target, AiBrain brain, long now,
                                         TempoState state) {
      if (servant == null || target == null || brain == null || state == null || !target.isAlive()) return false;
      if (ServantPlannedActionExecutor.isActive(servant)) return false;
      boolean imminentContact = closingContactImminent(servant, target);
      boolean closeEnough = canAttemptBasicAttack(servant, target) || imminentContact;
      // Before the first 40-tick deadline, yielding lets character-specific
      // helpers retain final execution authority.  The legacy tail guard still
      // supplies a basic attack when those helpers decline to act.
      boolean forced = state.stage() > 0;
      if (!forced && !imminentContact) return false;
      if (closeEnough) {
         brain.submit(AiIntent.attempt(
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("typemoonworld", "ai/basic_attack"),
            AiIntent.PRIORITY_ATTACK + 2, 80.0 + state.stage() * 20.0, 1, true,
            () -> tryBasicAttack(servant, target, now), AiControl.ATTACK, AiControl.LOOK));
      } else {
         brain.submit(AiIntent.attempt(
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("typemoonworld", "ai/tempo_approach"),
            AiIntent.PRIORITY_ATTACK + (state.stage() >= 2 ? 2 : 0),
            120.0 + state.disconnectedTicks(), 2, true,
            () -> forceApproach(servant, target, now, state.stage()), AiControl.MOVE, AiControl.LOOK));
      }
      return true;
   }

   /** Legacy AI calls this after its helpers; it supplies the same no-wait guarantee. */
   public static void enforceLegacy(ServantEntity servant, LivingEntity target, long now) {
      TempoState state = tick(servant, target, now);
      if (state == TempoState.EMPTY || ServantCombatSystem.cannotAct(servant) || servant.isPerformingAction()) return;
      if (tryBasicAttack(servant, target, now)) {
         return;
      }
      if (state.stage() > 0) {
         forceApproach(servant, target, now, state.stage());
      }
   }

   public static boolean tryBasicAttack(ServantEntity servant, LivingEntity target, long now) {
      if (servant == null || target == null || !target.isAlive() || servant.level() != target.level()
         || ServantCombatSystem.cannotAct(servant) || servant.isPerformingAction()
         || !canAttemptBasicAttack(servant, target) && !closingContactImminent(servant, target)) return false;
      CompoundTag data = servant.getPersistentData();
      if (now - data.getLong(LAST_BASIC) < BASIC_COOLDOWN) return false;
      data.putLong(LAST_BASIC, now);
      servant.getNavigation().stop();
      servant.faceToward(target.position());
      if (closingContactImminent(servant, target)) {
         Vec3 motion = servant.getDeltaMovement();
         servant.setDeltaMovement(motion.x * 0.28, motion.y, motion.z * 0.28);
         servant.hasImpulse = true;
      }
      boolean hit = servant.doBasicHurtTarget(target);
      servant.triggerBasicAttackAnimation();
      recordContact(servant, target, hit ? ContactType.DAMAGE : ContactType.BLOCKED, now);
      return true;
   }

   /** Size-aware range shared by ground, flight and high-speed contact handling. */
   public static double basicAttackReach(LivingEntity attacker, LivingEntity target) {
      if (attacker == null || target == null) return MIN_BASIC_REACH;
      return basicAttackReach(attacker.getBbWidth(), target.getBbWidth());
   }

   static double basicAttackReach(double attackerWidth, double targetWidth) {
      double widths = Math.max(0.0, attackerWidth) + Math.max(0.0, targetWidth);
      return Math.max(MIN_BASIC_REACH,
         Math.min(MAX_BASIC_REACH, BASIC_REACH_BASE + widths * BASIC_REACH_WIDTH_SCALE));
   }

   public static boolean canAttemptBasicAttack(ServantEntity servant, LivingEntity target) {
      if (servant == null || target == null || !target.isAlive() || servant.level() != target.level()) return false;
      double distance = servant.distanceTo(target);
      double verticalGap = Math.abs(
         servant.getY() + servant.getBbHeight() * 0.5
            - target.getY() - target.getBbHeight() * 0.5);
      boolean lineOfSight = servant.getSensing().hasLineOfSight(target);
      return basicAttackGeometry(distance, verticalGap, lineOfSight,
         servant.getBbWidth(), target.getBbWidth());
   }

   static boolean basicAttackGeometry(double distance, double verticalGap, boolean lineOfSight,
                                      double attackerWidth, double targetWidth) {
      return distance <= basicAttackReach(attackerWidth, targetWidth)
         && verticalGap <= BASIC_VERTICAL_TOLERANCE
         && (lineOfSight || distance <= 1.8);
   }

   static boolean sweptContact(AABB attackerBounds, Vec3 motion, AABB targetBounds) {
      if (attackerBounds == null || motion == null || targetBounds == null) return false;
      AABB swept = attackerBounds.expandTowards(motion.scale(1.35)).inflate(0.25, 0.15, 0.25);
      return swept.intersects(targetBounds.inflate(0.05));
   }

   private static boolean closingContactImminent(ServantEntity servant, LivingEntity target) {
      if (servant == null || target == null || !target.isAlive() || servant.level() != target.level()) return false;
      Vec3 motion = servant.getDeltaMovement();
      Vec3 toward = target.position().subtract(servant.position()).multiply(1.0, 0.0, 1.0);
      if (motion.horizontalDistanceSqr() < 0.0225 || toward.lengthSqr() < 1.0E-4
         || motion.dot(toward.normalize()) < 0.12) return false;
      double verticalGap = Math.abs(
         servant.getY() + servant.getBbHeight() * 0.5
            - target.getY() - target.getBbHeight() * 0.5);
      if (verticalGap > BASIC_VERTICAL_TOLERANCE
         || !servant.getSensing().hasLineOfSight(target) && servant.distanceTo(target) > 1.8) return false;
      return sweptContact(servant.getBoundingBox(), motion, target.getBoundingBox());
   }

   public static void recordContact(ServantEntity servant, LivingEntity target, ContactType type, long now) {
      // Record the exchange even when this hit is lethal; the next tick will
      // clear the target state through the normal dead-target path.
      if (servant == null || target == null) return;
      CompoundTag data = servant.getPersistentData();
      data.putUUID(TARGET, target.getUUID());
      data.putLong(LAST_CONTACT, now);
      data.putLong(LAST_FAILURE, type.ordinal());
      data.putLong(PRESSURE_UNTIL, Math.max(data.getLong(PRESSURE_UNTIL), now + pressureWindow(servant, target)));
      initializeOrbitMetrics(data, servant, target);
      data.putBoolean(ORBITING, false);
      data.putBoolean(MELEE_OVERRIDE, false);
   }

   public static void clear(ServantEntity servant) {
      if (servant == null) return;
      CompoundTag data = servant.getPersistentData();
      data.remove(TARGET);
      data.remove(LAST_CONTACT);
      data.remove(LAST_PROGRESS);
      data.remove(LAST_X);
      data.remove(LAST_Y);
      data.remove(LAST_Z);
      data.remove(PRESSURE_UNTIL);
      data.remove(LAST_FAILURE);
      data.remove(LAST_DISTANCE);
      data.remove(LAST_ANGLE);
      data.remove(ANGLE_TRAVEL);
      data.remove(ORBIT_STALL);
      data.remove(ORBIT_REVERSALS);
      data.remove(ORBIT_DIRECTION);
      data.remove(ORBITING);
      data.remove(MELEE_OVERRIDE);
   }

   public static boolean inMeleePressure(ServantEntity servant, long now) {
      return servant != null && servant.getPersistentData().getLong(PRESSURE_UNTIL) > now;
   }

   public static boolean isOrbiting(ServantEntity servant) {
      return servant != null && servant.getPersistentData().getBoolean(ORBITING);
   }

   public static boolean isMeleeOverride(ServantEntity servant) {
      return servant != null && servant.getPersistentData().getBoolean(MELEE_OVERRIDE);
   }

   public static long disconnectedTicks(ServantEntity servant, long now) {
      if (servant == null || !servant.getPersistentData().contains(LAST_CONTACT)) return 0L;
      return Math.max(0L, now - servant.getPersistentData().getLong(LAST_CONTACT));
   }

   public static int stageForDisconnectedTicks(long disconnectedTicks) {
      long ticks = Math.max(0L, disconnectedTicks);
      return ticks >= MAX_DISCONNECTED_TICKS ? 4
         : ticks >= BREAKOUT_ESCALATION_TICKS ? 3
         : ticks >= MOBILITY_ESCALATION_TICKS ? 2
         : ticks >= PRESSURE_START_TICKS ? 1 : 0;
   }

   private static boolean forceApproach(ServantEntity servant, LivingEntity target, long now, int stage) {
      if (servant.isPerformingAction() || ServantCombatSystem.cannotAct(servant)) return false;
      servant.getLookControl().setLookAt(target, 55.0F, 45.0F);
      servant.setSprinting(true);
      if (ServantFlightCombatService.forceMeleeApproach(servant, target, now)) return true;
      boolean moved = ServantNavigationHelper.moveToTargetThrottled(servant, target,
         stage >= 3 ? 1.9 : stage >= 2 ? 1.65 : 1.4, now, 3, 0.25, "CombatTempoApproach");
      if (ServantCombatDisposition.isRelentlessAdvance(servant)
         && (servant.horizontalCollision || !moved) && now - servant.getPersistentData().getLong(LAST_BREAK) >= 8L
         && servant.level() instanceof ServerLevel level) {
         Vec3 direction = target.position().subtract(servant.position()).multiply(1.0, 0.0, 1.0);
         if (direction.lengthSqr() > 1.0E-4) {
            direction = direction.normalize();
            servant.getPersistentData().putLong(LAST_BREAK, now);
            TerrainImpactService.impact(level, servant,
               servant.position().add(direction.scale(1.8)).add(0.0, 1.0, 0.0),
               TerrainImpactProfile.of(TerrainImpactProfile.Tier.MEDIUM),
               TerrainImpactService.Shape.AIR_SPHERE);
         }
      }
      if (!moved && stage >= 2 && servant.onGround()) {
         Vec3 toward = target.position().subtract(servant.position()).multiply(1.0, 0.0, 1.0);
         if (toward.lengthSqr() > 1.0E-4) {
            toward = toward.normalize();
            Vec3 motion = servant.getDeltaMovement();
            double boost = stage >= 3 ? 0.75 : 0.55;
            Vec3 offset = toward.scale(1.25);
            if (servant.level().noCollision(servant, servant.getBoundingBox().move(offset.x, 0.15, offset.z))) {
               servant.setDeltaMovement(motion.x + toward.x * boost, Math.max(motion.y, 0.18), motion.z + toward.z * boost);
               servant.hasImpulse = true;
               ServantNavigationHelper.limitMeleeApproachMotion(servant, target);
               return true;
            }
         }
      }
      ServantNavigationHelper.limitMeleeApproachMotion(servant, target);
      return moved;
   }

   private static int pressureWindow(ServantEntity servant, LivingEntity target) {
      return servant.distanceTo(target) <= 3.0 ? MELEE_PRESSURE_MAX : MELEE_PRESSURE_MIN;
   }

   public enum ContactType { RANGE, DAMAGE, BLOCKED, LAUNCH, WALL, LANDING }

   private static void initializeOrbitMetrics(CompoundTag data, ServantEntity servant, LivingEntity target) {
      if (servant == null || target == null) return;
      Vec3 relative = servant.position().subtract(target.position());
      data.putDouble(LAST_DISTANCE, relative.length());
      data.putDouble(LAST_ANGLE, Math.atan2(relative.z, relative.x));
      data.putDouble(ANGLE_TRAVEL, 0.0);
      data.putInt(ORBIT_STALL, 0);
      data.putInt(ORBIT_REVERSALS, 0);
      data.putInt(ORBIT_DIRECTION, 0);
      data.putBoolean(ORBITING, false);
   }

   private static void updateOrbitMetrics(ServantEntity servant, LivingEntity target, CompoundTag data,
                                          long now, double distance) {
      if (!data.contains(LAST_DISTANCE) || !data.contains(LAST_ANGLE)) {
         initializeOrbitMetrics(data, servant, target);
         return;
      }
      Vec3 relative = servant.position().subtract(target.position());
      double angle = Math.atan2(relative.z, relative.x);
      double delta = angle - data.getDouble(LAST_ANGLE);
      while (delta > Math.PI) delta -= Math.PI * 2.0;
      while (delta < -Math.PI) delta += Math.PI * 2.0;
      double previousDistance = data.getDouble(LAST_DISTANCE);
      int stall = data.getInt(ORBIT_STALL);
      if (distance < previousDistance - 0.12) stall = 0;
      else if (distance > 4.35) stall++;
      else stall = Math.max(0, stall - 1);
      if (Math.abs(delta) > 0.035) {
         int direction = delta > 0.0 ? 1 : -1;
         int previousDirection = data.getInt(ORBIT_DIRECTION);
         if (previousDirection != 0 && previousDirection != direction) {
            data.putInt(ORBIT_REVERSALS, data.getInt(ORBIT_REVERSALS) + 1);
         }
         data.putInt(ORBIT_DIRECTION, direction);
         data.putDouble(ANGLE_TRAVEL, data.getDouble(ANGLE_TRAVEL) + Math.abs(delta));
      }
      data.putDouble(LAST_DISTANCE, distance);
      data.putDouble(LAST_ANGLE, angle);
      data.putInt(ORBIT_STALL, stall);
      boolean orbiting = distance > 4.35 && stall >= ORBIT_DETECTION_TICKS
         && (data.getDouble(ANGLE_TRAVEL) >= 1.8 || data.getInt(ORBIT_REVERSALS) >= 3);
      data.putBoolean(ORBITING, orbiting);
   }

   public record TempoState(java.util.UUID target, long lastContact, long lastProgress,
                            int stage, boolean meleePressure, long disconnectedTicks,
                            boolean orbiting, boolean meleeOverride) {
      /** Source-compatible constructor for integrations built against phase one. */
      public TempoState(java.util.UUID target, long lastContact, long lastProgress,
                        int stage, boolean meleePressure, long disconnectedTicks) {
         this(target, lastContact, lastProgress, stage, meleePressure, disconnectedTicks, false, false);
      }

      private static final TempoState EMPTY = new TempoState(null, 0L, 0L, 0, false, 0L, false, false);
   }
}
