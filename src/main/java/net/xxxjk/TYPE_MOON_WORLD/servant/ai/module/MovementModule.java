package net.xxxjk.TYPE_MOON_WORLD.servant.ai.module;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantAiContext;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantAiModule;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.MedeaEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.MedeaWorkshopHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.MedusaCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.MedusaEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;

public final class MovementModule implements ServantAiModule {
   private static final int WANDER_INTERVAL = 120;
   private static final int WANDER_RANGE = 8;

   @Override
   public void tick(ServantEntity entity, ServantAiContext context) {
      if (entity instanceof MedeaEntity medea) {
         this.tickMedea(medea, context);
         return;
      }
      if (entity instanceof MedusaEntity medusa) {
         this.tickMedusa(medusa, context);
         return;
      }

      LivingEntity target = context.target();
      if (target != null) {
         return;
      }

      int wanderInterval = Math.max(50, (int)(WANDER_INTERVAL - context.behaviorProfile().aggressionRange()));
      int wanderRange = Math.max(WANDER_RANGE, (int)Math.round(context.behaviorProfile().attackCommitDistance() * 2.0));
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
         entity.getNavigation().moveTo(wanderTarget.getX() + 0.5, wanderTarget.getY(), wanderTarget.getZ() + 0.5, wanderSpeed);
      }
   }

   private void tickMedea(MedeaEntity entity, ServantAiContext context) {
      LivingEntity target = context.target();
      if (target != null && target.isAlive()) {
         double distance = entity.distanceTo(target);
         if (entity.isFlyingMode()) {
            entity.getNavigation().stop();
            Vec3 toTarget = target.position().subtract(entity.position());
            Vec3 horizontal = new Vec3(toTarget.x, 0.0, toTarget.z);
            Vec3 forward = horizontal.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : horizontal.normalize();
            Vec3 right = new Vec3(-forward.z, 0.0, forward.x);
            double orbit = Math.sin(entity.tickCount * 0.15) * 2.4;
            Vec3 desired = target.position().subtract(forward.scale(10.5)).add(right.scale(orbit)).add(0.0, 3.0, 0.0);
            entity.setDeltaMovement(desired.subtract(entity.position()).scale(0.08));
            entity.hasImpulse = true;
         } else if (distance <= 4.0) {
            Vec3 away = entity.position().subtract(target.position());
            if (away.lengthSqr() > 1.0E-4) {
               away = away.normalize().scale(5.5);
               entity.getNavigation().moveTo(entity.getX() + away.x, entity.getY(), entity.getZ() + away.z, 1.1);
            }
         } else if (distance > 11.0) {
            entity.getNavigation().moveTo(target, 0.95);
         }
         return;
      }

      entity.setFlyingMode(false);
      Vec3 center = MedeaWorkshopHelper.getWorkshopCenter(entity);
      if (center != null && entity.distanceToSqr(center) > 16.0) {
         entity.getNavigation().moveTo(center.x, center.y, center.z, 0.7);
         return;
      }

      if (entity.getNavigation().isDone() && entity.getRandom().nextInt(80) == 0) {
         BlockPos current = entity.blockPosition();
         int dx = entity.getRandom().nextIntBetweenInclusive(-6, 6);
         int dz = entity.getRandom().nextIntBetweenInclusive(-6, 6);
         BlockPos wanderTarget = current.offset(dx, 0, dz);
         entity.getNavigation().moveTo(wanderTarget.getX() + 0.5, wanderTarget.getY(), wanderTarget.getZ() + 0.5, 0.7);
      }
   }

   private void tickMedusa(MedusaEntity entity, ServantAiContext context) {
      LivingEntity target = context.target();
      if (target != null && target.isAlive()) {
         if (MedusaCombatHelper.isBusy(entity)) {
            entity.getNavigation().stop();
            return;
         }

         double distance = entity.distanceTo(target);
         entity.setCrouchPose(false);
         if (distance > 6.5) {
            entity.getNavigation().moveTo(target, 1.28);
         } else if (distance > 2.2) {
            entity.getNavigation().moveTo(target, 1.22);
            float side = entity.getRandom().nextBoolean() ? 0.65F : -0.65F;
            entity.getMoveControl().strafe(0.28F, side);
         } else {
            entity.getNavigation().stop();
            float side = entity.getRandom().nextBoolean() ? 0.85F : -0.85F;
            entity.getMoveControl().strafe(0.15F, side);
         }

         if ((target.getY() - entity.getY() > 1.5 || distance > 5.0 && !entity.getSensing().hasLineOfSight(target))
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
         entity.getNavigation().moveTo(wanderTarget.getX() + 0.5, wanderTarget.getY(), wanderTarget.getZ() + 0.5, 0.75);
      }
   }
}
