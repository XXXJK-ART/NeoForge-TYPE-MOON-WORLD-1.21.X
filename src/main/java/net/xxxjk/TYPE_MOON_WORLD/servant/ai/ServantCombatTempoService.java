package net.xxxjk.TYPE_MOON_WORLD.servant.ai;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
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
   public static final int MELEE_PRESSURE_MIN = 60;
   public static final int MELEE_PRESSURE_MAX = 100;

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
   private static final int BASIC_COOLDOWN = 10;

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
      if (servant.distanceTo(target) <= 4.2) {
         data.putLong(LAST_CONTACT, now);
         int pressure = pressureWindow(servant, target);
         data.putLong(PRESSURE_UNTIL, Math.max(data.getLong(PRESSURE_UNTIL), now + pressure));
      }
      long lastContact = data.getLong(LAST_CONTACT);
      long disconnected = Math.max(0L, now - lastContact);
      int stage = stageForDisconnectedTicks(disconnected);
      return new TempoState(target.getUUID(), lastContact, data.getLong(LAST_PROGRESS),
         stage, data.getLong(PRESSURE_UNTIL) > now, disconnected);
   }

   public static boolean submitFallback(ServantEntity servant, LivingEntity target, AiBrain brain, long now,
                                         TempoState state) {
      if (servant == null || target == null || brain == null || state == null || !target.isAlive()) return false;
      if (ServantPlannedActionExecutor.isActive(servant)) return false;
      boolean closeEnough = servant.distanceTo(target) <= 4.2;
      // Before the first 40-tick deadline, yielding lets character-specific
      // helpers retain final execution authority.  The legacy tail guard still
      // supplies a basic attack when those helpers decline to act.
      boolean forced = state.stage() > 0;
      if (!forced) return false;
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
      if (servant.distanceTo(target) <= 4.2) {
         tryBasicAttack(servant, target, now);
      } else if (state.stage() > 0) {
         forceApproach(servant, target, now, state.stage());
      }
   }

   public static boolean tryBasicAttack(ServantEntity servant, LivingEntity target, long now) {
      if (servant == null || target == null || !target.isAlive() || servant.level() != target.level()
         || ServantCombatSystem.cannotAct(servant) || servant.isPerformingAction()
         || servant.distanceTo(target) > 4.35) return false;
      CompoundTag data = servant.getPersistentData();
      if (now - data.getLong(LAST_BASIC) < BASIC_COOLDOWN) return false;
      data.putLong(LAST_BASIC, now);
      servant.getNavigation().stop();
      servant.faceToward(target.position());
      boolean hit = servant.doHurtTarget(target);
      servant.triggerBasicAttackAnimation();
      recordContact(servant, target, hit ? ContactType.DAMAGE : ContactType.BLOCKED, now);
      return true;
   }

   public static void recordContact(ServantEntity servant, LivingEntity target, ContactType type, long now) {
      if (servant == null || target == null || !target.isAlive()) return;
      CompoundTag data = servant.getPersistentData();
      data.putUUID(TARGET, target.getUUID());
      data.putLong(LAST_CONTACT, now);
      data.putLong(LAST_FAILURE, type.ordinal());
      data.putLong(PRESSURE_UNTIL, Math.max(data.getLong(PRESSURE_UNTIL), now + pressureWindow(servant, target)));
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
   }

   public static boolean inMeleePressure(ServantEntity servant, long now) {
      return servant != null && servant.getPersistentData().getLong(PRESSURE_UNTIL) > now;
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

   public record TempoState(java.util.UUID target, long lastContact, long lastProgress,
                            int stage, boolean meleePressure, long disconnectedTicks) {
      private static final TempoState EMPTY = new TempoState(null, 0L, 0L, 0, false, 0L);
   }
}
