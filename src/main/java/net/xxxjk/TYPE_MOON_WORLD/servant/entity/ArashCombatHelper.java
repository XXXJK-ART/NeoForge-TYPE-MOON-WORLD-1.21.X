package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;
import net.xxxjk.TYPE_MOON_WORLD.entity.ArashParticleArrowEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.ArashStellaControllerEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantAiContext;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantEngagementService;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantMasterTargeting;

public final class ArashCombatHelper {
   private static final String TAG_NEXT_NORMAL = "ArashNextNormalArrow";
   private static final String TAG_ATTACK_FACE_UNTIL = "ArashAttackFaceUntil";
   private static final String TAG_NEXT_RAIN = "ArashNextArrowRain";
   private static final String TAG_NEXT_SMALL = "ArashNextSmallEnergyArrow";
   private static final String TAG_NEXT_LARGE = "ArashNextLargeEnergyArrow";
   private static final String TAG_LAST_SCAN = "ArashLastTargetScan";
   private static final String TAG_LAST_REPOSITION = "ArashLastReposition";
   private static final String TAG_NEXT_CROSSOVER = "ArashNextCrossover";
   private static final String TAG_CROSSOVER_UNTIL = "ArashCrossoverUntil";
   private static final String TAG_CROSSOVER_AXIS_X = "ArashCrossoverAxisX";
   private static final String TAG_CROSSOVER_AXIS_Z = "ArashCrossoverAxisZ";
   private static final String TAG_CROSSOVER_SIDE = "ArashCrossoverSide";
   private static final String TAG_ORBIT_DIRECTION = "ArashOrbitDirection";
   private static final String TAG_NEXT_ORBIT_SWITCH = "ArashNextOrbitSwitch";
   private static final String TAG_MOTION_SAMPLE = "ArashLastMotionSample";
   private static final String TAG_MOTION_X = "ArashLastMotionX";
   private static final String TAG_MOTION_Z = "ArashLastMotionZ";
   private static final String TAG_STATIONARY_TICKS = "ArashStationaryTicks";

   private ArashCombatHelper() {
   }

   public static void tickTargeting(ArashEntity arash, ServantAiContext context) {
      LivingEntity current = arash.getTarget();
      if (isTarget(arash, current) && arash.distanceToSqr(current) <= ArashCombatRules.TARGET_RANGE * ArashCombatRules.TARGET_RANGE) {
         return;
      }
      long now = context.gameTick();
      long last = arash.getPersistentData().getLong(TAG_LAST_SCAN);
      if (last > 0L && now - last < ArashCombatRules.TARGET_SCAN_INTERVAL) return;
      arash.getPersistentData().putLong(TAG_LAST_SCAN, now);
      AABB area = arash.getBoundingBox().inflate(ArashCombatRules.TARGET_RANGE);
      LivingEntity best = arash.level().getEntitiesOfClass(LivingEntity.class, area, target -> isTarget(arash, target)).stream()
         .min(Comparator.comparingDouble(arash::distanceToSqr)).orElse(null);
      arash.setTarget(best);
   }

