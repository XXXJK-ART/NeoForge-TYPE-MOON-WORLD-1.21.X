package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import java.util.Comparator;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;

public final class ArashAimHelper {
   public static final double AUTO_AIM_RANGE = 200.0;
   public static final double AUTO_AIM_ANGLE_DEGREES = 8.0;
   public static final double ARROW_RAIN_ASSIST_RADIUS = 12.0;
   private static final double PROJECTILE_DRAG = 0.99;
   private static final double MAX_TARGET_SPEED = 0.8;
   private static final double MAX_HORIZONTAL_LEAD = 32.0;
   private static final double MAX_VERTICAL_LEAD = 4.0;

   private ArashAimHelper() {
   }

   public static Vec3 autoAimDirection(LivingEntity shooter, Vec3 origin, Vec3 lookDirection, double projectileSpeed) {
      Vec3 fallback = normalizedOrFallback(lookDirection, shooter.getLookAngle());
      LivingEntity target = findAutoAimTarget(shooter, origin, fallback);
      return target == null ? fallback
         : clampDirectionToCone(fallback, leadDirection(origin, target, projectileSpeed), AUTO_AIM_ANGLE_DEGREES);
   }

   public static LivingEntity findAutoAimTarget(LivingEntity shooter, Vec3 origin, Vec3 lookDirection) {
      if (shooter == null || shooter.level() == null) return null;
      Vec3 look = normalizedOrFallback(lookDirection, shooter.getLookAngle());
      double coneRadius = Math.tan(Math.toRadians(AUTO_AIM_ANGLE_DEGREES)) * AUTO_AIM_RANGE + 2.0;
      AABB search = new AABB(origin, origin.add(look.scale(AUTO_AIM_RANGE))).inflate(coneRadius);
      Level level = shooter.level();
      return level.getEntitiesOfClass(LivingEntity.class, search, target ->
            EntityUtils.isValidCombatTarget(shooter, target)
               && shooter.hasLineOfSight(target)
               && isWithinAimCone(look, target.getEyePosition().subtract(origin), AUTO_AIM_ANGLE_DEGREES)
               && target.getEyePosition().distanceToSqr(origin) <= AUTO_AIM_RANGE * AUTO_AIM_RANGE)
         .stream()
         .min(Comparator.comparingDouble(target -> aimScore(origin, look, target)))
         .orElse(null);
   }

   public static LivingEntity findTargetNearPoint(LivingEntity shooter, Vec3 center, double radius) {
      if (shooter == null || shooter.level() == null || center == null || radius <= 0.0) return null;
      double radiusSqr = radius * radius;
      return shooter.level().getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(radius),
            target -> EntityUtils.isValidCombatTarget(shooter, target)
               && target.getBoundingBox().getCenter().distanceToSqr(center) <= radiusSqr
               && target.distanceToSqr(shooter) <= AUTO_AIM_RANGE * AUTO_AIM_RANGE)
         .stream()
         .min(Comparator.comparingDouble(target -> target.getBoundingBox().getCenter().distanceToSqr(center)))
         .orElse(null);
   }

   public static Vec3 leadDirection(Vec3 origin, LivingEntity target, double projectileSpeed) {
      return predictedAimPoint(origin, target, projectileSpeed).subtract(origin).normalize();
   }

   public static Vec3 predictedAimPoint(Vec3 origin, LivingEntity target, double projectileSpeed) {
      Vec3 base = target.getEyePosition();
      Vec3 predicted = base;
      for (int iteration = 0; iteration < 3; iteration++) {
         double flightTicks = estimateFlightTicks(origin.distanceTo(predicted), projectileSpeed);
         predicted = base.add(predictionOffset(target.getDeltaMovement(), flightTicks));
      }
      return predicted;
   }

   public static Vec3 predictionOffset(Vec3 targetVelocity, double flightTicks) {
      Vec3 horizontal = new Vec3(targetVelocity.x, 0.0, targetVelocity.z);
      if (horizontal.length() > MAX_TARGET_SPEED) horizontal = horizontal.normalize().scale(MAX_TARGET_SPEED);
      Vec3 horizontalLead = horizontal.scale(Math.max(0.0, flightTicks));
      if (horizontalLead.length() > MAX_HORIZONTAL_LEAD) {
         horizontalLead = horizontalLead.normalize().scale(MAX_HORIZONTAL_LEAD);
      }
      double verticalLead = Math.max(-MAX_VERTICAL_LEAD,
         Math.min(MAX_VERTICAL_LEAD, targetVelocity.y * Math.max(0.0, flightTicks)));
      return horizontalLead.add(0.0, verticalLead, 0.0);
   }

   public static double estimateFlightTicks(double distance, double projectileSpeed) {
      double speed = Math.max(0.01, projectileSpeed);
      double remaining = 1.0 - Math.max(0.0, distance) * (1.0 - PROJECTILE_DRAG) / speed;
      if (remaining <= 0.0) return Math.max(0.0, distance) / speed;
      return Math.max(0.0, Math.log(remaining) / Math.log(PROJECTILE_DRAG));
   }

   public static boolean isWithinAimCone(Vec3 lookDirection, Vec3 toTarget, double maxAngleDegrees) {
      if (lookDirection.lengthSqr() < 1.0E-8 || toTarget.lengthSqr() < 1.0E-8) return false;
      double threshold = Math.cos(Math.toRadians(Math.max(0.1, Math.min(89.0, maxAngleDegrees))));
      return lookDirection.normalize().dot(toTarget.normalize()) + 1.0E-9 >= threshold;
   }

   public static Vec3 clampDirectionToCone(Vec3 lookDirection, Vec3 desiredDirection, double maxAngleDegrees) {
      Vec3 look = normalizedOrFallback(lookDirection, Vec3.ZERO);
      Vec3 desired = normalizedOrFallback(desiredDirection, look);
      double dot = Math.max(-1.0, Math.min(1.0, look.dot(desired)));
      double angle = Math.acos(dot);
      double maximum = Math.toRadians(Math.max(0.1, Math.min(89.0, maxAngleDegrees)));
      if (angle <= maximum) return desired;
      if (angle < 1.0E-6 || Math.sin(angle) < 1.0E-6) return look;
      double ratio = maximum / angle;
      double denominator = Math.sin(angle);
      return look.scale(Math.sin((1.0 - ratio) * angle) / denominator)
         .add(desired.scale(Math.sin(ratio * angle) / denominator)).normalize();
   }

   private static double aimScore(Vec3 origin, Vec3 look, LivingEntity target) {
      Vec3 toTarget = target.getEyePosition().subtract(origin);
      double distance = toTarget.length();
      double dot = distance < 1.0E-6 ? 1.0 : look.dot(toTarget.scale(1.0 / distance));
      return (1.0 - dot) * 1000.0 + distance / AUTO_AIM_RANGE;
   }

   private static Vec3 normalizedOrFallback(Vec3 direction, Vec3 fallback) {
      if (direction != null && direction.lengthSqr() >= 1.0E-8) return direction.normalize();
      if (fallback != null && fallback.lengthSqr() >= 1.0E-8) return fallback.normalize();
      return new Vec3(0.0, 0.0, 1.0);
   }
}
