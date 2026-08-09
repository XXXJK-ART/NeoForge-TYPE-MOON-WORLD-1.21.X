package net.xxxjk.TYPE_MOON_WORLD.servant.ai;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;

public final class ServantNavigationHelper {
   public static final int DEFAULT_REPATH_INTERVAL = 8;
   public static final int SHORT_REPATH_INTERVAL = 5;
   private static final String MOVEMENT_TICK = "TypeMoonMovementWriteTick";
   private static final String MOVEMENT_WRITER = "TypeMoonMovementWriter";
   private static final String PROGRESS_X = "TypeMoonCombatProgressX";
   private static final String PROGRESS_Y = "TypeMoonCombatProgressY";
   private static final String PROGRESS_Z = "TypeMoonCombatProgressZ";
   private static final String PROGRESS_TICK = "TypeMoonCombatProgressTick";
   private static final String PRESSURE_SIDE = "TypeMoonMeleePressureSide";
   private static final String PRESSURE_SIDE_UNTIL = "TypeMoonMeleePressureSideUntil";
   private static final long NO_PROGRESS_TIMEOUT = 12L;

   private ServantNavigationHelper() {
   }

   /** Detailed path outcome used by the tempo guard; the old boolean methods remain compatible. */
   public enum NavigationResult {
      MOVED, NO_PATH, BLOCKED, NO_PROGRESS, UNSAFE;

      public boolean accepted() {
         return this == MOVED;
      }
   }

   public static NavigationResult moveToPositionDetailed(
      ServantEntity entity, Vec3 target, double speed, long gameTick,
      int repathInterval, double minTargetMoveSqr, String keyPrefix) {
      if (entity == null || target == null) return NavigationResult.NO_PATH;
      BlockPos targetBlock = BlockPos.containing(target);
      if (!entity.level().hasChunkAt(targetBlock)) return NavigationResult.UNSAFE;
      AABB destination = entity.getBoundingBox().move(
         target.x - entity.getX(), target.y - entity.getY(), target.z - entity.getZ());
      if (!entity.level().noCollision(entity, destination)) return NavigationResult.UNSAFE;
      CompoundTag data = entity.getPersistentData();
      String failureKey = keyPrefix + "Failure";
      data.remove(failureKey);
      boolean accepted = moveToPositionThrottled(entity, target, speed, gameTick,
         repathInterval, minTargetMoveSqr, keyPrefix);
      if (!accepted) {
         String failure = data.getString(failureKey);
         data.remove(failureKey);
         if (NavigationResult.NO_PROGRESS.name().equals(failure)) return NavigationResult.NO_PROGRESS;
         return entity.horizontalCollision ? NavigationResult.BLOCKED : NavigationResult.NO_PATH;
      }
      if (entity.distanceToSqr(target) <= 2.25) return NavigationResult.MOVED;
      if (!actualProgress(entity, gameTick)) {
         clearMovementState(entity, keyPrefix);
         return NavigationResult.NO_PROGRESS;
      }
      String progress = keyPrefix + "Progress";
      String px = keyPrefix + "ProgressX";
      String py = keyPrefix + "ProgressY";
      String pz = keyPrefix + "ProgressZ";
      if (!data.contains(px)) {
         data.putDouble(px, entity.getX());
         data.putDouble(py, entity.getY());
         data.putDouble(pz, entity.getZ());
         data.putLong(progress, gameTick);
      } else {
         double dx = entity.getX() - data.getDouble(px);
         double dy = entity.getY() - data.getDouble(py);
         double dz = entity.getZ() - data.getDouble(pz);
         if (dx * dx + dy * dy + dz * dz >= 0.04) {
            data.putDouble(px, entity.getX());
            data.putDouble(py, entity.getY());
            data.putDouble(pz, entity.getZ());
            data.putLong(progress, gameTick);
         }
      }
      if (gameTick - data.getLong(progress) >= 12L) {
         NavigationResult result = entity.getNavigation().isDone()
            ? NavigationResult.BLOCKED : NavigationResult.NO_PROGRESS;
         clearMovementState(entity, keyPrefix);
         return result;
      }
      return NavigationResult.MOVED;
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
      // Vanilla pathfinders are deliberately conservative in water and can stop
      // making progress against a moving target. Keep navigation as a fallback,
      // but apply a bounded steering impulse while submerged.
      if (entity.isInWaterOrBubble() && target != null) {
         return steerInWater(entity, target.position(), speed, gameTick, keyPrefix);
      }
      entity.setSwimming(false);
      if (ServantEngagementService.matchup(entity, target) == ServantEngagementService.Matchup.MELEE_VS_RANGED
         && entity.distanceTo(target) > 7.0) {
         double agilitySpeed = entity.getAttributeValue(Attributes.MOVEMENT_SPEED);
         double chaseScale = 1.12 + Math.max(0.0, Math.min(0.32, (agilitySpeed - 0.20) * 0.9));
         boolean moved = moveToPositionThrottled(
            entity,
            ServantEngagementService.meleeApproachPoint(entity, target, gameTick),
            speed * chaseScale,
            gameTick,
            Math.min(repathInterval, SHORT_REPATH_INTERVAL),
            minTargetMoveSqr,
            keyPrefix + "Intercept"
         );
         return moved;
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
         if (!movementAvailable(entity, gameTick, keyPrefix)) return false;
         if (outsideBasicAttackRange(entity, target) && !actualProgress(entity, gameTick)) {
            clearMovementState(entity, keyPrefix);
            return false;
         }
         rememberMovementWriter(entity, gameTick, keyPrefix);
         limitMeleeApproachMotion(entity, target);
         return true;
      }

      if (!movementAvailable(entity, gameTick, keyPrefix)) return false;

      data.putLong(keyPrefix + "LastPathTick", gameTick);
      data.putDouble(xKey, target.getX());
      data.putDouble(yKey, target.getY());
      data.putDouble(zKey, target.getZ());
      data.putDouble(speedKey, speed);
      if (!movementAvailable(entity, gameTick, keyPrefix)) return false;
      boolean accepted = entity.getNavigation().moveTo(target, speed);
      if (accepted) {
         rememberMovementWriter(entity, gameTick, keyPrefix);
         if (outsideBasicAttackRange(entity, target) && !actualProgress(entity, gameTick)) {
            clearMovementState(entity, keyPrefix);
            return false;
         }
      }
      limitMeleeApproachMotion(entity, target);
      return accepted;
   }