   public static void tick(ArashEntity arash, ServantAiContext context) {
      if (!(arash.level() instanceof ServerLevel level) || !arash.isAlive()) return;
      if (arash.getPersistentData().getBoolean(ArashEntity.TAG_STELLA_SACRIFICE)) {
         arash.getNavigation().stop();
         arash.setTarget(null);
         return;
      }
      LivingEntity target = arash.getTarget();
      if (!isTarget(arash, target)) {
         arash.setTarget(null);
         arash.getNavigation().stop();
         return;
      }
      arash.getLookControl().setLookAt(target, 40.0F, 40.0F);
      if (arash.getPersistentData().getBoolean(ArashEntity.TAG_STELLA_CHANTING)) {
         arash.getNavigation().stop();
         return;
      }

      long now = context.gameTick();
      boolean lineOfSight = arash.getSensing().hasLineOfSight(target);
      if (ArashStellaControllerEntity.tryBegin(arash, target)) return;
      tacticalMovement(arash, target, now, lineOfSight);
      int cluster = countCluster(arash, target, 7.0);

      if (now >= arash.getPersistentData().getLong(TAG_NEXT_LARGE)
         && arash.getCraftedArrowCount() >= ArashCombatRules.ENERGY_ARROW_COST
         && arash.getCurrentMp() >= ArashCombatRules.LARGE_ENERGY_MANA
         && (target.getMaxHealth() >= 160.0F || cluster >= 4)) {
         lockAttackFacing(arash, target, now, 22);
         arash.setCurrentMp(arash.getCurrentMp() - ArashCombatRules.LARGE_ENERGY_MANA);
         arash.consumeCraftedArrows(ArashCombatRules.ENERGY_ARROW_COST);
         arash.getPersistentData().putLong(TAG_NEXT_LARGE, now + ArashCombatRules.LARGE_ENERGY_COOLDOWN);
         fireDirect(level, arash, target, ArashParticleArrowEntity.LARGE_ENERGY, ArashCombatRules.LARGE_ENERGY_DAMAGE, 2.1);
         arash.triggerNamedActionAnimation("energy_large");
         ServantVoiceHelper.tryPlayAttack(arash);
         return;
      }
      if (now >= arash.getPersistentData().getLong(TAG_NEXT_RAIN)
         && arash.getCraftedArrowCount() >= ArashCombatRules.ARROW_RAIN_COST
         && arash.getCurrentMp() >= ArashCombatRules.RAIN_MANA
         && (!lineOfSight || arash.distanceTo(target) >= 48.0F || cluster >= 3)) {
         lockAttackFacing(arash, target, now, 16);
         arash.setCurrentMp(arash.getCurrentMp() - ArashCombatRules.RAIN_MANA);
         arash.consumeCraftedArrows(ArashCombatRules.ARROW_RAIN_COST);
         arash.getPersistentData().putLong(TAG_NEXT_RAIN, now + ArashCombatRules.RAIN_COOLDOWN);
         fireRain(level, arash, target);
         arash.triggerNamedActionAnimation("arrow_rain");
         ServantVoiceHelper.tryPlayAttack(arash);
         return;
      }
      if (now >= arash.getPersistentData().getLong(TAG_NEXT_SMALL)
         && arash.getCraftedArrowCount() >= ArashCombatRules.ENERGY_ARROW_COST
         && arash.getCurrentMp() >= ArashCombatRules.SMALL_ENERGY_MANA) {
         lockAttackFacing(arash, target, now, 13);
         arash.setCurrentMp(arash.getCurrentMp() - ArashCombatRules.SMALL_ENERGY_MANA);
         arash.consumeCraftedArrows(ArashCombatRules.ENERGY_ARROW_COST);
         arash.getPersistentData().putLong(TAG_NEXT_SMALL, now + ArashCombatRules.SMALL_ENERGY_COOLDOWN);
         fireDirect(level, arash, target, ArashParticleArrowEntity.SMALL_ENERGY, ArashCombatRules.SMALL_ENERGY_DAMAGE, 2.7);
         arash.triggerNamedActionAnimation("energy_small");
         ServantVoiceHelper.tryPlayAttack(arash);
         return;
      }
      if (now >= arash.getPersistentData().getLong(TAG_NEXT_NORMAL)
         && arash.consumeCraftedArrows(ArashCombatRules.NORMAL_ARROW_COST)) {
         lockAttackFacing(arash, target, now, ArashCombatRules.NORMAL_ARROW_INTERVAL);
         arash.getPersistentData().putLong(TAG_NEXT_NORMAL, now + ArashCombatRules.NORMAL_ARROW_INTERVAL);
         fireDirect(level, arash, target, ArashParticleArrowEntity.NORMAL, ArashCombatRules.NORMAL_ARROW_DAMAGE, 3.2);
         arash.triggerNamedActionAnimation("bow_shot");
         ServantVoiceHelper.tryPlayAttack(arash);
      }
   }

   public static void tickAttackFacing(ArashEntity arash) {
      LivingEntity target = arash.getTarget();
      if (!isTarget(arash, target)
         || arash.level().getGameTime() > arash.getPersistentData().getLong(TAG_ATTACK_FACE_UNTIL)) return;
      faceAttackTarget(arash, target);
   }

   private static void lockAttackFacing(ArashEntity arash, LivingEntity target, long now, int durationTicks) {
      arash.getPersistentData().putLong(TAG_ATTACK_FACE_UNTIL, now + Math.max(1, durationTicks));
      faceAttackTarget(arash, target);
   }

