package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.BucephalusEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.GordiusWheelEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatSystem;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.world.dimension.ModDimensions;

public final class IskandarCombatHelper {
   private static final String TAG_LAST_PHASE = "IskandarLastHpPhase";
   private static final String TAG_IONIOI_WARMUP_TARGET = "IskandarIonioiWarmupTarget";
   private static final String TAG_IONIOI_WARMUP_START = "IskandarIonioiWarmupStart";
   private static final int HIGH_HP_PHASE = 1;
   private static final int MID_HP_PHASE = 2;
   private static final int LOW_HP_PHASE = 3;
   private static final int IONIOI_LOW_PHASE_WARMUP_TICKS = 8 * 20;

   private IskandarCombatHelper() {
   }

   public static void tick(IskandarEntity entity, ServerLevel level) {
      int phase = phase(entity);
      entity.getPersistentData().putInt(TAG_LAST_PHASE, phase);
      if (entity.isIonioiHetairoiActive()) {
         tickFallbackMount(entity, level, phase);
         entity.tryGordiusWheelCharge(level);
         return;
      }
      if (ServantCombatSystem.cannotAct(entity) || ServantCombatSystem.skillsSuppressed(entity)) {
         return;
      }

      LivingEntity target = entity.getTarget();
      if (target == null || !target.isAlive()) {
         dismountForWalking(entity);
         resetIonioiWarmup(entity);
         return;
      }

      if (shouldUseIonioi(entity, level)) {
         return;
      }

      if (shouldUseGordiusWheel(entity, level, target)) {
         keepRidingGordiusWheel(entity, level);
         entity.tryGordiusWheelCharge(level);
         return;
      }

      tickFallbackMount(entity, level, phase);
      entity.tryGordiusWheelCharge(level);
   }

   public static boolean shouldUseIonioi(IskandarEntity entity, ServerLevel level) {
      if (ModDimensions.isIonioiHetairoiDimension(level.dimension().location())) {
         return false;
      }
      if (!entity.isGordiusWheelDestroyed()) {
         resetIonioiWarmup(entity);
         return false;
      }
      boolean ridingBucephalus = entity.getVehicle() instanceof BucephalusEntity;
      LivingEntity target = entity.getTarget();
      if (target == null || !target.isAlive()) {
         target = recentIonioiAggressor(entity);
      }
      int phase = phase(entity);
      if (target == null || !target.isAlive()) {
         resetIonioiWarmup(entity);
         return false;
      }
      int enemies = countNearbyEnemies(entity, level, 24.0);
      float maxTargetHealth = target.getMaxHealth();
      boolean tacticalReason = switch (phase) {
         case LOW_HP_PHASE -> enemies >= 3 || maxTargetHealth >= 180.0F;
         case MID_HP_PHASE -> enemies >= 5 || maxTargetHealth >= 300.0F;
         default -> enemies >= 7 || maxTargetHealth >= 500.0F;
      };
      boolean fallbackLastStand = ridingBucephalus
         && phase == LOW_HP_PHASE
         && hasSummonedFallbackMount(entity)
         && entity.distanceToSqr(target) <= 48.0 * 48.0;
      if (!tacticalReason && !fallbackLastStand) {
         resetIonioiWarmup(entity);
         return false;
      }
      return ionioiWarmupReady(entity, level, target.getId());
   }

   private static boolean hasSummonedFallbackMount(IskandarEntity entity) {
      return entity.isGordiusWheelDestroyed() && entity.getVehicle() instanceof BucephalusEntity;
   }

   private static boolean ionioiWarmupReady(IskandarEntity entity, ServerLevel level, int targetId) {
      int previousTarget = entity.getPersistentData().getInt(TAG_IONIOI_WARMUP_TARGET);
      long now = level.getGameTime();
      if (previousTarget != targetId || !entity.getPersistentData().contains(TAG_IONIOI_WARMUP_START)) {
         entity.getPersistentData().putInt(TAG_IONIOI_WARMUP_TARGET, targetId);
         entity.getPersistentData().putLong(TAG_IONIOI_WARMUP_START, now);
         return false;
      }
      return now - entity.getPersistentData().getLong(TAG_IONIOI_WARMUP_START) >= IONIOI_LOW_PHASE_WARMUP_TICKS;
   }

   private static void resetIonioiWarmup(IskandarEntity entity) {
      entity.getPersistentData().remove(TAG_IONIOI_WARMUP_TARGET);
      entity.getPersistentData().remove(TAG_IONIOI_WARMUP_START);
   }

   private static LivingEntity recentIonioiAggressor(IskandarEntity entity) {
      LivingEntity attacker = entity.getLastHurtByMob();
      if (attacker == null || !attacker.isAlive() || entity.tickCount - entity.getLastHurtByMobTimestamp() > 200) {
         return null;
      }
      return attacker != entity
         && !EntityUtils.isSpectatorPlayer(attacker)
         && !EntityUtils.isUntargetableServantTransition(attacker)
         && !entity.isAlliedTo(attacker)
         ? attacker
         : null;
   }

   private static boolean shouldUseGordiusWheel(IskandarEntity entity, ServerLevel level, LivingEntity target) {
      if (!entity.canSummonGordiusWheel(level)) {
         return false;
      }
      int phase = phase(entity);
      int enemies = countNearbyEnemies(entity, level, 18.0);
      return entity.distanceToSqr(target) <= 32.0 * 32.0
         && (phase >= MID_HP_PHASE || enemies >= 3 || entity.distanceToSqr(target) > 8.0 * 8.0);
   }

   private static void tickFallbackMount(IskandarEntity entity, ServerLevel level, int phase) {
      if (entity.getVehicle() instanceof GordiusWheelEntity) {
         return;
      }
      if (!entity.isGordiusWheelDestroyed()) {
         dismountForWalking(entity);
         return;
      }
      keepRidingBucephalus(entity, level);
   }

   private static void dismountForWalking(IskandarEntity entity) {
      if (entity.getVehicle() instanceof BucephalusEntity || entity.getVehicle() instanceof GordiusWheelEntity) {
         entity.stopRiding();
      }
   }

   private static void keepRidingBucephalus(IskandarEntity entity, ServerLevel level) {
      if (entity.getVehicle() instanceof BucephalusEntity) {
         return;
      }
      entity.summonBucephalusAndRide(level);
   }

   private static void keepRidingGordiusWheel(IskandarEntity entity, ServerLevel level) {
      if (entity.getVehicle() instanceof GordiusWheelEntity) {
         return;
      }
      entity.summonGordiusWheelAndRide(level);
   }

   private static int phase(IskandarEntity entity) {
      float ratio = entity.getHealth() / Math.max(1.0F, entity.getMaxHealth());
      if (ratio <= 0.33F) {
         return LOW_HP_PHASE;
      }
      return ratio <= 0.66F ? MID_HP_PHASE : HIGH_HP_PHASE;
   }

   private static int countNearbyEnemies(IskandarEntity entity, ServerLevel level, double radius) {
      return level.getEntitiesOfClass(LivingEntity.class, entity.getBoundingBox().inflate(radius),
         candidate -> candidate != entity
            && candidate.isAlive()
            && !entity.isAlliedTo(candidate)
            && !EntityUtils.isImmunePlayerTarget(candidate)
            && candidate.position().subtract(entity.position()).multiply(1.0, 0.0, 1.0).lengthSqr() <= radius * radius)
         .size();
   }
}
