package net.xxxjk.TYPE_MOON_WORLD.servant.ai;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatFormulas;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatMotionService;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantParams;
import net.xxxjk.TYPE_MOON_WORLD.combat.ai.AiActionDescriptor;
import net.xxxjk.TYPE_MOON_WORLD.combat.ai.AiBlackboard;
import net.xxxjk.TYPE_MOON_WORLD.combat.ai.AiBrain;
import net.xxxjk.TYPE_MOON_WORLD.combat.ai.CombatCapabilitySnapshot;
import net.xxxjk.TYPE_MOON_WORLD.combat.ai.ServantCapabilityResolver;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantSkillDefinition.FactType;

/** Mid-range pursuit and route interception without teleporting or loading new chunks. */
public final class ServantManeuverService {
   public static final double MAX_NORMAL_ENGAGEMENT_DISTANCE = 48.0;
   private static final double MIN_MANEUVER_DISTANCE = 5.5;
   private static final String SIDE_REENGAGE_START = "TypeMoonSideReengageStart";
   private static final String SIDE_REENGAGE_FAILURES = "TypeMoonSideReengageFailures";
   private static final String SIDE_REENGAGE_TARGET = "TypeMoonSideReengageTarget";
   private static final String SIDE_REENGAGE_COOLDOWN = "TypeMoonSideReengageCooldown";

   private ServantManeuverService() { }

   public static boolean shouldManeuver(ServantEntity servant, LivingEntity target) {
      if (servant == null || target == null || !target.isAlive() || servant.isPerformingAction()) return false;
      double distance = servant.distanceTo(target);
      ServantAiDefinition.Tactical tactical = ServantTacticalProfileResolver.resolve(servant);
      boolean airborneIntercept = !target.onGround()
         && Math.abs(target.getY() - servant.getY()) >= 1.5
         && tactical.verticalMobility() >= 0.35;
      boolean rangedPressure = hasRangedPressure(servant, target);
      boolean antiKiteApproach = rangedPressure && distance > Math.max(5.0, tactical.minimumRange() + 1.0);
      return distance >= MIN_MANEUVER_DISTANCE && distance <= MAX_NORMAL_ENGAGEMENT_DISTANCE
         && (ServantCombatMotionService.canPursue(servant, target)
             || antiKiteApproach
             || ServantEngagementService.role(servant) == ServantEngagementService.CombatRole.MELEE
                && distance >= 10.0
                && (target.getDeltaMovement().horizontalDistanceSqr() >= 0.08 || airborneIntercept));
   }

   public static Vec3 destination(ServantEntity servant, LivingEntity target, long now, double interceptBias) {
      double distance = servant.distanceTo(target);
      double leadTicks = Math.max(2.0, Math.min(10.0, 2.0 + distance * 0.16));
      Vec3 velocity = target.getDeltaMovement();
      Vec3 lead = velocity.scale(leadTicks);
      double maxLead = Math.min(12.0, 4.0 + distance * 0.18);
      if (lead.lengthSqr() > maxLead * maxLead) lead = lead.normalize().scale(maxLead);
      Vec3 predicted = target.position().add(lead);

      if (ServantCombatDisposition.isRelentlessAdvance(servant)) {
         Vec3 direct = predicted.subtract(servant.position()).multiply(1.0, 0.0, 1.0);
         return direct.lengthSqr() < 1.0E-4 ? predicted : predicted.subtract(direct.normalize().scale(1.8));
      }

      Vec3 travel = predicted.subtract(servant.position()).multiply(1.0, 0.0, 1.0);
      if (travel.lengthSqr() < 1.0E-4) return predicted;
      travel = travel.normalize();
      int sideSign = ((servant.getId() + (int)(now / 24L)) & 1) == 0 ? 1 : -1;
      Vec3 side = new Vec3(-travel.z, 0.0, travel.x).scale(sideSign * Math.max(0.0, interceptBias));
      Vec3 candidate = predicted.add(side).subtract(travel.scale(1.8));
      Vec3 safe = findSafeDestination(servant, candidate);
      return safe == null ? predicted : safe;
   }

