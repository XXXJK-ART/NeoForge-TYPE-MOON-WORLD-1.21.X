package net.xxxjk.TYPE_MOON_WORLD.servant.ai.module;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantAiContext;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantAiModule;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantFaction;

public final class HostileTargetingModule implements ServantAiModule {
   @Override
   public void tick(ServantEntity entity, ServantAiContext context) {
      LivingEntity currentTarget = entity.getTarget();
      if (currentTarget != null && !currentTarget.isDeadOrDying()
            && currentTarget.isAlive() && currentTarget.distanceTo(entity) < 32.0) {
         return;
      }

      ServantFaction faction = entity.getDefinition() != null
         ? entity.getDefinition().faction()
         : ServantFaction.HUMAN;

      LivingEntity nearestHostile = null;
      double nearestDist = Double.MAX_VALUE;

      for (LivingEntity le : entity.level().getEntitiesOfClass(
         LivingEntity.class,
         entity.getBoundingBox().inflate(16.0),
         e -> e != entity && e.isAlive()
      )) {
         if (!isHostileTo(le, faction, entity)) {
            continue;
         }
         double dist = entity.distanceToSqr(le);
         if (dist < nearestDist) {
            nearestDist = dist;
            nearestHostile = le;
         }
      }

      if (nearestHostile != null) {
         entity.setTarget(nearestHostile);
      }
   }

   private static boolean isHostileTo(LivingEntity other, ServantFaction myFaction, ServantEntity self) {
      if (other instanceof Player player) {
         return !player.isCreative() && !player.isSpectator();
      }
      if (other instanceof Enemy) {
         return true;
      }
      if (other instanceof ServantEntity otherServant) {
         ServantFaction otherFaction = otherServant.getDefinition() != null
            ? otherServant.getDefinition().faction()
            : ServantFaction.HUMAN;
         return isFactionEnemy(myFaction, otherFaction);
      }
      return false;
   }

   private static boolean isFactionEnemy(ServantFaction a, ServantFaction b) {
      if (a == b) return false;
      return switch (a) {
         case HEAVEN -> b == ServantFaction.BEAST;
         case EARTH -> b == ServantFaction.BEAST;
         case HUMAN -> b == ServantFaction.BEAST;
         case STAR -> b == ServantFaction.BEAST || b == ServantFaction.HEAVEN;
         case BEAST -> true;
      };
   }
}
