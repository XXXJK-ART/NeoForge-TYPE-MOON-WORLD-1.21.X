package net.xxxjk.TYPE_MOON_WORLD.servant.ai;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;

public final class ServantNavigationHelper {
   public static final int DEFAULT_REPATH_INTERVAL = 8;
   public static final int SHORT_REPATH_INTERVAL = 5;

   private ServantNavigationHelper() {
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
      if (ServantEngagementService.matchup(entity, target) == ServantEngagementService.Matchup.MELEE_VS_RANGED
         && entity.distanceTo(target) > 7.0) {
         return moveToPositionThrottled(
            entity,
            ServantEngagementService.meleeApproachPoint(entity, target, gameTick),
            speed * 1.12,
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

   public static boolean moveToPositionThrottled(
      ServantEntity entity,
      Vec3 target,
      double speed,
      long gameTick,
      int repathInterval,
      double minTargetMoveSqr,
      String keyPrefix
   ) {
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