   private static boolean steerInWater(ServantEntity entity, Vec3 target, double requestedSpeed, long gameTick, String keyPrefix) {
      if (!movementAvailable(entity, gameTick, keyPrefix)) return false;
      entity.setSwimming(true);
      Vec3 toTarget = target.subtract(entity.position());
      double distance = toTarget.length();
      if (distance < 0.75) {
         entity.getNavigation().stop();
         entity.setDeltaMovement(entity.getDeltaMovement().scale(0.72));
         rememberMovementWriter(entity, gameTick, keyPrefix);
         return true;
      }
      Vec3 direction = toTarget.scale(1.0 / distance);
      double maxSpeed = Math.max(0.24, Math.min(0.58, 0.20 + requestedSpeed * 0.22));
      CompoundTag data = entity.getPersistentData();
      Vec3 motion = entity.getDeltaMovement();
      double along = motion.dot(direction);
      double acceleration = Math.max(0.0, maxSpeed - along) * 0.28;
      Vec3 next = motion.scale(0.88).add(direction.scale(acceleration));
      if (next.length() > maxSpeed) {
         next = next.normalize().scale(maxSpeed);
      }
      entity.setDeltaMovement(next);
      entity.hasImpulse = true;
      rememberMovementWriter(entity, gameTick, keyPrefix);
      // Keep a low-frequency path active for collision avoidance and unloaded chunks.
      if (gameTick - data.getLong(keyPrefix + "WaterPathTick") >= 6L) {
         data.putLong(keyPrefix + "WaterPathTick", gameTick);
         entity.getNavigation().moveTo(target.x, target.y, target.z, Math.max(0.8, requestedSpeed));
      }
      return true;
   }