   public static boolean maneuver(ServantEntity servant, LivingEntity target, long now,
                                  double interceptBias, double pursuitAggression) {
      if (!shouldManeuver(servant, target)) return false;
      Vec3 destination = destination(servant, target, now, interceptBias);
      ServantParams params = servant.getDefinition() == null ? null : servant.getDefinition().parameters();
      int agility = ServantCombatFormulas.agilityStep(params);
      double speed = ServantCombatDisposition.isRelentlessAdvance(servant) ? 2.0
         : Math.min(2.0, 1.25 + agility * 0.1 + Math.max(0.0, pursuitAggression) * 0.25);
      servant.getLookControl().setLookAt(target, 50.0F, 40.0F);
      servant.setSprinting(true);
      boolean moved = ServantNavigationHelper.moveToPositionThrottled(
         servant, destination, speed, now, 4, 0.45, "ServantCombatManeuver"
      );
      boolean burst = !moved && servant.onGround() && servant.distanceTo(target) <= 24.0
         && trySafeBurst(servant, destination, agility, now, 0.25);
      return moved || burst;
   }

   public static boolean hasRangedPressure(ServantEntity servant, LivingEntity target) {
      return servant != null && target != null
         && (ServantEngagementService.role(target) == ServantEngagementService.CombatRole.RANGED
             || AiBrain.blackboard(servant).opponent(target.getUUID()).knows(FactType.PROJECTILE_PRESSURE));
   }

   public static boolean trySideForwardReengage(ServantEntity servant, LivingEntity target,
                                                 ServantAiDefinition.Tactical tactical,
                                                 AiBlackboard blackboard, long now) {
      if (servant == null || target == null || tactical == null || !target.isAlive()
         || ServantCombatMotionService.isRecovering(servant) || !ServantCapabilityResolver.isMobile(servant)) return false;
      CombatCapabilitySnapshot capability = ServantCapabilityResolver.resolve(servant);
      int agility = ServantCombatFormulas.agilityStep(servant.getDefinition() == null
         ? null : servant.getDefinition().parameters());
      boolean agileMelee = agility >= 4 && (capability.has(FactType.GAP_CLOSE) || capability.has(FactType.PURSUIT));
      boolean knownRanged = ServantEngagementService.role(target) == ServantEngagementService.CombatRole.RANGED
         || blackboard != null && blackboard.opponent(target.getUUID()).knows(FactType.PROJECTILE_PRESSURE);
      double distance = servant.distanceTo(target);
      if (!agileMelee || distance <= tactical.preferredRange() + 6.0 || !knownRanged) {
         clearSideReengage(servant);
         if (distance <= tactical.preferredRange() + 6.0) servant.getPersistentData().remove(SIDE_REENGAGE_COOLDOWN);
         return false;
      }

      var data = servant.getPersistentData();
      if (now < data.getLong(SIDE_REENGAGE_COOLDOWN)) return false;
      if (!data.hasUUID(SIDE_REENGAGE_TARGET) || !target.getUUID().equals(data.getUUID(SIDE_REENGAGE_TARGET))) {
         data.putUUID(SIDE_REENGAGE_TARGET, target.getUUID());
         data.putLong(SIDE_REENGAGE_START, now);
         data.putInt(SIDE_REENGAGE_FAILURES, 0);
      }
      if (now - data.getLong(SIDE_REENGAGE_START) >= 24L || data.getInt(SIDE_REENGAGE_FAILURES) >= 2) {
         data.putLong(SIDE_REENGAGE_COOLDOWN, now + 40L);
         clearSideReengage(servant);
         return false;
      }

      Vec3 toward = target.position().subtract(servant.position()).multiply(1.0, 0.0, 1.0);
      if (toward.lengthSqr() < 1.0E-4) return false;
      int sideSign = ((servant.getId() + (int)(now / 8L)) & 1) == 0 ? 1 : -1;
      double[] angles = {45.0 * sideSign, 70.0 * sideSign, -45.0 * sideSign, -70.0 * sideSign};
      double stride = Math.min(Math.max(6.0, tactical.repositionDistance()), Math.max(6.0, distance - tactical.preferredRange()));
      for (double angle : angles) {
         Vec3 direction = sideForwardDirection(toward, angle);
         Vec3 safe = findSafeDestination(servant, servant.position().add(direction.scale(stride)));
         if (safe == null) continue;
         boolean moved = ServantNavigationHelper.moveToPositionThrottled(
            servant, safe, Math.min(2.1, 1.45 + agility * 0.1), now, 3, 0.4, "ServantSideForwardReengage");
         if (moved) {
            servant.getLookControl().setLookAt(target, 55.0F, 40.0F);
            servant.setSprinting(true);
            return true;
         }
      }
      data.putInt(SIDE_REENGAGE_FAILURES, data.getInt(SIDE_REENGAGE_FAILURES) + 1);
      return false;
   }

