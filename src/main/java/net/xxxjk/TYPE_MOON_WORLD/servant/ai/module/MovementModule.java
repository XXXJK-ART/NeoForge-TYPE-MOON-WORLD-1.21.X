package net.xxxjk.TYPE_MOON_WORLD.servant.ai.module;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantAiContext;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantAiModule;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantNavigationHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.MedeaEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.MedeaWorkshopHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.MedusaCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.MedusaEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ParacelsusEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.skill.ParacelsusServantSkills;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;

public final class MovementModule implements ServantAiModule {
   private static final int WANDER_INTERVAL = 120;
   private static final int WANDER_RANGE = 8;

   @Override
   public void tick(ServantEntity entity, ServantAiContext context) {
      LivingEntity target = context.target();
      if (target != null) {
         // CombatModule owns all combat movement; a second writer here caused path oscillation.
         return;
      }
      if (entity instanceof MedeaEntity medea) {
         this.tickMedea(medea, context);
         return;
      }
      if (entity instanceof MedusaEntity medusa) {
         this.tickMedusa(medusa, context);
         return;
      }

      var ai = context.aiConfig();
      int wanderInterval = ai == null ? Math.max(50, (int)(WANDER_INTERVAL - context.behaviorProfile().aggressionRange())) : Math.max(50, (int)(WANDER_INTERVAL + ai.movement().wanderRadius() * 2.0));
      int wanderRange = ai == null ? Math.max(WANDER_RANGE, (int)Math.round(context.behaviorProfile().attackCommitDistance() * 2.0)) : Math.max(WANDER_RANGE, (int)Math.round(ai.movement().wanderRadius()));
      double wanderSpeed = switch (entity.getCombatDisposition()) {
         case CAUTIOUS -> 0.55;
         case FRENZIED -> 0.75;
         default -> 0.65;
      };

      if (entity.getNavigation().isDone() && entity.getRandom().nextInt(wanderInterval) == 0) {
         BlockPos current = entity.blockPosition();
         int dx = entity.getRandom().nextIntBetweenInclusive(-wanderRange, wanderRange);
         int dz = entity.getRandom().nextIntBetweenInclusive(-wanderRange, wanderRange);
         BlockPos wanderTarget = current.offset(dx, 0, dz);
         ServantNavigationHelper.moveToPositionThrottled(
            entity,
            new Vec3(wanderTarget.getX() + 0.5, wanderTarget.getY(), wanderTarget.getZ() + 0.5),
            wanderSpeed,
            context.gameTick(),
            20,
            2.0,
            "ServantWanderPath"
         );
      }
   }

   private void tickMedea(MedeaEntity entity, ServantAiContext context) {
      entity.setFlyingMode(false);
      Vec3 center = MedeaWorkshopHelper.getWorkshopCenter(entity);
      if (center != null && entity.distanceToSqr(center) > 16.0) {
         ServantNavigationHelper.moveToPositionThrottled(
            entity,
            center,
            0.7,
            context.gameTick(),
            20,
            2.0,
            "MedeaWorkshopPath"
         );
         return;
      }

      if (entity.getNavigation().isDone() && entity.getRandom().nextInt(80) == 0) {
         BlockPos current = entity.blockPosition();
         int dx = entity.getRandom().nextIntBetweenInclusive(-6, 6);
         int dz = entity.getRandom().nextIntBetweenInclusive(-6, 6);
         BlockPos wanderTarget = current.offset(dx, 0, dz);
         ServantNavigationHelper.moveToPositionThrottled(
            entity,
            new Vec3(wanderTarget.getX() + 0.5, wanderTarget.getY(), wanderTarget.getZ() + 0.5),
            0.7,
            context.gameTick(),
            20,
            2.0,
            "MedeaWanderPath"
         );
      }
   }

   private void tickMedusa(MedusaEntity entity, ServantAiContext context) {
      LivingEntity target = context.target();
      if (target != null && target.isAlive()) {
         if (MedusaCombatHelper.isBusy(entity)) {
            ServantNavigationHelper.stopIfMoving(entity);
            return;
         }

         double distance = entity.distanceTo(target);
         entity.setCrouchPose(false);
         if (distance > 6.5) {
            ServantNavigationHelper.moveToTargetThrottled(
               entity,
               target,
               1.28,
               context.gameTick(),
               ServantNavigationHelper.SHORT_REPATH_INTERVAL,
               0.85,
               "MedusaChasePath"
            );
         } else if (distance > 2.2) {
            ServantNavigationHelper.moveToTargetThrottled(
               entity,
               target,
               1.22,
               context.gameTick(),
               ServantNavigationHelper.SHORT_REPATH_INTERVAL,
               0.65,
               "MedusaPressurePath"
            );
            float side = entity.getRandom().nextBoolean() ? 0.65F : -0.65F;
            entity.getMoveControl().strafe(0.28F, side);
         } else {
            ServantNavigationHelper.stopIfMoving(entity);
            float side = entity.getRandom().nextBoolean() ? 0.85F : -0.85F;
            entity.getMoveControl().strafe(0.15F, side);
         }

         boolean shouldCheckRooftop = context.gameTick() % 5L == 0L
            && (target.getY() - entity.getY() > 1.5 || distance > 5.0 && !entity.getSensing().hasLineOfSight(target));
         if (shouldCheckRooftop
            && entity.onGround()) {
            MedusaCombatHelper.requestRooftopReposition(entity, target);
         }
         return;
      }

      entity.setEyesReleased(false);
      entity.setBlindfoldSealed(true);
      entity.setCrouchPose(false);
      if (entity.getNavigation().isDone() && entity.getRandom().nextInt(90) == 0) {
         BlockPos current = entity.blockPosition();
         int dx = entity.getRandom().nextIntBetweenInclusive(-15, 15);
         int dz = entity.getRandom().nextIntBetweenInclusive(-15, 15);
         BlockPos wanderTarget = current.offset(dx, 0, dz);
         ServantNavigationHelper.moveToPositionThrottled(
            entity,
            new Vec3(wanderTarget.getX() + 0.5, wanderTarget.getY(), wanderTarget.getZ() + 0.5),
            0.75,
            context.gameTick(),
            20,
            2.0,
            "MedusaWanderPath"
         );
      }
   }
}