   private static void faceAttackTarget(ArashEntity arash, LivingEntity target) {
      Vec3 offset = target.getEyePosition().subtract(arash.getEyePosition());
      arash.faceVector(offset);
      double horizontal = Math.max(1.0E-5, offset.horizontalDistance());
      float pitch = (float)(-Mth.atan2(offset.y, horizontal) * Mth.RAD_TO_DEG);
      pitch = Mth.clamp(pitch, -55.0F, 55.0F);
      arash.setXRot(pitch);
      arash.xRotO = pitch;
      arash.getLookControl().setLookAt(target.getX(), target.getEyeY(), target.getZ(), 180.0F, 180.0F);
   }

   private static void fireDirect(ServerLevel level, ArashEntity arash, LivingEntity target, int variant,
                                  float damage, double speed) {
      Vec3 start = arash.getEyePosition().add(arash.getLookAngle().scale(0.65));
      Vec3 direction = ArashAimHelper.leadDirection(start, target, speed);
      ArashParticleArrowEntity arrow = new ArashParticleArrowEntity(level, arash, variant, damage);
      arrow.setPos(start.x, start.y, start.z);
      arrow.setDeltaMovement(direction.scale(speed));
      level.addFreshEntity(arrow);
   }

   private static void fireRain(ServerLevel level, ArashEntity arash, LivingEntity target) {
      Vec3 start = arash.getEyePosition().add(0.0, 0.35, 0.0);
      for (int i = 0; i < ArashCombatRules.RAIN_ARROW_COUNT; i++) {
         double initialHorizontal = target.position().subtract(start).horizontalDistance();
         double flightTicks = Math.max(12.0, initialHorizontal / 2.65);
         Vec3 predicted = target.position().add(ArashAimHelper.predictionOffset(target.getDeltaMovement(), flightTicks))
            .add((arash.getRandom().nextDouble() - 0.5) * 5.0, target.getBbHeight() * 0.5,
               (arash.getRandom().nextDouble() - 0.5) * 5.0);
         Vec3 delta = predicted.subtract(start);
         double horizontal = Math.max(1.0, Math.sqrt(delta.x * delta.x + delta.z * delta.z));
         double ticks = Math.max(12.0, horizontal / 2.65);
         double vy = delta.y / ticks + 0.015 * ticks;
         ArashParticleArrowEntity arrow = new ArashParticleArrowEntity(level, arash,
            ArashParticleArrowEntity.RAIN, ArashCombatRules.RAIN_ARROW_DAMAGE);
         arrow.setPos(start.x, start.y, start.z);
         arrow.setDeltaMovement(delta.x / ticks, vy, delta.z / ticks);
         level.addFreshEntity(arrow);
      }
   }

