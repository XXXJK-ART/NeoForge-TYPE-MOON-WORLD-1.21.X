package net.xxxjk.TYPE_MOON_WORLD.servant.ai;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantCommandMode;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantMasterTargeting;

/** Keeps a recently engaged, still valid opponent from being discarded after knockback or brief occlusion. */
public final class ServantTargetingService {
   public static final double FOLLOW_DISTANCE = 96.0;
   public static final double RETAIN_DISTANCE = 128.0;
   public static final long MEMORY_TICKS = 240L;

   private static final String TARGET_UUID = "ServantCombatTargetMemory";
   private static final String TARGET_TICK = "ServantCombatTargetMemoryTick";
   private static final String TARGET_X = "ServantCombatTargetMemoryX";
   private static final String TARGET_Y = "ServantCombatTargetMemoryY";
   private static final String TARGET_Z = "ServantCombatTargetMemoryZ";

   private ServantTargetingService() { }

   public static LivingEntity refreshOrRecover(ServantEntity servant, long now) {
      var followRange = servant.getAttribute(Attributes.FOLLOW_RANGE);
      if (followRange != null && followRange.getBaseValue() < FOLLOW_DISTANCE) {
         followRange.setBaseValue(FOLLOW_DISTANCE);
      }
      LivingEntity current = servant.getTarget();
      if (current != null) {
         if (isRetainable(servant, current) && commandAllows(servant, current)) {
            remember(servant, current, now);
         } else if (!current.isAlive() || !EntityUtils.isValidCombatTarget(servant, current)) {
            forget(servant);
         }
         return current;
      }

      var data = servant.getPersistentData();
      if (!data.hasUUID(TARGET_UUID) || now - data.getLong(TARGET_TICK) > MEMORY_TICKS) {
         forget(servant);
         return null;
      }
      if (!(servant.level() instanceof ServerLevel level)) {
         return null;
      }

      Entity remembered = level.getEntity(data.getUUID(TARGET_UUID));
      if (!(remembered instanceof LivingEntity target)
         || !isRetainable(servant, target)
         || !commandAllows(servant, target)) {
         if (remembered != null) forget(servant);
         return null;
      }

      servant.setTarget(target);
      remember(servant, target, now);
      return target;
   }

   public static void remember(ServantEntity servant, LivingEntity target, long now) {
      if (!isRetainable(servant, target) || !commandAllows(servant, target)) return;
      var data = servant.getPersistentData();
      data.putUUID(TARGET_UUID, target.getUUID());
      data.putLong(TARGET_TICK, now);
      data.putDouble(TARGET_X, target.getX());
      data.putDouble(TARGET_Y, target.getY());
      data.putDouble(TARGET_Z, target.getZ());
   }

   public static boolean canRetain(ServantEntity servant, LivingEntity target, long now) {
      var data = servant.getPersistentData();
      return data.hasUUID(TARGET_UUID)
         && data.getUUID(TARGET_UUID).equals(target.getUUID())
         && now - data.getLong(TARGET_TICK) <= MEMORY_TICKS
         && isRetainable(servant, target)
         && commandAllows(servant, target);
   }

   public static Vec3 lastKnownPosition(ServantEntity servant) {
      var data = servant.getPersistentData();
      if (!data.hasUUID(TARGET_UUID)) return servant.position();
      return new Vec3(data.getDouble(TARGET_X), data.getDouble(TARGET_Y), data.getDouble(TARGET_Z));
   }

   public static void forget(ServantEntity servant) {
      var data = servant.getPersistentData();
      data.remove(TARGET_UUID);
      data.remove(TARGET_TICK);
      data.remove(TARGET_X);
      data.remove(TARGET_Y);
      data.remove(TARGET_Z);
   }

   private static boolean isRetainable(ServantEntity servant, LivingEntity target) {
      return target.level() == servant.level()
         && EntityUtils.isValidCombatTarget(servant, target)
         && !ServantMasterTargeting.isContractMaster(servant, target)
         && servant.distanceToSqr(target) <= RETAIN_DISTANCE * RETAIN_DISTANCE;
   }

   private static boolean commandAllows(ServantEntity servant, LivingEntity target) {
      if (servant.getEntityMaster() == null || servant.getCommandMode() != ServantCommandMode.STAY) return true;
      return target.distanceToSqr(Vec3.atCenterOf(servant.getStayAnchor())) <= 12.0 * 12.0;
   }
}