   public static Vec3 sideForwardDirection(Vec3 towardTarget, double degrees) {
      Vec3 horizontal = towardTarget == null ? Vec3.ZERO : towardTarget.multiply(1.0, 0.0, 1.0);
      if (horizontal.lengthSqr() < 1.0E-4) return Vec3.ZERO;
      horizontal = horizontal.normalize();
      double radians = Math.toRadians(degrees);
      return new Vec3(horizontal.x * Math.cos(radians) - horizontal.z * Math.sin(radians), 0.0,
         horizontal.x * Math.sin(radians) + horizontal.z * Math.cos(radians)).normalize();
   }

   private static void clearSideReengage(ServantEntity servant) {
      servant.getPersistentData().remove(SIDE_REENGAGE_START);
      servant.getPersistentData().remove(SIDE_REENGAGE_FAILURES);
      servant.getPersistentData().remove(SIDE_REENGAGE_TARGET);
   }

   public static boolean approachForAction(ServantEntity servant, LivingEntity target, AiActionDescriptor action,
                                           long now, ServantAiDefinition.Tactical tactical) {
      if (servant == null || target == null || action == null || tactical == null || !target.isAlive()) return false;
      String movement = action.maneuver().movement();
      Vec3 candidate;
      if ("intercept".equals(movement) || "pursuit".equals(movement)) {
         candidate = destination(servant, target, now, tactical.interceptBias());
      } else {
         Vec3 lead = target.getDeltaMovement().scale(2.0 + tactical.pursuitAggression() * 3.0);
         candidate = target.position().add(lead);
      }
      Vec3 delta = candidate.subtract(servant.position());
      double stepLimit = Math.max(6.0, tactical.repositionDistance());
      if (delta.horizontalDistanceSqr() > stepLimit * stepLimit) {
         Vec3 horizontal = delta.multiply(1.0, 0.0, 1.0).normalize().scale(stepLimit);
         candidate = servant.position().add(horizontal).add(0.0,
            Math.max(-3.0, Math.min(3.0, delta.y * tactical.verticalMobility())), 0.0);
      }
      Vec3 safe = findSafeDestination(servant, candidate);
      if (safe == null) safe = target.position();
      int agility = ServantCombatFormulas.agilityStep(servant.getDefinition() == null
         ? null : servant.getDefinition().parameters());
      double speed = Math.min(2.1, 1.25 + agility * 0.1 + tactical.pursuitAggression() * 0.3);
      servant.getLookControl().setLookAt(target, 50.0F, 40.0F);
      servant.setSprinting(true);
      boolean moved = ServantNavigationHelper.moveToPositionThrottled(
         servant, safe, speed, now, 4, 0.4, "ServantPlannedActionApproach");
      return moved || servant.onGround() && servant.distanceTo(target) <= 24.0
         && trySafeBurst(servant, safe, agility, now, tactical.verticalMobility());
   }

   public static boolean shouldReposition(ServantEntity servant, LivingEntity target,
                                          ServantAiDefinition.Tactical tactical, long now) {
      if (servant == null || target == null || tactical == null || !target.isAlive()
         || servant.isPerformingAction() || shouldManeuver(servant, target)) return false;
      int interval = Math.max(16, 34 - (int)Math.round(tactical.pursuitAggression() * 14.0));
      if (Math.floorMod(now + servant.getId(), interval) != 0) return false;
      double distance = servant.distanceTo(target);
      if (distance < tactical.minimumRange() || distance > tactical.maximumRange()) return true;
      return tactical.repositionDistance() >= 6.0 && tactical.pursuitAggression() >= 0.3;
   }

