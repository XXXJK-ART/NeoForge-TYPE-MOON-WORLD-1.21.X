package net.xxxjk.TYPE_MOON_WORLD.servant.ai;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;

public final class ServantNavigationHelper {
   public static final int DEFAULT_REPATH_INTERVAL = 8;
   public static final int SHORT_REPATH_INTERVAL = 5;

   private ServantNavigationHelper() {
   }

   /** Detailed path outcome used by the tempo guard; the old boolean methods remain compatible. */
   public enum NavigationResult {
      MOVED, NO_PATH, BLOCKED, NO_PROGRESS, UNSAFE;

      public boolean accepted() {
         return this == MOVED;
      }
   }

   public static NavigationResult moveToPositionDetailed(
      ServantEntity entity, Vec3 target, double speed, long gameTick,
      int repathInterval, double minTargetMoveSqr, String keyPrefix) {
      if (entity == null || target == null) return NavigationResult.NO_PATH;
      BlockPos targetBlock = BlockPos.containing(target);
      if (!entity.level().hasChunkAt(targetBlock)) return NavigationResult.UNSAFE;
      AABB destination = entity.getBoundingBox().move(
         target.x - entity.getX(), target.y - entity.getY(), target.z - entity.getZ());
      if (!entity.level().noCollision(entity, destination)) return NavigationResult.UNSAFE;
      boolean accepted = moveToPositionThrottled(entity, target, speed, gameTick,
         repathInterval, minTargetMoveSqr, keyPrefix);
      if (!accepted) return NavigationResult.NO_PATH;
      if (entity.distanceToSqr(target) <= 2.25) return NavigationResult.MOVED;
      CompoundTag data = entity.getPersistentData();
      String progress = keyPrefix + "Progress";
      String px = keyPrefix + "ProgressX";
      String py = keyPrefix + "ProgressY";
      String pz = keyPrefix + "ProgressZ";
      if (!data.contains(px)) {
         data.putDouble(px, entity.getX());
         data.putDouble(py, entity.getY());
         data.putDouble(pz, entity.getZ());
         data.putLong(progress, gameTick);
      } else {
         double dx = entity.getX() - data.getDouble(px);
         double dy = entity.getY() - data.getDouble(py);
         double dz = entity.getZ() - data.getDouble(pz);
         if (dx * dx + dy * dy + dz * dz >= 0.04) {
            data.putDouble(px, entity.getX());
            data.putDouble(py, entity.getY());
            data.putDouble(pz, entity.getZ());
            data.putLong(progress, gameTick);
         }
      }
      if (gameTick - data.getLong(progress) >= 12L) {
         return entity.getNavigation().isDone() ? NavigationResult.BLOCKED : NavigationResult.NO_PROGRESS;
      }
      return NavigationResult.MOVED;
   }

   public static boolean moveToTargetThrottled(
      ServantEntity entity,
      LivingEntity target,
      double speed,
      long gameTick,
      String keyPrefix
   ) {
      return moveToTargetThrottled(entity, target, speed, gameTick, DEFAULT_REPATH_INTERVAL, 0.8, keyPrefix);
   }

   public static boolean moveToTargetThrottled(
      ServantEntity entity,
      LivingEntity target,
      double speed,
      long gameTick,
      int repathInterval,
      double minTargetMoveSqr,
      String keyPrefix
   ) {
      // Vanilla pathfinders are deliberately conservative in water and can stop
      // making progress against a moving target. Keep navigation as a fallback,
      // but apply a bounded steering impulse while submerged.
      if (entity.isInWaterOrBubble() && target != null) {
         return steerInWater(entity, target.position(), speed, gameTick, keyPrefix);
      }
      entity.setSwimming(false);
      if (ServantEngagementService.matchup(entity, target) == ServantEngagementService.Matchup.MELEE_VS_RANGED
         && entity.distanceTo(target) > 7.0) {
         double agilitySpeed = entity.getAttributeValue(Attributes.MOVEMENT_SPEED);
         double chaseScale = 1.12 + Math.max(0.0, Math.min(0.32, (agilitySpeed - 0.20) * 0.9));
         tryMeleeClosingBurst(entity, target, gameTick);
         return moveToPositionThrottled(
            entity,
            ServantEngagementService.meleeApproachPoint(entity, target, gameTick),
            speed * chaseScale,
            gameTick,
            Math.min(repathInterval, SHORT_REPATH_INTERVAL),
            minTargetMoveSqr,
            keyPrefix + "Intercept"
         );
      }
      CompoundTag data = entity.getPersistentData();
      String xKey = keyPrefix + "TargetX";
      String yKey = keyPrefix + "TargetY";
      String zKey = keyPrefix + "TargetZ";
      String speedKey = keyPrefix + "Speed";
      boolean hasPathMemory = data.contains(xKey) && data.contains(yKey) && data.contains(zKey);
      double dx = hasPathMemory ? target.getX() - data.getDouble(xKey) : Double.MAX_VALUE;
      double dy = hasPathMemory ? target.getY() - data.getDouble(yKey) : Double.MAX_VALUE;
      double dz = hasPathMemory ? target.getZ() - data.getDouble(zKey) : Double.MAX_VALUE;
      boolean targetMoved = dx * dx + dy * dy + dz * dz >= minTargetMoveSqr;
      boolean speedChanged = !data.contains(speedKey) || Math.abs(data.getDouble(speedKey) - speed) > 0.05;
      if (!targetMoved && !speedChanged && !entity.getNavigation().isDone() && gameTick - data.getLong(keyPrefix + "LastPathTick") < repathInterval) {
         return true;
      }

      data.putLong(keyPrefix + "LastPathTick", gameTick);
      data.putDouble(xKey, target.getX());
      data.putDouble(yKey, target.getY());
      data.putDouble(zKey, target.getZ());
      data.putDouble(speedKey, speed);
      return entity.getNavigation().moveTo(target, speed);
   }