   private static void tacticalMovement(ArashEntity arash, LivingEntity target, long now, boolean lineOfSight) {
      var data = arash.getPersistentData();
      int orbitDirection = data.getInt(TAG_ORBIT_DIRECTION);
      if (orbitDirection == 0) {
         orbitDirection = arash.getRandom().nextBoolean() ? 1 : -1;
         data.putInt(TAG_ORBIT_DIRECTION, orbitDirection);
         data.putLong(TAG_NEXT_ORBIT_SWITCH, now + 100L + arash.getRandom().nextInt(61));
      } else if (now >= data.getLong(TAG_NEXT_ORBIT_SWITCH)) {
         orbitDirection = -orbitDirection;
         data.putInt(TAG_ORBIT_DIRECTION, orbitDirection);
         data.putLong(TAG_NEXT_ORBIT_SWITCH, now + 100L + arash.getRandom().nextInt(61));
      }

      int stationaryTicks = sampleStationaryTicks(arash, now);
      double distance = arash.distanceTo(target);
      boolean meleePressureWindow = ServantEngagementService.role(target) == ServantEngagementService.CombatRole.MELEE
         && distance <= 18.0 && Math.floorMod(now, 100L) >= 70L;
      ServantEngagementService.RangeBand band = ServantEngagementService.rangedBand(
         target,
         ArashCombatRules.CROSSOVER_TRIGGER_RANGE,
         ArashCombatRules.PREFERRED_COMBAT_RANGE,
         ArashCombatRules.APPROACH_THRESHOLD
      );
      if (arash.getPersistentData().getLong(TAG_CROSSOVER_UNTIL) > now) return;
      if (meleePressureWindow) {
         arash.getNavigation().stop();
         arash.getMoveControl().strafe(distance > 8.0 ? 0.24F : 0.0F, orbitDirection * 0.32F);
         arash.getLookControl().setLookAt(target, 40.0F, 40.0F);
         return;
      }
      boolean crowded = countCluster(arash, arash, 5.5) >= 2;
      boolean shouldCross = distance <= band.minimum()
         || stationaryTicks >= 30 && distance <= 18.0 || crowded && distance <= 11.0;
      if (shouldCross && now >= data.getLong(TAG_NEXT_CROSSOVER)
         && beginCrossover(arash, target, now, orbitDirection)) {
         data.putInt(TAG_STATIONARY_TICKS, 0);
         return;
      }

      long lastReposition = data.getLong(TAG_LAST_REPOSITION);
      if (now - lastReposition < ArashCombatRules.TACTICAL_REPATH_INTERVAL) {
         if (arash.getNavigation().isDone()) applyMobileStrafe(arash, target, distance, orbitDirection, band);
         return;
      }
      data.putLong(TAG_LAST_REPOSITION, now);

      Vec3 destination = findTacticalPosition(arash, target, lineOfSight, orbitDirection, band);
      double speed = distance > band.maximum() ? 1.25 : lineOfSight ? 1.10 : 1.20;
      if (destination == null || !arash.getNavigation().moveTo(destination.x, destination.y, destination.z, speed)) {
         orbitDirection = -orbitDirection;
         data.putInt(TAG_ORBIT_DIRECTION, orbitDirection);
         applyMobileStrafe(arash, target, distance, orbitDirection, band);
      }
   }

   private static boolean beginCrossover(ArashEntity arash, LivingEntity target, long now, int orbitDirection) {
      Vec3 toward = target.position().subtract(arash.position()).multiply(1.0, 0.0, 1.0);
      if (toward.lengthSqr() < 1.0E-5) return false;
      toward = toward.normalize();
      Vec3 side = new Vec3(-toward.z, 0.0, toward.x).scale(orbitDirection);
      Vec3 desired = target.position().add(toward.scale(9.0)).add(side.scale(3.5));
      BlockPos stand = resolveStand(arash, desired);
      if (stand == null) {
         desired = target.position().add(toward.scale(7.0)).add(side.scale(-3.5));
         stand = resolveStand(arash, desired);
      }
      if (stand != null) {
         Vec3 destination = Vec3.atBottomCenterOf(stand);
         arash.getNavigation().moveTo(destination.x, destination.y, destination.z, 1.42);
      } else {
         arash.getNavigation().stop();
      }
      if (arash.onGround()) arash.jumpFromGround();
      Vec3 motion = arash.getDeltaMovement();
      arash.setDeltaMovement(toward.x * 0.72 + side.x * 0.24, Math.max(0.34, motion.y),
         toward.z * 0.72 + side.z * 0.24);
      int cooldown = ServantEngagementService.role(target) == ServantEngagementService.CombatRole.MELEE
         ? ArashCombatRules.CROSSOVER_COOLDOWN + 40 : ArashCombatRules.CROSSOVER_COOLDOWN;
      arash.getPersistentData().putLong(TAG_NEXT_CROSSOVER, now + cooldown);
      arash.getPersistentData().putInt(TAG_ORBIT_DIRECTION, -orbitDirection);
      arash.getPersistentData().putLong(TAG_CROSSOVER_UNTIL, now + 18L);
      arash.getPersistentData().putDouble(TAG_CROSSOVER_AXIS_X, toward.x);
      arash.getPersistentData().putDouble(TAG_CROSSOVER_AXIS_Z, toward.z);
      arash.getPersistentData().putInt(TAG_CROSSOVER_SIDE, orbitDirection);
      if (arash.level() instanceof ServerLevel level) {
         level.sendParticles(ParticleTypes.CLOUD, arash.getX(), arash.getY() + 0.15, arash.getZ(),
            14, 0.45, 0.12, 0.45, 0.08);
         level.sendParticles(ParticleTypes.CRIT, arash.getX(), arash.getY() + 0.9, arash.getZ(),
            10, 0.35, 0.55, 0.35, 0.16);
      }
      return true;
   }

