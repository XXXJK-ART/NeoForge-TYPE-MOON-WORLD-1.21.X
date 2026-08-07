package net.xxxjk.TYPE_MOON_WORLD.servant.combat;

import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantCombatTempoService;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantMasterProtection;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantParams;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatSpectacleService;
import net.xxxjk.TYPE_MOON_WORLD.world.terrain.TerrainImpactProfile;
import net.xxxjk.TYPE_MOON_WORLD.world.terrain.TerrainImpactService;

/** Server-authoritative launch, collision, landing, recovery and pursuit-window state. */
@EventBusSubscriber(modid = TYPE_MOON_WORLD.MOD_ID)
public final class ServantCombatMotionService {
   private static final String PREFIX = "TypeMoonMotion";
   private static final String ACTIVE_UNTIL = PREFIX + "ActiveUntil";
   private static final String PURSUIT_UNTIL = PREFIX + "PursuitUntil";
   private static final String ATTACKER = PREFIX + "Attacker";
   private static final String IMPACT_TIER = PREFIX + "ImpactTier";
   private static final String CONTROL_DEPTH = PREFIX + "ControlDepth";
   private static final String LAST_CONTROL = PREFIX + "LastControl";
   private static final String PREVIOUS_X = PREFIX + "PreviousX";
   private static final String PREVIOUS_Y = PREFIX + "PreviousY";
   private static final String PREVIOUS_Z = PREFIX + "PreviousZ";
   private static final String RECOVERY_COOLDOWN = PREFIX + "RecoveryCooldown";
   private static final String RECOVERY_UNTIL = PREFIX + "RecoveryUntil";
   private static final String COLLISION_COOLDOWN = PREFIX + "CollisionCooldown";
   private static final String STATE = PREFIX + "State";
   private static final String STAGGER_UNTIL = PREFIX + "StaggerUntil";
   private static final String PREVIOUS_POS_X = PREFIX + "PreviousPosX";
   private static final String PREVIOUS_POS_Y = PREFIX + "PreviousPosY";
   private static final String PREVIOUS_POS_Z = PREFIX + "PreviousPosZ";
   private static final String LAUNCH_TICK = PREFIX + "LaunchTick";
   private static final String LAUNCH_ENTITY_TICK = PREFIX + "LaunchEntityTick";
   private static final String TRAVEL_DISTANCE = PREFIX + "TravelDistance";
   private static final String PENDING_IMPACT_UNTIL = PREFIX + "PendingImpactUntil";
   private static final String PENDING_WALL = PREFIX + "PendingWall";
   private static final String PENDING_ENERGY = PREFIX + "PendingEnergy";
   private static final String CHAIN_IMPACTS_LEFT = PREFIX + "ChainImpactsLeft";
   private static final String NPC_RECOVERY_ROLLED = PREFIX + "NpcRecoveryRolled";
   private static final String TERRAIN_PERMISSION = PREFIX + "TerrainPermission";
   private static final int CONTROL_RESET_TICKS = 60;
   private static final int RECOVERY_COOLDOWN_TICKS = 40;
   private static final int MAX_ACTIVE_TICKS = 50;

   private ServantCombatMotionService() { }