   private static boolean steerInWater(ServantEntity entity, Vec3 target, double requestedSpeed, long gameTick, String keyPrefix) {
      entity.setSwimming(true);
      Vec3 toTarget = target.subtract(entity.position());
      double distance = toTarget.length();
      if (distance < 0.75) {
         entity.getNavigation().stop();
         entity.setDeltaMovement(entity.getDeltaMovement().scale(0.72));
         return true;
      }
      Vec3 direction = toTarget.scale(1.0 / distance);
      double maxSpeed = Math.max(0.24, Math.min(0.58, 0.20 + requestedSpeed * 0.22));
      CompoundTag data = entity.getPersistentData();
      Vec3 motion = entity.getDeltaMovement();
      double along = motion.dot(direction);
      double acceleration = Math.max(0.0, maxSpeed - along) * 0.28;
      Vec3 next = motion.scale(0.88).add(direction.scale(acceleration));
      if (next.length() > maxSpeed) {
         next = next.normalize().scale(maxSpeed);
      }
      entity.setDeltaMovement(next);
      entity.hasImpulse = true;
      // Keep a low-frequency path active for collision avoidance and unloaded chunks.
      if (gameTick - data.getLong(keyPrefix + "WaterPathTick") >= 6L) {
         data.putLong(keyPrefix + "WaterPathTick", gameTick);
         entity.getNavigation().moveTo(target.x, target.y, target.z, Math.max(0.8, requestedSpeed));
      }
      return true;
   }

   /** Gives every melee servant a speed-scaled intercept burst against a retreating ranged target. */
   public static void tryMeleeClosingBurst(ServantEntity entity, LivingEntity target, long gameTick) {
      if (ServantEngagementService.matchup(entity, target) != ServantEngagementService.Matchup.MELEE_VS_RANGED
         || entity.isPerformingAction() || !entity.onGround() || entity.distanceTo(target) <= 7.0
         || entity.distanceTo(target) > 30.0 || target.getY() - entity.getY() > 4.0) return;
      double agilitySpeed = entity.getAttributeValue(Attributes.MOVEMENT_SPEED);
      CompoundTag data = entity.getPersistentData();
      int cooldown = Math.max(6, 16 - (int)Math.round(agilitySpeed * 20.0));
      if (gameTick - data.getLong("ServantMeleeClosingBurstTick") < cooldown) return;
      Vec3 direction = target.position().add(target.getDeltaMovement().scale(4.0))
         .subtract(entity.position()).multiply(1.0, 0.0, 1.0);
      if (direction.lengthSqr() < 1.0E-4) return;
      direction = direction.normalize();
      Vec3 lane = direction.scale(1.6);
      if (!entity.level().noCollision(entity, entity.getBoundingBox().move(lane.x, 0.18, lane.z))) return;
      Vec3 motion = entity.getDeltaMovement();
      double desired = Math.max(0.42, Math.min(0.78, 0.30 + agilitySpeed));
      double current = motion.x * direction.x + motion.z * direction.z;
      double boost = Math.max(0.0, desired - current);
      entity.setDeltaMovement(motion.x + direction.x * boost, Math.max(motion.y, 0.18),
         motion.z + direction.z * boost);
      entity.hasImpulse = true;
      data.putLong("ServantMeleeClosingBurstTick", gameTick);
   }

   public static boolean moveToPositionThrottled(
      ServantEntity entity,
      Vec3 target,
      double speed,
      long gameTick,
      int repathInterval,
      double minTargetMoveSqr,
      String keyPrefix
   ) {
      if (entity.isInWaterOrBubble()) {
         return steerInWater(entity, target, speed, gameTick, keyPrefix);
      }
      entity.setSwimming(false);
      CompoundTag data = entity.getPersistentData();
      String xKey = keyPrefix + "TargetX";
      String yKey = keyPrefix + "TargetY";
      String zKey = keyPrefix + "TargetZ";
      String speedKey = keyPrefix + "Speed";
      boolean hasPathMemory = data.contains(xKey) && data.contains(yKey) && data.contains(zKey);
      double dx = hasPathMemory ? target.x - data.getDouble(xKey) : Double.MAX_VALUE;
      double dy = hasPathMemory ? target.y - data.getDouble(yKey) : Double.MAX_VALUE;
      double dz = hasPathMemory ? target.z - data.getDouble(zKey) : Double.MAX_VALUE;
      boolean targetMoved = dx * dx + dy * dy + dz * dz >= minTargetMoveSqr;
      boolean speedChanged = !data.contains(speedKey) || Math.abs(data.getDouble(speedKey) - speed) > 0.05;
      if (!targetMoved && !speedChanged && !entity.getNavigation().isDone() && gameTick - data.getLong(keyPrefix + "LastPathTick") < repathInterval) {
         return true;
      }

      data.putLong(keyPrefix + "LastPathTick", gameTick);
      data.putDouble(xKey, target.x);
      data.putDouble(yKey, target.y);
      data.putDouble(zKey, target.z);
      data.putDouble(speedKey, speed);
      return entity.getNavigation().moveTo(target.x, target.y, target.z, speed);
   }

   public static boolean stopIfMoving(ServantEntity entity) {
      if (entity.getNavigation().isDone()) {
         return false;
      }
      entity.getNavigation().stop();
      return true;
   }
}