   public static boolean reposition(ServantEntity servant, LivingEntity target,
                                    ServantAiDefinition.Tactical tactical, long now) {
      if (servant == null || target == null || tactical == null) return false;
      double distance = servant.distanceTo(target);
      Vec3 radial = servant.position().subtract(target.position()).multiply(1.0, 0.0, 1.0);
      if (radial.lengthSqr() < 1.0E-4) radial = servant.getLookAngle().scale(-1.0).multiply(1.0, 0.0, 1.0);
      if (radial.lengthSqr() < 1.0E-4) radial = new Vec3(1.0, 0.0, 0.0);
      radial = radial.normalize();
      if ("ambusher".equals(tactical.style())) {
         Vec3 look = target.getLookAngle().multiply(-1.0, 0.0, -1.0);
         if (look.lengthSqr() > 1.0E-4) radial = look.normalize();
      }
      double desiredRadius = distance < tactical.minimumRange()
         ? tactical.preferredRange() : distance > tactical.maximumRange()
            ? tactical.maximumRange() - 1.0 : tactical.preferredRange();
      double speed = distance > tactical.maximumRange() ? 1.55 : 1.2 + tactical.pursuitAggression() * 0.25;
      servant.getLookControl().setLookAt(target, 45.0F, 35.0F);
      List<ScoredDestination> candidates = scoreCandidates(servant, target, tactical, radial, desiredRadius);
      for (int index = 0; index < Math.min(3, candidates.size()); index++) {
         Vec3 destination = candidates.get(index).position();
         if (ServantNavigationHelper.moveToPositionThrottled(
            servant, destination, speed, now, 5, 0.5, "ServantTacticalReposition")) return true;
      }
      return false;
   }

   static double leadTicks(double distance) {
      return Math.max(2.0, Math.min(10.0, 2.0 + distance * 0.16));
   }

   private static boolean trySafeBurst(ServantEntity servant, Vec3 destination, int agility, long now,
                                       double verticalMobility) {
      if (now - servant.getPersistentData().getLong("ServantManeuverBurstTick") < Math.max(8, 16 - agility)) return false;
      Vec3 direction = destination.subtract(servant.position()).multiply(1.0, 0.0, 1.0);
      if (direction.lengthSqr() < 1.0E-4) return false;
      direction = direction.normalize();
      Vec3 offset = direction.scale(1.8);
      if (!servant.level().noCollision(servant, servant.getBoundingBox().move(offset.x, 0.2, offset.z))) return false;
      Vec3 motion = servant.getDeltaMovement();
      double desired = Math.min(0.95, 0.5 + agility * 0.08);
      double current = motion.x * direction.x + motion.z * direction.z;
      double boost = Math.max(0.0, desired - current);
      double lift = 0.12 + Math.max(0.0, Math.min(1.0, verticalMobility)) * 0.22;
      servant.setDeltaMovement(motion.x + direction.x * boost, Math.max(motion.y, lift), motion.z + direction.z * boost);
      servant.hasImpulse = true;
      servant.getPersistentData().putLong("ServantManeuverBurstTick", now);
      return true;
   }

   private static Vec3 findSafeDestination(ServantEntity servant, Vec3 candidate) {
      BlockPos base = BlockPos.containing(candidate);
      for (int vertical = 2; vertical >= -3; vertical--) {
         BlockPos feet = base.offset(0, vertical, 0);
         if (!servant.level().hasChunkAt(feet)) continue;
         BlockPos floorPos = feet.below();
         BlockState floor = servant.level().getBlockState(floorPos);
         if (!floor.isFaceSturdy(servant.level(), floorPos, Direction.UP)) continue;
         if (!servant.level().getFluidState(feet).isEmpty() || !servant.level().getFluidState(feet.above()).isEmpty()) continue;
         Vec3 result = Vec3.atBottomCenterOf(feet);
         Vec3 offset = result.subtract(servant.position());
         if (servant.level().noCollision(servant, servant.getBoundingBox().move(offset))) return result;
      }
      return null;
   }