   public static LaunchResult launch(LivingEntity attacker, LivingEntity target, Vec3 horizontal,
                                     double horizontalPower, double verticalPower,
                                     TerrainImpactProfile.Tier impactTier, int pursuitTicks) {
      if (attacker == null || target == null || !target.isAlive() || attacker.level() != target.level()) {
         return LaunchResult.REJECTED;
      }
      Vec3 direction = horizontal.multiply(1.0, 0.0, 1.0);
      if (direction.lengthSqr() < 1.0E-4) {
         direction = target.position().subtract(attacker.position()).multiply(1.0, 0.0, 1.0);
      }
      if (direction.lengthSqr() < 1.0E-4) direction = new Vec3(0.0, 0.0, 1.0);
      direction = direction.normalize();

      long now = target.level().getGameTime();
      CompoundTag data = target.getPersistentData();
      int depth = now - data.getLong(LAST_CONTROL) <= CONTROL_RESET_TICKS
         ? Math.min(4, data.getInt(CONTROL_DEPTH) + 1) : 1;
      double controlScale = controlScale(depth);
      boolean protectedRecovery = now < data.getLong(RECOVERY_UNTIL);
      if (protectedRecovery) controlScale *= 0.35;
      double impactScale = ServantCombatSpectacleService.impactScale(attacker instanceof net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity servant ? servant : null, target);
      double launchScale = Math.max(0.45, Math.min(2.15, controlScale * impactScale));

      Vec3 motion = target.getDeltaMovement();
      double appliedHorizontal = Math.max(0.0, horizontalPower) * launchScale;
      double appliedVertical = Math.max(0.0, verticalPower) * (0.72 + launchScale * 0.18);
      target.setDeltaMovement(
         direction.x * appliedHorizontal,
         Math.max(motion.y, appliedVertical),
         direction.z * appliedHorizontal
      );
      target.hasImpulse = true;
      target.hurtMarked = true;

      data.putLong(ACTIVE_UNTIL, now + Math.min(MAX_ACTIVE_TICKS,
         Math.max(12, (int)Math.ceil((appliedHorizontal + appliedVertical) * 10.0))));
      data.putLong(PURSUIT_UNTIL, now + Math.max(0, pursuitTicks));
      data.putUUID(ATTACKER, attacker.getUUID());
      TerrainImpactProfile.Tier resolvedImpactTier = cappedImpactTier(attacker, impactTier);
      data.putString(IMPACT_TIER, resolvedImpactTier.name());
      data.putInt(CONTROL_DEPTH, depth);
      data.putLong(LAST_CONTROL, now);
      data.putString(STATE, MotionState.AIRBORNE.name());
      data.putString(TERRAIN_PERMISSION, attacker instanceof Player ? "PLAYER" : "NPC");
      if (attacker instanceof ServantEntity servant
         && (resolvedImpactTier.ordinal() >= TerrainImpactProfile.Tier.HEAVY.ordinal()
            || ServantCombatSpectacleService.isForcedBreaker(servant))) {
         data.putInt(CHAIN_IMPACTS_LEFT, ServantCombatSpectacleService.chainImpactLimit(servant));
      } else {
         data.putInt(CHAIN_IMPACTS_LEFT, 0);
      }
      data.putLong(LAUNCH_TICK, now);
      data.putInt(LAUNCH_ENTITY_TICK, target.tickCount);
      data.putDouble(TRAVEL_DISTANCE, 0.0);
      data.remove(PENDING_IMPACT_UNTIL);
      data.remove(NPC_RECOVERY_ROLLED);
      storePreviousMotion(data, target.getDeltaMovement());
      storePreviousPosition(data, target.position());
      if (attacker instanceof ServantEntity servant) {
         ServantCombatTempoService.recordContact(servant, target,
            ServantCombatTempoService.ContactType.LAUNCH, now);
      }
      return new LaunchResult(true, depth, controlScale, appliedHorizontal, appliedVertical);
   }

   public static boolean isLaunched(LivingEntity target) {
      if (target == null) return false;
      CompoundTag data = target.getPersistentData();
      return data.contains(ACTIVE_UNTIL) && target.level().getGameTime() <= data.getLong(ACTIVE_UNTIL);
   }

   public static boolean canPursue(LivingEntity attacker, LivingEntity target) {
      if (attacker == null || target == null || !target.isAlive() || attacker.level() != target.level()) return false;
      CompoundTag data = target.getPersistentData();
      return data.contains(PURSUIT_UNTIL) && target.level().getGameTime() <= data.getLong(PURSUIT_UNTIL)
         && data.hasUUID(ATTACKER) && attacker.getUUID().equals(data.getUUID(ATTACKER));
   }

   public static boolean isRecovering(LivingEntity target) {
      return target != null && target.level().getGameTime() < target.getPersistentData().getLong(RECOVERY_UNTIL);
   }

   public static boolean isImpactStaggered(LivingEntity target) {
      return target != null && target.level().getGameTime() <= target.getPersistentData().getLong(STAGGER_UNTIL)
         && (state(target) == MotionState.WALL_STAGGER || state(target) == MotionState.GROUND_STAGGER);
   }

   public static MotionState state(LivingEntity target) {
      if (target == null) return MotionState.NONE;
      try {
         MotionState state = MotionState.valueOf(target.getPersistentData().getString(STATE));
         long now = target.level().getGameTime();
         if (state == MotionState.AIRBORNE && !isLaunched(target)) return MotionState.NONE;
         if ((state == MotionState.WALL_STAGGER || state == MotionState.GROUND_STAGGER)
            && now > target.getPersistentData().getLong(STAGGER_UNTIL)) return MotionState.NONE;
         if (state == MotionState.TECH_PROTECTED
            && now >= target.getPersistentData().getLong(RECOVERY_UNTIL)) return MotionState.NONE;
         return state;
      } catch (IllegalArgumentException ignored) {
         return MotionState.NONE;
      }
   }

