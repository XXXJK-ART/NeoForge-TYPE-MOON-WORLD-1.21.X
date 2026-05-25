package net.xxxjk.TYPE_MOON_WORLD.servant.ai.module;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantAiContext;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantAiModule;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantFaction;

public final class HostileTargetingModule implements ServantAiModule {
   private static final String LAST_TARGET_SCAN_TICK = "ServantLastTargetScanTick";
   private static final int TARGET_SCAN_INTERVAL_TICKS = 5;

   @Override
   public void tick(ServantEntity entity, ServantAiContext context) {
      double aggressionRange = Math.max(16.0, context.behaviorProfile().aggressionRange());
      LivingEntity currentTarget = entity.getTarget();
      if (isValidCurrentTarget(entity, currentTarget, aggressionRange)) {
         return;
      }

      long gameTick = context.gameTick();
      long lastScanTick = entity.getPersistentData().getLong(LAST_TARGET_SCAN_TICK);
      if (gameTick - lastScanTick < TARGET_SCAN_INTERVAL_TICKS) {
         return;
      }
      entity.getPersistentData().putLong(LAST_TARGET_SCAN_TICK, gameTick);

      ServantFaction faction = entity.getDefinition() != null
         ? entity.getDefinition().faction()
         : ServantFaction.HUMAN;

      LivingEntity bestTarget = null;
      double bestScore = Double.NEGATIVE_INFINITY;

      for (LivingEntity le : entity.level().getEntitiesOfClass(
         LivingEntity.class,
         entity.getBoundingBox().inflate(aggressionRange),
         e -> e != entity && e.isAlive()
      )) {
         if (!isHostileTo(le, faction, entity)) {
            continue;
         }

         double score = scoreTarget(entity, le, aggressionRange);
         if (score > bestScore) {
            bestScore = score;
            bestTarget = le;
         }
      }

      if (bestTarget != null) {
         entity.setTarget(bestTarget);
      } else if (currentTarget != null) {
         entity.setTarget(null);
      }
   }

   private static boolean isValidCurrentTarget(ServantEntity entity, LivingEntity target, double aggressionRange) {
      if (target == null || target.isDeadOrDying() || !target.isAlive()) {
         return false;
      }

      double maxDistanceSqr = aggressionRange * aggressionRange * 1.35;
      double targetDistanceSqr = entity.distanceToSqr(target);
      if (targetDistanceSqr > maxDistanceSqr) {
         return false;
      }

      return entity.getSensing().hasLineOfSight(target) || targetDistanceSqr < 16.0;
   }

   private static double scoreTarget(ServantEntity entity, LivingEntity target, double aggressionRange) {
      double distanceSqr = entity.distanceToSqr(target);
      double score = aggressionRange * aggressionRange - distanceSqr;

      if (entity.getLastHurtByMob() == target) {
         score += 180.0;
      }
      if (target instanceof Mob mob && mob.getTarget() == entity) {
         score += 140.0;
      }
      if (entity.getSensing().hasLineOfSight(target)) {
         score += 40.0;
      }
      if (target instanceof Enemy) {
         score += 25.0;
      }
      if (target.getHealth() < target.getMaxHealth() * 0.35F) {
         score += 15.0;
      }

      return score;
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