   private static List<ScoredDestination> scoreCandidates(ServantEntity servant, LivingEntity target,
                                                           ServantAiDefinition.Tactical tactical, Vec3 baseDirection,
                                                           double desiredRadius) {
      double[] angles = switch (tactical.style()) {
         case "berserker" -> new double[]{0.0, 22.5, -22.5, 45.0, -45.0, 90.0};
         case "vanguard", "duelist" -> new double[]{0.0, 35.0, -35.0, 70.0, -70.0, 180.0};
         case "skirmisher", "ambusher" -> new double[]{70.0, -70.0, 110.0, -110.0, 145.0, -145.0};
         default -> new double[]{0.0, 45.0, -45.0, 90.0, -90.0, 180.0};
      };
      List<ScoredDestination> result = new ArrayList<>(6);
      double movementLimit = Math.max(6.0, tactical.repositionDistance());
      for (double angle : angles) {
         double radians = Math.toRadians(angle);
         Vec3 direction = new Vec3(baseDirection.x * Math.cos(radians) - baseDirection.z * Math.sin(radians), 0.0,
            baseDirection.x * Math.sin(radians) + baseDirection.z * Math.cos(radians));
         Vec3 candidate = target.position().add(direction.scale(desiredRadius));
         Vec3 travel = candidate.subtract(servant.position());
         if (travel.horizontalDistanceSqr() > movementLimit * movementLimit) {
            Vec3 horizontal = travel.multiply(1.0, 0.0, 1.0).normalize().scale(movementLimit);
            candidate = servant.position().add(horizontal).add(0.0,
               Math.max(-2.0, Math.min(2.0, travel.y * tactical.verticalMobility())), 0.0);
         }
         Vec3 safe = findSafeDestination(servant, candidate);
         if (safe == null) continue;
         double rangeError = Math.abs(safe.distanceTo(target.position()) - tactical.preferredRange());
         boolean lineOfSight = hasLineOfSightFrom(servant, safe, target);
         boolean rangedStyle = "sniper".equals(tactical.style()) || "controller".equals(tactical.style())
            || "support".equals(tactical.style()) || "disaster".equals(tactical.style());
         long allies = servant.level().getEntitiesOfClass(LivingEntity.class,
            new AABB(safe, safe).inflate(3.0), servant::isAlliedTo).size();
         double heightValue = Math.max(-3.0, Math.min(3.0, safe.y - target.getY())) * tactical.verticalMobility();
         Vec3 targetVelocity = target.getDeltaMovement().multiply(1.0, 0.0, 1.0);
         double interceptValue = targetVelocity.lengthSqr() < 1.0E-4 ? 0.0
            : safe.subtract(target.position()).multiply(1.0, 0.0, 1.0).normalize().dot(targetVelocity.normalize());
         boolean prefersHiddenApproach = "ambusher".equals(tactical.style());
         double score = 40.0 - rangeError * 4.0 + heightValue * (rangedStyle ? 3.0 : 1.0)
            + (lineOfSight != prefersHiddenApproach ? 8.0 : -4.0)
            - allies * tactical.collateralCaution() * 5.0 - interceptValue * tactical.interceptBias();
         result.add(new ScoredDestination(safe, score));
      }
      result.sort(Comparator.comparingDouble(ScoredDestination::score).reversed());
      return result;
   }

   private static boolean hasLineOfSightFrom(ServantEntity servant, Vec3 position, LivingEntity target) {
      Vec3 from = position.add(0.0, servant.getEyeHeight(), 0.0);
      return servant.level().clip(new ClipContext(from, target.getEyePosition(), ClipContext.Block.COLLIDER,
         ClipContext.Fluid.NONE, servant)).getType() == HitResult.Type.MISS;
   }

   private record ScoredDestination(Vec3 position, double score) { }
}