   static double controlScale(int depth) {
      return switch (Math.max(1, depth)) {
         case 1 -> 1.0;
         case 2 -> 0.72;
         case 3 -> 0.48;
         default -> 0.28;
      };
   }

   @SubscribeEvent
   public static void tick(EntityTickEvent.Post event) {
      if (!(event.getEntity() instanceof LivingEntity target)
         || !(target.level() instanceof ServerLevel level)) return;
      CompoundTag data = target.getPersistentData();
      if (state(target) == MotionState.NONE && data.contains(STATE)) {
         data.putString(STATE, MotionState.NONE.name());
      }
      if (!isLaunched(target) && !data.contains(PENDING_IMPACT_UNTIL)) return;
      long now = level.getGameTime();
      if (data.contains(PENDING_IMPACT_UNTIL)) {
         if (tryRecovery(target, data, now)) {
            Vec3 softened = target.getDeltaMovement().scale(0.38);
            target.setDeltaMovement(softened.x, Math.max(0.08, softened.y), softened.z);
            finishRecovery(target, now);
            spawnRecoveryFx(level, target);
         } else if (now >= data.getLong(PENDING_IMPACT_UNTIL)) {
            resolveImpact(level, target, previousMotion(data), data.getBoolean(PENDING_WALL),
               data.getDouble(PENDING_ENERGY));
         }
         return;
      }
      Vec3 previous = previousMotion(data);
      Vec3 current = target.getDeltaMovement();
      double lostHorizontal = Math.max(0.0, previous.horizontalDistance() - current.horizontalDistance());
      Vec3 previousPosition = previousPosition(data);
      double moved = target.position().distanceTo(previousPosition);
      data.putDouble(TRAVEL_DISTANCE, data.getDouble(TRAVEL_DISTANCE) + moved);
      boolean completedMovementTick = now >= data.getLong(LAUNCH_TICK)
         && target.tickCount > data.getInt(LAUNCH_ENTITY_TICK);
      boolean likelyWallStop = target.horizontalCollision
         || previous.horizontalDistance() >= 0.45 && lostHorizontal >= 0.18;
      BlockHitResult wallHit = completedMovementTick && likelyWallStop
         ? sweepWall(level, target, previousPosition, previous) : null;
      boolean wallImpact = wallHit != null && lostHorizontal >= 0.18;
      boolean groundImpact = target.onGround() && previous.y <= -0.42;
      double impactEnergy = wallImpact ? Math.max(lostHorizontal, previous.horizontalDistance() * 0.55)
         : Math.max(0.0, Math.abs(previous.y) - Math.abs(current.y));

      if ((wallImpact || groundImpact) && now >= data.getLong(COLLISION_COOLDOWN)) {
      data.putLong(COLLISION_COOLDOWN, now + 4L);
         if (wallImpact) {
            resolveImpact(level, target, previous, true, Math.max(0.1, impactEnergy), wallHit.getLocation());
            return;
         }
         data.putLong(PENDING_IMPACT_UNTIL, now + 3L);
         data.putBoolean(PENDING_WALL, false);
         data.putDouble(PENDING_ENERGY, Math.max(0.1, impactEnergy));
         if (!(target instanceof ServerPlayer) && tryRecovery(target, data, now)) {
            finishRecovery(target, now);
            spawnRecoveryFx(level, target);
         }
         return;
      }

      if (now >= data.getLong(ACTIVE_UNTIL)
         || target.onGround() && Math.abs(previous.y) < 0.2 && target.getDeltaMovement().horizontalDistanceSqr() < 0.05) {
         data.remove(ACTIVE_UNTIL);
         data.putString(STATE, MotionState.NONE.name());
      } else {
         storePreviousMotion(data, target.getDeltaMovement());
         storePreviousPosition(data, target.position());
      }
   }

