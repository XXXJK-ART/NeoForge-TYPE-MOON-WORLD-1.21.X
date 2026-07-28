package net.xxxjk.TYPE_MOON_WORLD.servant.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantClassType;

/** Long-range interception used after an opponent has been launched out of the normal combat envelope. */
public final class ServantPursuitService {
   private static final String LAST_BURST_TICK = "ServantDistantPursuitBurstTick";
   private static final int BURST_COOLDOWN_TICKS = 14;
   private static final double MAX_LEAD_DISTANCE = 8.0;

   private ServantPursuitService() { }

   public static boolean shouldPursue(ServantEntity servant, LivingEntity target) {
      if (target == null || !target.isAlive() || servant.isPerformingAction()) return false;
      double distance = servant.distanceTo(target);
      return distance > pursuitStartDistance(servant)
         && distance <= ServantTargetingService.RETAIN_DISTANCE;
   }

   public static void pursue(ServantEntity servant, LivingEntity target, long now) {
      if (!shouldPursue(servant, target)) return;
      double distance = servant.distanceTo(target);
      Vec3 intercept = interceptPosition(target, distance);
      double speed = pursuitSpeed(distance);
      servant.getLookControl().setLookAt(target, 45.0F, 35.0F);
      servant.setSprinting(true);
      boolean pathStarted = ServantNavigationHelper.moveToPositionThrottled(
         servant, intercept, speed, now, 4, 0.5, "ServantDistantPursuitPath"
      );
      if (!pathStarted) {
         servant.getMoveControl().setWantedPosition(intercept.x, intercept.y, intercept.z, speed);
      }
      tryForwardBurst(servant, target, distance, now);
   }

   static double pursuitStartDistance(ServantClassType classType) {
      return switch (classType) {
         case ARCHER -> 88.0;
         case CASTER -> 56.0;
         default -> 36.0;
      };
   }

   static double pursuitSpeed(double distance) {
      if (distance >= 96.0) return 2.2;
      if (distance >= 64.0) return 1.95;
      if (distance >= 44.0) return 1.7;
      return 1.5;
   }

   static double leadTicks(double distance) {
      return Math.max(2.0, Math.min(10.0, distance / 8.0));
   }

   private static double pursuitStartDistance(ServantEntity servant) {
      return servant.getDefinition() == null
         ? 40.0
         : pursuitStartDistance(servant.getDefinition().classType());
   }

   private static Vec3 interceptPosition(LivingEntity target, double distance) {
      Vec3 velocity = target.getDeltaMovement().multiply(1.0, 0.0, 1.0);
      Vec3 lead = velocity.scale(leadTicks(distance));
      if (lead.lengthSqr() > MAX_LEAD_DISTANCE * MAX_LEAD_DISTANCE) {
         lead = lead.normalize().scale(MAX_LEAD_DISTANCE);
      }
      return target.position().add(lead);
   }

   private static void tryForwardBurst(ServantEntity servant, LivingEntity target, double distance, long now) {
      if (!servant.onGround() || distance < 44.0 || target.getY() - servant.getY() > 6.0) return;
      var data = servant.getPersistentData();
      if (now - data.getLong(LAST_BURST_TICK) < BURST_COOLDOWN_TICKS) return;

      Vec3 horizontal = target.position().subtract(servant.position()).multiply(1.0, 0.0, 1.0);
      if (horizontal.lengthSqr() < 1.0E-4) return;
      Vec3 direction = horizontal.normalize();
      if (!hasSafeBurstLane(servant, direction)) return;

      Vec3 motion = servant.getDeltaMovement();
      double desiredForward = distance >= 72.0 ? 0.72 : 0.58;
      double currentForward = motion.x * direction.x + motion.z * direction.z;
      double acceleration = Math.max(0.0, desiredForward - currentForward);
      servant.setDeltaMovement(
         motion.x + direction.x * acceleration,
         Math.max(motion.y, 0.22),
         motion.z + direction.z * acceleration
      );
      servant.hasImpulse = true;
      data.putLong(LAST_BURST_TICK, now);
   }

   private static boolean hasSafeBurstLane(ServantEntity servant, Vec3 direction) {
      Vec3 offset = direction.scale(2.5);
      if (!servant.level().noCollision(servant, servant.getBoundingBox().move(offset.x, 0.25, offset.z))) {
         return false;
      }
      BlockPos landing = BlockPos.containing(servant.position().add(offset)).below();
      BlockState floor = servant.level().getBlockState(landing);
      return floor.isFaceSturdy(servant.level(), landing, Direction.UP)
         && servant.level().getFluidState(landing.above()).isEmpty();
   }
}
