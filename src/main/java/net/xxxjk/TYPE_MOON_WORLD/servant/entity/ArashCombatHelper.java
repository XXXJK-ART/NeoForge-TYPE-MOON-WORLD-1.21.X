package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.entity.ArashParticleArrowEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.ArashStellaControllerEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantAiContext;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;

public final class ArashCombatHelper {
   private static final String TAG_ARROWS = "ArashVirtualArrows";
   private static final String TAG_NEXT_NORMAL = "ArashNextNormalArrow";
   private static final String TAG_NEXT_RAIN = "ArashNextArrowRain";
   private static final String TAG_NEXT_SMALL = "ArashNextSmallEnergyArrow";
   private static final String TAG_NEXT_LARGE = "ArashNextLargeEnergyArrow";
   private static final String TAG_LAST_SCAN = "ArashLastTargetScan";
   private static final String TAG_LAST_REPOSITION = "ArashLastReposition";
   private static final String TAG_NEXT_CROSSOVER = "ArashNextCrossover";
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
         && arash.getCurrentMp() >= ArashCombatRules.LARGE_ENERGY_MANA
         && (target.getMaxHealth() >= 160.0F || cluster >= 4)) {
         arash.setCurrentMp(arash.getCurrentMp() - ArashCombatRules.LARGE_ENERGY_MANA);
         arash.getPersistentData().putLong(TAG_NEXT_LARGE, now + ArashCombatRules.LARGE_ENERGY_COOLDOWN);
         fireDirect(level, arash, target, ArashParticleArrowEntity.LARGE_ENERGY, ArashCombatRules.LARGE_ENERGY_DAMAGE, 2.1);
         arash.triggerNamedActionAnimation("energy_large");
         ServantVoiceHelper.tryPlayAttack(arash);
         return;
      }
      if (now >= arash.getPersistentData().getLong(TAG_NEXT_RAIN)
         && arash.getCurrentMp() >= ArashCombatRules.RAIN_MANA
         && (!lineOfSight || arash.distanceTo(target) >= 48.0F || cluster >= 3)) {
         arash.setCurrentMp(arash.getCurrentMp() - ArashCombatRules.RAIN_MANA);
         arash.getPersistentData().putLong(TAG_NEXT_RAIN, now + ArashCombatRules.RAIN_COOLDOWN);
         fireRain(level, arash, target);
         arash.triggerNamedActionAnimation("arrow_rain");
         ServantVoiceHelper.tryPlayAttack(arash);
         return;
      }
      if (now >= arash.getPersistentData().getLong(TAG_NEXT_SMALL)
         && arash.getCurrentMp() >= ArashCombatRules.SMALL_ENERGY_MANA) {
         arash.setCurrentMp(arash.getCurrentMp() - ArashCombatRules.SMALL_ENERGY_MANA);
         arash.getPersistentData().putLong(TAG_NEXT_SMALL, now + ArashCombatRules.SMALL_ENERGY_COOLDOWN);
         fireDirect(level, arash, target, ArashParticleArrowEntity.SMALL_ENERGY, ArashCombatRules.SMALL_ENERGY_DAMAGE, 2.7);
         arash.triggerNamedActionAnimation("energy_small");
         ServantVoiceHelper.tryPlayAttack(arash);
         return;
      }
      if (now >= arash.getPersistentData().getLong(TAG_NEXT_NORMAL) && consumeArrow(arash)) {
         arash.getPersistentData().putLong(TAG_NEXT_NORMAL, now + ArashCombatRules.NORMAL_ARROW_INTERVAL);
         fireDirect(level, arash, target, ArashParticleArrowEntity.NORMAL, ArashCombatRules.NORMAL_ARROW_DAMAGE, 3.2);
         arash.triggerNamedActionAnimation("bow_shot");
         ServantVoiceHelper.tryPlayAttack(arash);
      }
   }

   private static boolean consumeArrow(ArashEntity arash) {
      var data = arash.getPersistentData();
      if (!data.contains(TAG_ARROWS)) data.putInt(TAG_ARROWS, ArashCombatRules.ARROW_CAPACITY);
      int arrows = data.getInt(TAG_ARROWS);
      if (arrows <= 0) {
         if (arash.getCurrentMp() < ArashCombatRules.ARROW_REFILL_MANA) return false;
         arash.setCurrentMp(arash.getCurrentMp() - ArashCombatRules.ARROW_REFILL_MANA);
         arrows = ArashCombatRules.ARROW_CAPACITY;
      }
      data.putInt(TAG_ARROWS, arrows - 1);
      return true;
   }

   private static void fireDirect(ServerLevel level, ArashEntity arash, LivingEntity target, int variant,
                                  float damage, double speed) {
      Vec3 start = arash.getEyePosition().add(arash.getLookAngle().scale(0.65));
      double distance = Math.max(1.0, start.distanceTo(target.getEyePosition()));
      Vec3 predicted = target.getEyePosition().add(target.getDeltaMovement().scale(distance / speed));
      Vec3 direction = predicted.subtract(start).normalize();
      ArashParticleArrowEntity arrow = new ArashParticleArrowEntity(level, arash, variant, damage);
      arrow.setPos(start.x, start.y, start.z);
      arrow.setDeltaMovement(direction.scale(speed));
      level.addFreshEntity(arrow);
   }

   private static void fireRain(ServerLevel level, ArashEntity arash, LivingEntity target) {
      Vec3 start = arash.getEyePosition().add(0.0, 0.35, 0.0);
      for (int i = 0; i < ArashCombatRules.RAIN_ARROW_COUNT; i++) {
         Vec3 predicted = target.position().add(target.getDeltaMovement().scale(18.0))
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
      boolean crowded = countCluster(arash, arash, 5.5) >= 2;
      boolean shouldCross = distance <= ArashCombatRules.CROSSOVER_TRIGGER_RANGE
         || stationaryTicks >= 30 && distance <= 18.0 || crowded && distance <= 11.0;
      if (shouldCross && now >= data.getLong(TAG_NEXT_CROSSOVER)
         && beginCrossover(arash, target, now, orbitDirection)) {
         data.putInt(TAG_STATIONARY_TICKS, 0);
         return;
      }

      long lastReposition = data.getLong(TAG_LAST_REPOSITION);
      if (now - lastReposition < ArashCombatRules.TACTICAL_REPATH_INTERVAL) {
         if (arash.getNavigation().isDone()) applyMobileStrafe(arash, target, distance, orbitDirection);
         return;
      }
      data.putLong(TAG_LAST_REPOSITION, now);

      Vec3 destination = findTacticalPosition(arash, target, lineOfSight, orbitDirection);
      double speed = distance > ArashCombatRules.APPROACH_THRESHOLD ? 1.25 : lineOfSight ? 1.10 : 1.20;
      if (destination == null || !arash.getNavigation().moveTo(destination.x, destination.y, destination.z, speed)) {
         orbitDirection = -orbitDirection;
         data.putInt(TAG_ORBIT_DIRECTION, orbitDirection);
         applyMobileStrafe(arash, target, distance, orbitDirection);
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
      if (stand == null) return false;

      Vec3 destination = Vec3.atBottomCenterOf(stand);
      arash.getNavigation().moveTo(destination.x, destination.y, destination.z, 1.42);
      if (arash.onGround()) arash.jumpFromGround();
      Vec3 motion = arash.getDeltaMovement();
      arash.setDeltaMovement(toward.x * 0.72 + side.x * 0.24, Math.max(0.34, motion.y),
         toward.z * 0.72 + side.z * 0.24);
      arash.getPersistentData().putLong(TAG_NEXT_CROSSOVER, now + ArashCombatRules.CROSSOVER_COOLDOWN);
      arash.getPersistentData().putInt(TAG_ORBIT_DIRECTION, -orbitDirection);
      if (arash.level() instanceof ServerLevel level) {
         level.sendParticles(ParticleTypes.CLOUD, arash.getX(), arash.getY() + 0.15, arash.getZ(),
            14, 0.45, 0.12, 0.45, 0.08);
         level.sendParticles(ParticleTypes.CRIT, arash.getX(), arash.getY() + 0.9, arash.getZ(),
            10, 0.35, 0.55, 0.35, 0.16);
      }
      return true;
   }

   private static Vec3 findTacticalPosition(ArashEntity arash, LivingEntity target, boolean lineOfSight,
                                            int orbitDirection) {
      double distance = arash.distanceTo(target);
      double desiredRange = distance > ArashCombatRules.APPROACH_THRESHOLD
         ? 42.0 : !lineOfSight ? 24.0 : ArashCombatRules.PREFERRED_COMBAT_RANGE;
      Vec3 radial = arash.position().subtract(target.position()).multiply(1.0, 0.0, 1.0);
      if (radial.lengthSqr() < 1.0E-5) radial = new Vec3(1.0, 0.0, 0.0);
      radial = radial.normalize();
      double[] angles = distance > ArashCombatRules.APPROACH_THRESHOLD
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

   private static void applyMobileStrafe(ArashEntity arash, LivingEntity target, double distance, int orbitDirection) {
      float forward = distance > ArashCombatRules.PREFERRED_COMBAT_RANGE + 7.0 ? 0.42F
         : distance < ArashCombatRules.PREFERRED_COMBAT_RANGE - 8.0 ? -0.28F : 0.08F;
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
      return target instanceof Enemy || target instanceof ServantEntity
         || target.getLastHurtMob() == arash || arash.getLastHurtByMob() == target;
   }
}