   public static void tickCrossoverMovement(ArashEntity arash) {
      if (arash.level().isClientSide || !arash.isAlive()) return;
      LivingEntity target = arash.getTarget();
      long now = arash.level().getGameTime();
      if (!isTarget(arash, target)) {
         arash.getPersistentData().remove(TAG_CROSSOVER_UNTIL);
         return;
      }
      continueCrossover(arash, target, now);
   }

   private static boolean continueCrossover(ArashEntity arash, LivingEntity target, long now) {
      var data = arash.getPersistentData();
      long until = data.getLong(TAG_CROSSOVER_UNTIL);
      if (until <= now) {
         data.remove(TAG_CROSSOVER_UNTIL);
         return false;
      }
      Vec3 axis = new Vec3(data.getDouble(TAG_CROSSOVER_AXIS_X), 0.0, data.getDouble(TAG_CROSSOVER_AXIS_Z));
      if (axis.lengthSqr() < 0.5) {
         data.remove(TAG_CROSSOVER_UNTIL);
         return false;
      }
      axis = axis.normalize();
      double crossedBy = arash.position().subtract(target.position()).dot(axis);
      Vec3 side = new Vec3(-axis.z, 0.0, axis.x).scale(data.getInt(TAG_CROSSOVER_SIDE));
      Vec3 motion = arash.getDeltaMovement();
      double forwardStep = crossedBy > 1.5 ? 0.16 : 0.38;
      double forwardMotion = crossedBy > 1.5 ? 0.32 : 0.72;
      arash.move(MoverType.SELF, axis.scale(forwardStep).add(side.scale(0.16)));
      arash.setDeltaMovement(axis.x * forwardMotion + side.x * 0.24,
         Math.max(arash.onGround() ? 0.24 : motion.y, motion.y),
         axis.z * forwardMotion + side.z * 0.24);
      arash.getLookControl().setLookAt(target, 40.0F, 40.0F);
      return true;
   }

   private static Vec3 findTacticalPosition(ArashEntity arash, LivingEntity target, boolean lineOfSight,
                                            int orbitDirection, ServantEngagementService.RangeBand band) {
      double distance = arash.distanceTo(target);
      double desiredRange = distance > band.maximum()
         ? Math.min(band.maximum() - 4.0, band.preferred() + 10.0)
         : !lineOfSight ? Math.max(16.0, band.preferred() - 8.0) : band.preferred();
      Vec3 radial = arash.position().subtract(target.position()).multiply(1.0, 0.0, 1.0);
      if (radial.lengthSqr() < 1.0E-5) radial = new Vec3(1.0, 0.0, 0.0);
      radial = radial.normalize();
      double[] angles = distance > band.maximum()
         ? new double[]{0.0, 24.0, -24.0, 48.0, -48.0, 78.0, -78.0}
         : new double[]{28.0, 46.0, 68.0, 92.0, 122.0, -38.0, -72.0};

      Vec3 best = null;
      double bestScore = -Double.MAX_VALUE;
      for (double rawAngle : angles) {
         double radians = Math.toRadians(rawAngle * orbitDirection);
         Vec3 direction = rotateHorizontal(radial, radians);
         double radius = desiredRange + (Math.abs(rawAngle) % 3.0 - 1.0) * 2.0;
         BlockPos stand = resolveStand(arash, target.position().add(direction.scale(radius)));
         if (stand == null) continue;
         Vec3 candidate = Vec3.atBottomCenterOf(stand);
         double score = scoreTacticalPosition(arash, target, candidate, desiredRange, lineOfSight, rawAngle);
         if (score > bestScore) {
            bestScore = score;
            best = candidate;
         }
      }
      return best;
   }