   private static boolean tryRecovery(LivingEntity target, CompoundTag data, long now) {
      if (now < data.getLong(RECOVERY_COOLDOWN)) return false;
      boolean requested = target instanceof ServerPlayer player && player.isShiftKeyDown();
      if (!requested && target instanceof net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity servant) {
         if (data.getBoolean(NPC_RECOVERY_ROLLED)) return false;
         data.putBoolean(NPC_RECOVERY_ROLLED, true);
         ServantParams params = servant.getDefinition() == null ? null : servant.getDefinition().parameters();
         int agility = ServantCombatFormulas.agilityStep(params);
         double tendency = net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantTacticalProfileResolver.resolve(servant).recoveryTendency();
         double staminaRatio = ServantCombatSystem.currentStamina(servant)
            / Math.max(1.0, ServantCombatFormulas.staminaMax(params));
         requested = agility >= 2 && servant.getRandom().nextDouble()
            < tendency * (0.25 + agility * 0.1) * (0.5 + Math.min(1.0, staminaRatio) * 0.5);
         if (requested) requested = ServantCombatSystem.tryConsumeStamina(servant, 8.0);
      }
      if (!requested) return false;
      data.putLong(RECOVERY_COOLDOWN, now + RECOVERY_COOLDOWN_TICKS);
      data.putLong(RECOVERY_UNTIL, now + 12L);
      return true;
   }

   private static void resolveImpact(ServerLevel level, LivingEntity target, Vec3 previous, boolean wallImpact,
                                     double impactEnergy) {
      resolveImpact(level, target, previous, wallImpact, impactEnergy, null);
   }

   private static void resolveImpact(ServerLevel level, LivingEntity target, Vec3 previous, boolean wallImpact,
                                     double impactEnergy, @Nullable Vec3 collisionPoint) {
      CompoundTag data = target.getPersistentData();
      TerrainImpactProfile.Tier tier = impactTier(data);
      double energy = Math.max(0.1, impactEnergy);
      boolean heavy = tier.ordinal() >= TerrainImpactProfile.Tier.HEAVY.ordinal();
      LivingEntity source = resolveAttacker(level, data);
      ServantEntity sourceServant = source instanceof ServantEntity servant ? servant : null;
      TerrainImpactProfile profile = wallImpact
         ? ServantCombatSpectacleService.wallTunnelProfile(sourceServant, target, heavy)
         : ServantCombatSpectacleService.routineGroundProfile(sourceServant, target, energy, heavy);
      Vec3 direction = previous.lengthSqr() < 1.0E-4 ? target.getLookAngle() : previous.normalize();
      Vec3 center = wallImpact && collisionPoint != null ? collisionPoint
         : target.position().add(direction.scale(wallImpact ? 0.55 : 0.0))
            .add(0.0, wallImpact ? target.getBbHeight() * 0.4 : 0.15, 0.0);
      if (wallImpact) {
         int chain = Math.max(0, data.getInt(CHAIN_IMPACTS_LEFT));
         if (chain > 0) {
            data.putInt(CHAIN_IMPACTS_LEFT, chain - 1);
         }
         TerrainImpactService.impactWallTunnel(level, source, center, direction, profile, terrainPermission(data),
            ServantCombatSpectacleService.wallTunnelLength(sourceServant, target, previous.horizontalDistance()),
            ServantCombatSpectacleService.wallTunnelWidth(sourceServant, target, previous.horizontalDistance()),
            ServantCombatSpectacleService.wallTunnelHeight(sourceServant, target, Math.abs(previous.y)));
      } else {
         TerrainImpactService.impact(level, source, center, profile,
            terrainPermission(data), TerrainImpactService.Shape.UPPER_SURFACE_CRATER);
      }

      float damage = (float)Math.min(heavy ? 14.0 : 8.0, Math.max(1.0, energy * (wallImpact ? 3.2 : 2.4)));
      if (!ServantMasterProtection.isProtectedMaster(source, target)) {
         target.hurt(impactDamageSource(target, source), damage);
      }
      target.setDeltaMovement(previous.x * (wallImpact ? 0.38 : 0.22), wallImpact ? Math.max(0.08, previous.y * 0.12) : 0.08,
         previous.z * (wallImpact ? 0.38 : 0.22));
      target.hurtMarked = true;
      boolean chainContinues = wallImpact && data.getInt(CHAIN_IMPACTS_LEFT) > 0 && previous.horizontalDistance() >= 0.55;
      if (chainContinues) {
         data.putString(STATE, MotionState.AIRBORNE.name());
         data.putLong(ACTIVE_UNTIL, level.getGameTime() + Math.min(MAX_ACTIVE_TICKS, 8L + data.getInt(CHAIN_IMPACTS_LEFT) * 4L));
         data.putLong(PURSUIT_UNTIL, Math.max(data.getLong(PURSUIT_UNTIL), level.getGameTime() + 12L));
         storePreviousMotion(data, target.getDeltaMovement());
         storePreviousPosition(data, target.position());
      } else {
         beginImpactStagger(target, level.getGameTime(), wallImpact ? 14 : 10, wallImpact);
      }
      if (source instanceof ServantEntity servant) {
         ServantCombatTempoService.recordContact(servant, target, wallImpact
            ? ServantCombatTempoService.ContactType.WALL
            : ServantCombatTempoService.ContactType.LANDING, level.getGameTime());
      }
      level.sendParticles(wallImpact ? ParticleTypes.POOF : ParticleTypes.CLOUD,
         center.x, center.y, center.z, heavy ? 24 : 14, profile.radius() * 0.35, 0.25, profile.radius() * 0.35, 0.08);
      level.playSound(null, target.blockPosition(), wallImpact ? SoundEvents.ZOMBIE_ATTACK_IRON_DOOR : SoundEvents.GENERIC_EXPLODE.value(),
         target instanceof Player ? SoundSource.PLAYERS : SoundSource.HOSTILE, heavy ? 1.1F : 0.75F, heavy ? 0.65F : 0.85F);
   }

