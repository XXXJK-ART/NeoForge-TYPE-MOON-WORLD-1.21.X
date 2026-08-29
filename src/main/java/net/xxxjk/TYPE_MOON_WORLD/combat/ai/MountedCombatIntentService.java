package net.xxxjk.TYPE_MOON_WORLD.combat.ai;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.xxxjk.TYPE_MOON_WORLD.entity.IskandarMountEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.MedusaPegasusEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.ZhaoYunHakuryuEntity;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import org.jetbrains.annotations.Nullable;

/**
 * Relays a mounted mob's combat intent to an ordinary mob mount.
 *
 * Dedicated mounts keep their own movement controller. Other PathfinderMobs
 * can at least receive the rider's target and use vanilla navigation.
 */
public final class MountedCombatIntentService {
   private MountedCombatIntentService() {
   }

   public static void tick(Mob rider) {
      if (!(rider.level() instanceof ServerLevel)
         || !(rider.getVehicle() instanceof Mob mount)
         || mount == rider
         || !mount.isAlive()
         || mount.level() != rider.level()
         || hasPlayerPassenger(mount)
         || isDedicatedMount(mount)) {
         return;
      }

      LivingEntity target = resolveTarget(rider);
      if (target == null) {
         return;
      }
      mount.setTarget(target);
      if (!(mount instanceof PathfinderMob pathfinder)) {
         return;
      }

      double stopDistance = 2.8 + rider.getBbWidth() * 0.5 + target.getBbWidth() * 0.5;
      if (mount.distanceTo(target) > stopDistance) {
         double speed = Math.max(0.55, mount.getAttributeValue(
            net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED) * 1.15);
         if (mount.tickCount % 4 == Math.floorMod(rider.getId(), 4)
            || pathfinder.getNavigation().isDone()) {
            pathfinder.getNavigation().moveTo(target, speed);
         }
      } else {
         pathfinder.getNavigation().stop();
      }
   }

   @Nullable
   private static LivingEntity resolveTarget(Mob rider) {
      LivingEntity target = EntityUtils.redirectMountedCombatTarget(rider, rider.getTarget());
      if (isValidTarget(rider, target)) {
         return target;
      }

      LivingEntity attacker = rider.getLastHurtByMob();
      if (isValidTarget(rider, attacker)
         && rider.tickCount - rider.getLastHurtByMobTimestamp() <= 200) {
         return attacker;
      }
      return null;
   }

   private static boolean isValidTarget(Mob rider, @Nullable LivingEntity target) {
      return target != null
         && target.isAlive()
         && target != rider
         && target != rider.getVehicle()
         && !EntityUtils.isImmunePlayerTarget(target)
         && !EntityUtils.isUntargetableServantTransition(target)
         && EntityUtils.isValidCombatTarget(rider, target);
   }

   private static boolean hasPlayerPassenger(Mob mount) {
      for (Entity passenger : mount.getPassengers()) {
         if (passenger instanceof Player) {
            return true;
         }
      }
      return false;
   }

   private static boolean isDedicatedMount(Mob mount) {
      return mount instanceof IskandarMountEntity
         || mount instanceof ZhaoYunHakuryuEntity
         || mount instanceof MedusaPegasusEntity;
   }
}