   /** Gives every melee servant a speed-scaled intercept burst against a retreating ranged target. */
   public static void tryMeleeClosingBurst(ServantEntity entity, LivingEntity target, long gameTick) {
      if (ServantEngagementService.matchup(entity, target) != ServantEngagementService.Matchup.MELEE_VS_RANGED
         || entity.isPerformingAction() || !entity.onGround() || entity.distanceTo(target) <= 7.0
         || entity.distanceTo(target) > 30.0 || target.getY() - entity.getY() > 4.0) return;
      double agilitySpeed = entity.getAttributeValue(Attributes.MOVEMENT_SPEED);
      CompoundTag data = entity.getPersistentData();
      int cooldown = Math.max(6, 16 - (int)Math.round(agilitySpeed * 20.0));
      if (gameTick - data.getLong("ServantMeleeClosingBurstTick") < cooldown) return;
      Vec3 direction = target.position().add(target.getDeltaMovement().scale(4.0))
         .subtract(entity.position()).multiply(1.0, 0.0, 1.0);
      if (direction.lengthSqr() < 1.0E-4) return;
      direction = direction.normalize();
      Vec3 lane = direction.scale(1.6);
      if (!entity.level().noCollision(entity, entity.getBoundingBox().move(lane.x, 0.18, lane.z))) return;
      if (!movementAvailable(entity, gameTick, "CombatMeleeBurst")) return;
      Vec3 motion = entity.getDeltaMovement();
      double desired = Math.max(0.42, Math.min(0.78, 0.30 + agilitySpeed));
      double current = motion.x * direction.x + motion.z * direction.z;
      double boost = Math.max(0.0, desired - current);
      entity.setDeltaMovement(motion.x + direction.x * boost, Math.max(motion.y, 0.18),
         motion.z + direction.z * boost);
      entity.hasImpulse = true;
      rememberMovementWriter(entity, gameTick, "CombatMeleeBurst");
      data.putLong("ServantMeleeClosingBurstTick", gameTick);
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
      if (entity.isInWaterOrBubble()) {
         return steerInWater(entity, target, speed, gameTick, keyPrefix);
      }
      entity.setSwimming(false);
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
         if (!movementAvailable(entity, gameTick, keyPrefix)) return false;
         LivingEntity combatTarget = entity.getTarget();
         if (combatTarget != null && outsideBasicAttackRange(entity, combatTarget)
            && !actualProgress(entity, gameTick)) {
            data.putString(keyPrefix + "Failure", NavigationResult.NO_PROGRESS.name());
            clearMovementState(entity, keyPrefix);
            return false;
         }
         rememberMovementWriter(entity, gameTick, keyPrefix);
         return true;
      }

      if (!movementAvailable(entity, gameTick, keyPrefix)) return false;