   private static void beginImpactStagger(LivingEntity target, long now, int recoveryTicks, boolean wall) {
      CompoundTag data = target.getPersistentData();
      data.remove(ACTIVE_UNTIL);
      data.remove(PENDING_IMPACT_UNTIL);
      data.putString(STATE, (wall ? MotionState.WALL_STAGGER : MotionState.GROUND_STAGGER).name());
      data.putLong(STAGGER_UNTIL, now + recoveryTicks);
      data.putLong(PURSUIT_UNTIL, Math.max(data.getLong(PURSUIT_UNTIL), now + recoveryTicks));
      data.putLong(RECOVERY_UNTIL, Math.max(data.getLong(RECOVERY_UNTIL), now + recoveryTicks));
   }

   public static double impactRadius(TerrainImpactProfile.Tier tier, double impactEnergy, boolean wallImpact) {
      TerrainImpactProfile.Tier resolved = tier == null ? TerrainImpactProfile.Tier.SMALL : tier;
      boolean heavy = resolved.ordinal() >= TerrainImpactProfile.Tier.HEAVY.ordinal();
      double energy = Math.max(0.1, impactEnergy);
      if (wallImpact) {
         return Math.min(heavy ? 7.0 : 4.5, Math.max(2.25, energy * 1.6));
      }
      return Math.min(heavy ? 4.5 : 3.2, Math.max(1.5, energy * 1.6));
   }

   private static void finishRecovery(LivingEntity target, long now) {
      CompoundTag data = target.getPersistentData();
      data.remove(ACTIVE_UNTIL);
      data.remove(PENDING_IMPACT_UNTIL);
      data.remove(PURSUIT_UNTIL);
      data.putString(STATE, MotionState.TECH_PROTECTED.name());
      data.putLong(RECOVERY_UNTIL, Math.max(data.getLong(RECOVERY_UNTIL), now + 12L));
   }