   private static double scoreTacticalPosition(ArashEntity arash, LivingEntity target, Vec3 candidate,
                                               double desiredRange, boolean currentlyVisible, double angle) {
      Vec3 eye = candidate.add(0.0, arash.getEyeHeight(), 0.0);
      HitResult clip = arash.level().clip(new ClipContext(eye, target.getEyePosition(), ClipContext.Block.COLLIDER,
         ClipContext.Fluid.NONE, arash));
      boolean clearShot = clip.getType() == HitResult.Type.MISS;
      double targetDistance = candidate.distanceTo(target.position());
      double movement = candidate.distanceTo(arash.position());
      int nearbyThreats = arash.level().getEntitiesOfClass(LivingEntity.class,
         new AABB(candidate, candidate).inflate(5.0), living -> isTarget(arash, living)).size();
      double score = clearShot ? 90.0 : -45.0;
      score -= Math.abs(targetDistance - desiredRange) * 3.0;
      score += Math.max(-4.0, Math.min(8.0, candidate.y - target.getY())) * 5.0;
      score += Math.min(16.0, movement) * 1.15;
      score -= Math.max(0.0, movement - 30.0) * 0.35;
      score -= nearbyThreats * 13.0;
      if (!currentlyVisible && clearShot) score += 35.0;
      if (Math.abs(angle) >= 45.0) score += 8.0;
      return score;
   }

   private static BlockPos resolveStand(ArashEntity arash, Vec3 desired) {
      BlockPos column = BlockPos.containing(desired);
      BlockPos stand = arash.level().getHeightmapPos(
         net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column);
      BlockState below = arash.level().getBlockState(stand.below());
      if (!arash.level().getWorldBorder().isWithinBounds(stand)
         || !below.isCollisionShapeFullBlock(arash.level(), stand.below())
         || !arash.level().getBlockState(stand).isAir() || !arash.level().getBlockState(stand.above()).isAir()) {
         return null;
      }
      Vec3 destination = Vec3.atBottomCenterOf(stand);
      if (!arash.level().noCollision(arash, arash.getBoundingBox().move(destination.subtract(arash.position())))) return null;
      return stand;
   }

   private static Vec3 rotateHorizontal(Vec3 vector, double radians) {
      double cosine = Math.cos(radians), sine = Math.sin(radians);
      return new Vec3(vector.x * cosine - vector.z * sine, 0.0, vector.x * sine + vector.z * cosine);
   }

   private static void applyMobileStrafe(ArashEntity arash, LivingEntity target, double distance, int orbitDirection,
                                         ServantEngagementService.RangeBand band) {
      float forward = distance > band.preferred() + 7.0 ? 0.42F
         : distance < band.preferred() - 8.0 ? -0.28F : 0.08F;
      arash.getMoveControl().strafe(forward, orbitDirection * 0.68F);
      arash.getLookControl().setLookAt(target, 40.0F, 40.0F);
   }

   private static int sampleStationaryTicks(ArashEntity arash, long now) {
      var data = arash.getPersistentData();
      long lastSample = data.getLong(TAG_MOTION_SAMPLE);
      if (!data.contains(TAG_MOTION_X) || now - lastSample >= 10L) {
         int stationary = data.getInt(TAG_STATIONARY_TICKS);
         if (data.contains(TAG_MOTION_X)) {
            double dx = arash.getX() - data.getDouble(TAG_MOTION_X);
            double dz = arash.getZ() - data.getDouble(TAG_MOTION_Z);
            stationary = dx * dx + dz * dz < 0.09 ? stationary + (int)Math.max(1L, now - lastSample) : 0;
         }
         data.putLong(TAG_MOTION_SAMPLE, now);
         data.putDouble(TAG_MOTION_X, arash.getX());
         data.putDouble(TAG_MOTION_Z, arash.getZ());
         data.putInt(TAG_STATIONARY_TICKS, Math.min(100, stationary));
      }
      return data.getInt(TAG_STATIONARY_TICKS);
   }

   private static int countCluster(ArashEntity arash, LivingEntity target, double radius) {
      return arash.level().getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(radius),
         living -> isTarget(arash, living)).size();
   }

   public static boolean isHighThreat(LivingEntity target) {
      return target instanceof ServantEntity || target instanceof EnderDragon || target instanceof WitherBoss
         || target.getMaxHealth() >= 200.0F || target.getAttributeValue(Attributes.ATTACK_DAMAGE) >= 20.0;
   }

   public static boolean isTarget(ArashEntity arash, LivingEntity target) {
      if (target == null || !EntityUtils.isValidCombatTarget(arash, target)) return false;
      if (ServantMasterTargeting.isContractMaster(arash, target)) return false;
      return target instanceof Enemy || target instanceof ServantEntity
         || target.getLastHurtMob() == arash || arash.getLastHurtByMob() == target;
   }
}
