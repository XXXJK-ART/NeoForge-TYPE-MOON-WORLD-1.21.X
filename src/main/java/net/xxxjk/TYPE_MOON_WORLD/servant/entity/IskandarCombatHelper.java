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
   private static final int HIGH_HP_PHASE = 1;
   private static final int MID_HP_PHASE = 2;
   private static final int LOW_HP_PHASE = 3;

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
         if (entity.hasSummonedGordiusWheelOnce()) {
            tickFallbackMount(entity, level, phase);
            return;
         }
         dismountForWalking(entity);
         return;
      }

      if (shouldUseIonioi(entity, level)) {
         dismountForWalking(entity);
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
      LivingEntity target = entity.getTarget();
      if (target == null || !target.isAlive()) {
         return false;
      }
      int phase = phase(entity);
      int enemies = countNearbyEnemies(entity, level, 24.0);
      float maxTargetHealth = target.getMaxHealth();
      return switch (phase) {
         case LOW_HP_PHASE -> enemies >= 3 || maxTargetHealth >= 180.0F || entity.getCurrentMp() >= IskandarEntity.IONIOI_MP_COST;
         case MID_HP_PHASE -> enemies >= 5 || maxTargetHealth >= 300.0F;
         default -> enemies >= 7 || maxTargetHealth >= 500.0F;
      };
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
      if (!entity.hasSummonedGordiusWheelOnce()) {
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