   private static void spawnRecoveryFx(ServerLevel level, LivingEntity target) {
      level.sendParticles(ParticleTypes.CLOUD, target.getX(), target.getY() + 0.35, target.getZ(),
         10, 0.35, 0.18, 0.35, 0.04);
      level.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
         target instanceof Player ? SoundSource.PLAYERS : SoundSource.HOSTILE, 0.55F, 1.4F);
   }

   private static TerrainImpactProfile.Tier impactTier(CompoundTag data) {
      try {
         return TerrainImpactProfile.Tier.valueOf(data.getString(IMPACT_TIER));
      } catch (IllegalArgumentException ignored) {
         return TerrainImpactProfile.Tier.SMALL;
      }
   }

   private static TerrainImpactProfile.Tier cappedImpactTier(LivingEntity attacker, @Nullable TerrainImpactProfile.Tier requested) {
      TerrainImpactProfile.Tier value = requested == null ? TerrainImpactProfile.Tier.SMALL : requested;
      if (!(attacker instanceof net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity servant)
         || servant.getDefinition() == null) return value;
      var tactical = net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantTacticalProfileResolver.resolve(servant);
      TerrainImpactProfile.Tier maximum;
      try {
         maximum = TerrainImpactProfile.Tier.valueOf(tactical.maximumTerrainImpact().toUpperCase());
      } catch (IllegalArgumentException ignored) {
         maximum = TerrainImpactProfile.Tier.SMALL;
      }
      return value.ordinal() <= maximum.ordinal() ? value : maximum;
   }

   @Nullable
   private static LivingEntity resolveAttacker(ServerLevel level, CompoundTag data) {
      if (!data.hasUUID(ATTACKER)) return null;
      UUID id = data.getUUID(ATTACKER);
      Entity entity = level.getEntity(id);
      return entity instanceof LivingEntity living ? living : null;
   }

   private static DamageSource impactDamageSource(LivingEntity target, @Nullable LivingEntity attacker) {
      if (attacker instanceof Player player) return target.damageSources().playerAttack(player);
      if (attacker != null) return target.damageSources().mobAttack(attacker);
      return target.damageSources().generic();
   }

   private static void storePreviousMotion(CompoundTag data, Vec3 motion) {
      data.putDouble(PREVIOUS_X, motion.x);
      data.putDouble(PREVIOUS_Y, motion.y);
      data.putDouble(PREVIOUS_Z, motion.z);
   }

   private static Vec3 previousMotion(CompoundTag data) {
      return new Vec3(data.getDouble(PREVIOUS_X), data.getDouble(PREVIOUS_Y), data.getDouble(PREVIOUS_Z));
   }

   private static void storePreviousPosition(CompoundTag data, Vec3 position) {
      data.putDouble(PREVIOUS_POS_X, position.x);
      data.putDouble(PREVIOUS_POS_Y, position.y);
      data.putDouble(PREVIOUS_POS_Z, position.z);
   }

   private static Vec3 previousPosition(CompoundTag data) {
      return new Vec3(data.getDouble(PREVIOUS_POS_X), data.getDouble(PREVIOUS_POS_Y), data.getDouble(PREVIOUS_POS_Z));
   }

   @Nullable
   private static BlockHitResult sweepWall(ServerLevel level, LivingEntity target, Vec3 start, Vec3 motion) {
      Vec3 horizontal = motion.multiply(1.0, 0.0, 1.0);
      if (horizontal.lengthSqr() < 1.0E-4) return null;
      // Vanilla stops the entity at its collision-box edge, so the centre-point
      // segment ends just short of the block face. Extend the sweep by the
      // leading half-width to sample the actual surface that stopped the body.
      Vec3 end = start.add(horizontal.x, motion.y, horizontal.z)
         .add(horizontal.normalize().scale(Math.max(0.3, target.getBbWidth() * 0.75 + 0.2)));
      Vec3 side = new Vec3(-horizontal.z, 0.0, horizontal.x).normalize()
         .scale(Math.max(0.1, target.getBbWidth() * 0.42));
      double[] heights = {0.2, 0.5, 0.82};
      Vec3[] offsets = {Vec3.ZERO, side, side.scale(-1.0)};
      BlockHitResult closest = null;
      double closestDistance = Double.MAX_VALUE;
      for (double height : heights) {
         for (Vec3 offset : offsets) {
            Vec3 from = start.add(offset).add(0.0, target.getBbHeight() * height, 0.0);
            Vec3 to = end.add(offset).add(0.0, target.getBbHeight() * height, 0.0);
            HitResult result = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER,
               ClipContext.Fluid.NONE, target));
            if (result instanceof BlockHitResult blockHit
               && blockHit.getDirection().getAxis() != Direction.Axis.Y) {
               double distance = from.distanceToSqr(blockHit.getLocation());
               if (distance < closestDistance) {
                  closest = blockHit;
                  closestDistance = distance;
               }
            }
         }
      }
      return closest;
   }

   private static TerrainImpactService.Permission terrainPermission(CompoundTag data) {
      return "PLAYER".equals(data.getString(TERRAIN_PERMISSION))
         ? TerrainImpactService.Permission.PLAYER : TerrainImpactService.Permission.NPC;
   }

   public record LaunchResult(boolean applied, int controlDepth, double controlScale,
                              double horizontalPower, double verticalPower) {
      public static final LaunchResult REJECTED = new LaunchResult(false, 0, 0.0, 0.0, 0.0);
   }

   public enum MotionState { NONE, AIRBORNE, WALL_STAGGER, GROUND_STAGGER, TECH_PROTECTED }
}