      data.putLong(keyPrefix + "LastPathTick", gameTick);
      data.putDouble(xKey, target.x);
      data.putDouble(yKey, target.y);
      data.putDouble(zKey, target.z);
      data.putDouble(speedKey, speed);
      boolean accepted = entity.getNavigation().moveTo(target.x, target.y, target.z, speed);
      if (accepted) {
         rememberMovementWriter(entity, gameTick, keyPrefix);
         LivingEntity combatTarget = entity.getTarget();
         if (combatTarget != null && outsideBasicAttackRange(entity, combatTarget)
            && !actualProgress(entity, gameTick)) {
            data.putString(keyPrefix + "Failure", NavigationResult.NO_PROGRESS.name());
            clearMovementState(entity, keyPrefix);
            return false;
         }
      }
      return accepted;
   }

   /** Allows one movement writer per server tick; repeated path/impulse writes are ignored. */
   public static boolean movementAvailable(ServantEntity entity, long gameTick, String writer) {
      if (entity == null) return false;
      CompoundTag data = entity.getPersistentData();
      long claimedTick = data.getLong(MOVEMENT_TICK);
      return claimedTick != gameTick || writer == null || writer.equals(data.getString(MOVEMENT_WRITER));
   }

   public static void rememberMovementWriter(ServantEntity entity, long gameTick, String writer) {
      if (entity == null) return;
      CompoundTag data = entity.getPersistentData();
      data.putLong(MOVEMENT_TICK, gameTick);
      data.putString(MOVEMENT_WRITER, writer == null ? "unknown" : writer);
   }

   /** Clears stale navigation ownership so a failed path cannot keep winning future ticks. */
   public static void clearMovementState(ServantEntity entity) {
      if (entity == null) return;
      CompoundTag data = entity.getPersistentData();
      data.remove(MOVEMENT_TICK);
      data.remove(MOVEMENT_WRITER);
      data.remove(PROGRESS_X);
      data.remove(PROGRESS_Y);
      data.remove(PROGRESS_Z);
      data.remove(PROGRESS_TICK);
      entity.getNavigation().stop();
   }

   /** Also removes one movement writer's cached destination and progress sample. */
   public static void clearMovementState(ServantEntity entity, String keyPrefix) {
      clearMovementState(entity);
      if (entity == null || keyPrefix == null || keyPrefix.isBlank()) return;
      CompoundTag data = entity.getPersistentData();
      data.remove(keyPrefix + "TargetX");
      data.remove(keyPrefix + "TargetY");
      data.remove(keyPrefix + "TargetZ");
      data.remove(keyPrefix + "Speed");
      data.remove(keyPrefix + "LastPathTick");
      data.remove(keyPrefix + "WaterPathTick");
      data.remove(keyPrefix + "Progress");
      data.remove(keyPrefix + "ProgressX");
      data.remove(keyPrefix + "ProgressY");
      data.remove(keyPrefix + "ProgressZ");
   }

   private static boolean outsideBasicAttackRange(ServantEntity entity, LivingEntity target) {
      return !ServantCombatTempoService.canAttemptBasicAttack(entity, target);
   }

   private static boolean actualProgress(ServantEntity entity, long gameTick) {
      CompoundTag data = entity.getPersistentData();
      if (!data.contains(PROGRESS_X)) {
         data.putDouble(PROGRESS_X, entity.getX());
         data.putDouble(PROGRESS_Y, entity.getY());
         data.putDouble(PROGRESS_Z, entity.getZ());
         data.putLong(PROGRESS_TICK, gameTick);
         return true;
      }
      double dx = entity.getX() - data.getDouble(PROGRESS_X);
      double dy = entity.getY() - data.getDouble(PROGRESS_Y);
      double dz = entity.getZ() - data.getDouble(PROGRESS_Z);
      if (dx * dx + dy * dy + dz * dz >= 0.04) {
         data.putDouble(PROGRESS_X, entity.getX());
         data.putDouble(PROGRESS_Y, entity.getY());
         data.putDouble(PROGRESS_Z, entity.getZ());
         data.putLong(PROGRESS_TICK, gameTick);
         return true;
      }
      return gameTick - data.getLong(PROGRESS_TICK) < NO_PROGRESS_TIMEOUT;
   }

   /** Decelerates agile melee units near their opponent and removes excessive lateral drift. */
   public static void limitMeleeApproachMotion(ServantEntity entity, LivingEntity target) {
      boolean pressure = ServantCombatTempoService.inMeleePressure(entity, entity.level().getGameTime())
         || ServantCombatTempoService.isMeleeOverride(entity);
      if ((!ServantEngagementService.isMeleeDuel(entity, target) && !pressure)
         || entity.distanceTo(target) > 14.0) return;
      Vec3 toward = target.position().subtract(entity.position()).multiply(1.0, 0.0, 1.0);
      if (toward.lengthSqr() < 1.0E-4) return;
      Vec3 motion = entity.getDeltaMovement();
      double movementAttribute = entity.getAttributeValue(Attributes.MOVEMENT_SPEED);
      double distance = entity.distanceTo(target);
      Vec3 adjusted = pressure
         ? stabilizePressureMotion(motion, toward, distance, movementAttribute, target.getDeltaMovement())
         : stabilizeApproachMotion(motion, toward, distance, movementAttribute, target.getDeltaMovement());
      if (adjusted.distanceToSqr(motion) > 1.0E-6) {
         entity.setDeltaMovement(adjusted);
         entity.hasImpulse = true;
      }
   }

   /** Target-facing footwork used while a shared melee pressure window is active. */
   public static void applyMeleePressureFootwork(ServantEntity entity, LivingEntity target, long gameTick) {
      if (entity == null || target == null || !target.isAlive()) return;
      stopIfMoving(entity);
      entity.getLookControl().setLookAt(target, 65.0F, 55.0F);
      entity.faceToward(target.position());
      double distance = entity.distanceTo(target);
      CompoundTag data = entity.getPersistentData();
      if (!data.contains(PRESSURE_SIDE) || gameTick >= data.getLong(PRESSURE_SIDE_UNTIL)) {
         int side = data.contains(PRESSURE_SIDE) ? -data.getInt(PRESSURE_SIDE)
            : (Math.floorMod(entity.getId(), 2) == 0 ? 1 : -1);
         data.putInt(PRESSURE_SIDE, side == 0 ? 1 : side);
         data.putLong(PRESSURE_SIDE_UNTIL, gameTick + 18L);
      }
      float forward = distance > 2.55 ? 0.58F : 0.12F;
      float lateral = distance > 2.55 ? 0.0F : data.getInt(PRESSURE_SIDE) * 0.10F;
      entity.getMoveControl().strafe(forward, lateral);
      Vec3 motion = entity.getDeltaMovement();
      Vec3 toward = target.position().subtract(entity.position()).multiply(1.0, 0.0, 1.0);
      Vec3 adjusted = stabilizePressureMotion(motion, toward, distance,
         entity.getAttributeValue(Attributes.MOVEMENT_SPEED), target.getDeltaMovement());
      if (adjusted.distanceToSqr(motion) > 1.0E-6) {
         entity.setDeltaMovement(adjusted);
         entity.hasImpulse = true;
      }
   }

   static Vec3 stabilizePressureMotion(Vec3 motion, Vec3 toward, double distance, double movementAttribute) {
      return stabilizePressureMotion(motion, toward, distance, movementAttribute, Vec3.ZERO);
   }

   static Vec3 stabilizePressureMotion(Vec3 motion, Vec3 toward, double distance,
                                       double movementAttribute, Vec3 targetMotion) {
      if (motion == null) return Vec3.ZERO;
      Vec3 direction = toward == null ? Vec3.ZERO : toward.multiply(1.0, 0.0, 1.0);
      if (direction.lengthSqr() < 1.0E-4) return motion;
      direction = direction.normalize();
      double rawForward = motion.x * direction.x + motion.z * direction.z;
      Vec3 lateral = new Vec3(motion.x - direction.x * rawForward, 0.0,
         motion.z - direction.z * rawForward);
      double minimumForward = distance > 2.55 ? Math.min(0.20, 0.10 + movementAttribute * 0.24) : -0.03;
      double maximumForward = distance > 2.55 ? Math.min(0.36, 0.20 + movementAttribute * 0.45) : 0.15;
      double forward = Math.max(minimumForward, Math.min(maximumForward, rawForward));
      if (distance <= 4.8) {
         forward = Math.min(forward, relativeForwardLimit(targetMotion, direction, 0.26, -0.03));
      }
      double lateralLimit = distance > 2.55 ? 0.035 : 0.09;
      if (lateral.lengthSqr() > lateralLimit * lateralLimit) {
         lateral = lateral.normalize().scale(lateralLimit);
      }
      return direction.scale(forward).add(lateral).add(0.0, motion.y, 0.0);
   }

   static Vec3 stabilizeApproachMotion(Vec3 motion, Vec3 toward, double distance,
                                      double movementAttribute) {
      return stabilizeApproachMotion(motion, toward, distance, movementAttribute, Vec3.ZERO);
   }

   static Vec3 stabilizeApproachMotion(Vec3 motion, Vec3 toward, double distance,
                                       double movementAttribute, Vec3 targetMotion) {
      Vec3 direction = toward.normalize();
      double rawForward = motion.x * direction.x + motion.z * direction.z;
      Vec3 lateral = new Vec3(motion.x - direction.x * rawForward, 0.0,
         motion.z - direction.z * rawForward);
      double maxForward = distance <= 4.5 ? 0.24 : Math.min(0.58, 0.34 + movementAttribute * 0.8);
      double forward = Math.max(-0.12, Math.min(maxForward, rawForward));
      if (distance <= 4.8) {
         forward = Math.min(forward, relativeForwardLimit(targetMotion, direction, 0.28, -0.04));
      }
      double lateralLimit = Math.min(0.18, 0.08 + movementAttribute * 0.3);
      if (lateral.lengthSqr() > lateralLimit * lateralLimit) lateral = lateral.normalize().scale(lateralLimit);
      return direction.scale(forward).add(lateral).add(0.0, motion.y, 0.0);
   }

   private static double relativeForwardLimit(Vec3 targetMotion, Vec3 direction,
                                              double maximumClosingSpeed, double minimum) {
      Vec3 target = targetMotion == null ? Vec3.ZERO : targetMotion;
      double targetForward = target.x * direction.x + target.z * direction.z;
      targetForward = Math.max(-0.24, Math.min(0.45, targetForward));
      return Math.max(minimum, targetForward + maximumClosingSpeed);
   }

   public static boolean stopIfMoving(ServantEntity entity) {
      if (entity.getNavigation().isDone()) {
         return false;
      }
      entity.getNavigation().stop();
      return true;
   }
}
