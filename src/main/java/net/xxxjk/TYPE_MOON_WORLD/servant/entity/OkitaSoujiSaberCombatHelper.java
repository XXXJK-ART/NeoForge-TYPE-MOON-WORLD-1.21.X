package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.combat.ai.EvasionMovementService;
import net.xxxjk.TYPE_MOON_WORLD.entity.OkitaShinsengumiEntity;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantAiContext;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantNavigationHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatSystem;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import org.joml.Vector3f;

public final class OkitaSoujiSaberCombatHelper {
   private static final String TAG_FLAG_POOL = "OkitaFlagPool";
   private static final String TAG_FLAG_POOL_INIT = "OkitaFlagPoolInitialized";
   private static final String TAG_HAORI_INIT = "OkitaHaoriInitialized";
   private static final String TAG_SHUKUCHI_COOLDOWN = "OkitaShukuchiCooldown";
   private static final String TAG_SHUKUCHI_UNTIL = "OkitaShukuchiUntil";
   private static final String TAG_MUMYOUDAN_COOLDOWN = "OkitaMumyoudanCooldown";
   private static final String TAG_WEAK_UNTIL = "OkitaWeakConstitutionUntil";
   private static final String TAG_FLAG_COOLDOWN = "OkitaFlagCooldown";
   private static final String TAG_LAST_MIND_EYE = "OkitaLastMindEye";
   private static final String TAG_TARGET_UUID = "OkitaCurrentTarget";
   private static final String TAG_TARGET_ACQUIRED = "OkitaTargetAcquiredTick";
   private static final String TAG_LAST_PRESSURE_REPOSITION = "OkitaLastPressureReposition";
   private static final String SHINSENGUMI_OWNER_TAG = "OkitaShinsengumiOwner";
   private static final int FLAG_POOL_MAX = 8;
   private static final int SHUKUCHI_COOLDOWN = 240;
   private static final int SHUKUCHI_DURATION = 200;
   private static final double SHUKUCHI_MP_COST = 5.0;
   private static final int MUMYOUDAN_COOLDOWN = 300;
   private static final double MUMYOUDAN_MP_COST = 10.0;
   private static final int WEAK_DURATION = 300;
   private static final int FLAG_COOLDOWN = 1800;
   private static final double FLAG_MP_COST = 80.0;
   private static final int FLAG_DURATION = 2400;
   private static final ResourceLocation HAORI_HEALTH_ID = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "okita_haori_health");
   private static final ResourceLocation HAORI_ATTACK_ID = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "okita_haori_attack");
   private static final ResourceLocation HAORI_SPEED_ID = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "okita_haori_speed");
   private static final ResourceLocation HAORI_ATTACK_SPEED_ID = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "okita_haori_attack_speed");
   private static final ResourceLocation SHUKUCHI_SPEED_ID = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "okita_shukuchi_speed");
   private static final ResourceLocation SHUKUCHI_ATTACK_SPEED_ID = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "okita_shukuchi_attack_speed");
   private static final ResourceLocation WEAK_ATTACK_ID = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "okita_weak_attack");
   private static final ResourceLocation WEAK_SPEED_ID = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "okita_weak_speed");
   private static final DustParticleOptions ASAGI = new DustParticleOptions(new Vector3f(0.25F, 0.82F, 0.95F), 1.1F);

   private OkitaSoujiSaberCombatHelper() {
   }

   public static void tick(OkitaSoujiSaberEntity entity, ServantAiContext context) {
      if (!(entity.level() instanceof ServerLevel level)) {
         return;
      }
      tickPersistentState(entity);
      LivingEntity target = context.target();
      if (!isValidTarget(entity, target)) {
         target = entity.getTarget();
      }
      if (!isValidTarget(entity, target)) {
         entity.setTarget(null);
         return;
      }

      entity.setTarget(target);
      entity.getLookControl().setLookAt(target, 60.0F, 60.0F);
      long now = level.getGameTime();
      double distance = entity.distanceTo(target);
      long targetAge = rememberTarget(entity, target, now);
      int nearbyEnemies = countNearbyEnemies(entity, level);
      boolean emergency = entity.getHealth() <= entity.getMaxHealth() * 0.30F;

      if (isWeakConstitutionActive(entity)) {
         if (tryFlagOfSincerity(entity, target, level, now, nearbyEnemies, emergency)) {
            return;
         }
         if (distance < 5.5 && tryMindEye(entity, target, now, true)) {
            return;
         }
         retreatFrom(entity, target, distance < 7.0 ? 1.35 : 1.15);
         return;
      }
      if (ServantCombatSystem.cannotAct(entity) || ServantCombatSystem.skillsSuppressed(entity)) {
         return;
      }
      if (distance <= 1.9 && nearbyEnemies >= 2 && tryMindEye(entity, target, now, true)) {
         return;
      }
      if (tryFlagOfSincerity(entity, target, level, now, nearbyEnemies, emergency)) {
         return;
      }
      boolean shouldUseMumyoudan = shouldUseMumyoudanZuki(entity, target, distance, targetAge, nearbyEnemies, now);
      if (shouldUseMumyoudan && distance > 2.8 && distance <= 5.5 && tryShukuchi(entity, target, level, now)) {
         return;
      }
      if (distance > 2.6 && distance <= 5.5 && shouldUseShukuchi(entity, target, targetAge, nearbyEnemies)
         && tryShukuchi(entity, target, level, now)) {
         return;
      }
      if (tryMindEye(entity, target, now, false)) {
         return;
      }
      if (distance <= 3.2 && shouldUseMumyoudan && tryMumyoudanZuki(entity, target, level, now)) {
         return;
      }
      if (distance <= 2.1 && tryPressureReposition(entity, target, now)) {
         return;
      }
      if (distance > 2.4) {
         ServantNavigationHelper.moveToTargetThrottled(entity, target, isShukuchiActive(entity) ? 1.55 : 1.25,
            now, ServantNavigationHelper.SHORT_REPATH_INTERVAL, 0.6, "OkitaChasePath");
      } else if (!entity.isPerformingAction()
         && now - entity.getPersistentData().getLong("OkitaLastBasicAttack") >= basicAttackInterval(entity, nearbyEnemies, emergency)) {
         entity.getPersistentData().putLong("OkitaLastBasicAttack", now);
         entity.faceToward(target.position());
         entity.doHurtTarget(target);
      }
   }

   public static void tickPersistentState(OkitaSoujiSaberEntity entity) {
      if (!(entity.level() instanceof ServerLevel level) || !entity.isAlive()) {
         return;
      }
      initializeFlagPool(entity);
      applyHaori(entity);
      long now = level.getGameTime();
      boolean shukuchi = isShukuchiActive(entity);
      updateModifier(entity.getAttribute(Attributes.MOVEMENT_SPEED), SHUKUCHI_SPEED_ID,
         shukuchi ? 0.50 : 0.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
      updateModifier(entity.getAttribute(Attributes.ATTACK_SPEED), SHUKUCHI_ATTACK_SPEED_ID,
         shukuchi ? 0.40 : 0.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
      boolean weak = isWeakConstitutionActive(entity);
      updateModifier(entity.getAttribute(Attributes.ATTACK_DAMAGE), WEAK_ATTACK_ID,
         weak ? -0.30 : 0.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
      updateModifier(entity.getAttribute(Attributes.MOVEMENT_SPEED), WEAK_SPEED_ID,
         weak ? -0.30 : 0.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
      if (weak && entity.getTarget() != null && now % 5L == 0L) {
         retreatFrom(entity, entity.getTarget(), 1.15);
      }
   }

   public static void initializeFlagPool(OkitaSoujiSaberEntity entity) {
      CompoundTag data = entity.getPersistentData();
      if (!data.getBoolean(TAG_FLAG_POOL_INIT)) {
         data.putBoolean(TAG_FLAG_POOL_INIT, true);
         data.putInt(TAG_FLAG_POOL, FLAG_POOL_MAX);
      } else if (!data.contains(TAG_FLAG_POOL)) {
         data.putInt(TAG_FLAG_POOL, FLAG_POOL_MAX);
      }
   }

   public static boolean isWeakConstitutionActive(OkitaSoujiSaberEntity entity) {
      return entity.getPersistentData().getLong(TAG_WEAK_UNTIL) > entity.level().getGameTime();
   }

   public static void decrementFlagPool(OkitaSoujiSaberEntity entity) {
      initializeFlagPool(entity);
      CompoundTag data = entity.getPersistentData();
      data.putInt(TAG_FLAG_POOL, Math.max(0, data.getInt(TAG_FLAG_POOL) - 1));
   }

   private static boolean tryShukuchi(OkitaSoujiSaberEntity entity, LivingEntity target, ServerLevel level, long now) {
      CompoundTag data = entity.getPersistentData();
      if (entity.isPerformingAction() || entity.getCurrentMp() < SHUKUCHI_MP_COST || now < data.getLong(TAG_SHUKUCHI_COOLDOWN)
         || !entity.getSensing().hasLineOfSight(target)) {
         return false;
      }
      Vec3 destination = findShukuchiDestination(entity, target);
      if (destination == null) {
         return false;
      }
      entity.setCurrentMp(Math.max(0.0, entity.getCurrentMp() - SHUKUCHI_MP_COST));
      data.putLong(TAG_SHUKUCHI_COOLDOWN, now + SHUKUCHI_COOLDOWN);
      data.putLong(TAG_SHUKUCHI_UNTIL, now + SHUKUCHI_DURATION);
      Vec3 start = entity.position();
      entity.triggerTeleportAnimation();
      entity.teleportTo(destination.x, destination.y, destination.z);
      entity.faceToward(target.position());
      level.sendParticles(ASAGI, start.x, start.y + 0.45, start.z, 16, 0.25, 0.35, 0.25, 0.03);
      level.sendParticles(ASAGI, destination.x, destination.y + 0.45, destination.z, 20, 0.25, 0.35, 0.25, 0.03);
      level.playSound(null, entity.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 0.85F, 1.75F);
      return true;
   }

   private static boolean tryMumyoudanZuki(OkitaSoujiSaberEntity entity, LivingEntity target, ServerLevel level, long now) {
      CompoundTag data = entity.getPersistentData();
      if (!entity.canUseMumyoudanZuki() || entity.isPerformingAction() || entity.getCurrentMp() < MUMYOUDAN_MP_COST
         || now < data.getLong(TAG_MUMYOUDAN_COOLDOWN) || !entity.hasMasterNoblePhantasmPermission()
         || !entity.getSensing().hasLineOfSight(target)) {
         return false;
      }
      entity.setCurrentMp(Math.max(0.0, entity.getCurrentMp() - MUMYOUDAN_MP_COST));
      data.putLong(TAG_MUMYOUDAN_COOLDOWN, now + MUMYOUDAN_COOLDOWN);
      entity.faceToward(target.position());
      entity.triggerSlashAnimation();
      ServantVoiceHelper.tryPlayOkitaSoujiSaberNp(entity);

      float damage = (float)(entity.getAttributeValue(Attributes.ATTACK_DAMAGE) * 3.0);
      for (int i = 0; i < 3 && target.isAlive(); i++) {
         target.stopUsingItem();
         target.removeEffect(MobEffects.DAMAGE_RESISTANCE);
         target.removeEffect(MobEffects.ABSORPTION);
         target.invulnerableTime = 0;
         target.hurt(entity.damageSources().source(OkitaSoujiSaberDamageTypes.MUMYOUDAN_ZUKI, entity), damage);
         target.invulnerableTime = 0;
      }
      target.getPersistentData().putLong("OkitaMumyoudanDefenseDisabledUntil", now + 100L);
      level.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY() + target.getBbHeight() * 0.55, target.getZ(), 3, 0.0, 0.0, 0.0, 0.0);
      level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + target.getBbHeight() * 0.55, target.getZ(), 32, 0.25, 0.35, 0.25, 0.08);
      level.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.HOSTILE, 1.25F, 1.55F);
      if (entity.getRandom().nextFloat() < 0.30F) {
         triggerWeakConstitution(entity, now);
      }
      return true;
   }

   private static boolean tryFlagOfSincerity(OkitaSoujiSaberEntity entity, LivingEntity target, ServerLevel level, long now,
                                             int enemies, boolean emergency) {
      CompoundTag data = entity.getPersistentData();
      initializeFlagPool(entity);
      if (entity.isPerformingAction() || ServantCombatSystem.cannotAct(entity) || data.getInt(TAG_FLAG_POOL) <= 0
         || entity.getCurrentMp() < FLAG_MP_COST || now < data.getLong(TAG_FLAG_COOLDOWN)
         || !entity.hasMasterNoblePhantasmPermission()) {
         return false;
      }
      if (enemies < 3 && !emergency) {
         return false;
      }
      int activeSoldiers = countActiveShinsengumi(level, entity);
      int count = Math.min(Math.min(FLAG_POOL_MAX, data.getInt(TAG_FLAG_POOL)), Math.max(0, FLAG_POOL_MAX - activeSoldiers));
      if (count <= 0) {
         return false;
      }
      int summoned = 0;
      for (int i = 0; i < count; i++) {
         OkitaShinsengumiEntity soldier = ModEntities.OKITA_SHINSENGUMI.get().create(level);
         if (soldier == null) {
            continue;
         }
         Vec3 pos = findSummonPosition(level, entity, i, count);
         soldier.moveTo(pos.x, pos.y, pos.z, entity.getYRot(), 0.0F);
         soldier.initializeForOkita(entity, target, now + FLAG_DURATION);
         if (level.addFreshEntity(soldier)) {
            summoned++;
         }
      }
      if (summoned <= 0) {
         return false;
      }
      entity.setCurrentMp(Math.max(0.0, entity.getCurrentMp() - FLAG_MP_COST));
      data.putLong(TAG_FLAG_COOLDOWN, now + FLAG_COOLDOWN);
      entity.triggerNamedActionAnimation("np");
      ServantVoiceHelper.tryPlayOkitaSoujiSaberNp(entity);
      level.sendParticles(ASAGI, entity.getX(), entity.getY() + 1.1, entity.getZ(), 80, 1.8, 1.0, 1.8, 0.05);
      level.playSound(null, entity.blockPosition(), SoundEvents.RAID_HORN.value(), SoundSource.HOSTILE, 1.1F, 1.35F);
      return true;
   }

   private static void applyHaori(OkitaSoujiSaberEntity entity) {
      updateModifier(entity.getAttribute(Attributes.MAX_HEALTH), HAORI_HEALTH_ID, 80.0, AttributeModifier.Operation.ADD_VALUE);
      updateModifier(entity.getAttribute(Attributes.ATTACK_DAMAGE), HAORI_ATTACK_ID, 5.0, AttributeModifier.Operation.ADD_VALUE);
      updateModifier(entity.getAttribute(Attributes.MOVEMENT_SPEED), HAORI_SPEED_ID, 0.12, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
      updateModifier(entity.getAttribute(Attributes.ATTACK_SPEED), HAORI_ATTACK_SPEED_ID, 0.10, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
      CompoundTag data = entity.getPersistentData();
      if (!data.getBoolean(TAG_HAORI_INIT)) {
         data.putBoolean(TAG_HAORI_INIT, true);
         entity.setHealth(entity.getMaxHealth());
      }
   }

   private static void triggerWeakConstitution(OkitaSoujiSaberEntity entity, long now) {
      entity.getPersistentData().putLong(TAG_WEAK_UNTIL, now + WEAK_DURATION);
      entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, WEAK_DURATION, 1, true, true, true));
      entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, WEAK_DURATION, 0, true, true, true));
      if (entity.level() instanceof ServerLevel level) {
         level.sendParticles(ParticleTypes.SMOKE, entity.getX(), entity.getY() + 1.0, entity.getZ(), 18, 0.25, 0.35, 0.25, 0.04);
         level.playSound(null, entity.blockPosition(), SoundEvents.PLAYER_HURT, SoundSource.HOSTILE, 0.75F, 1.45F);
      }
   }

   private static boolean tryMindEye(OkitaSoujiSaberEntity entity, LivingEntity target, long now, boolean forced) {
      CompoundTag data = entity.getPersistentData();
      if (!forced && entity.getHealth() > entity.getMaxHealth() * 0.45F
         && !(entity.getLastHurtByMob() == target && entity.tickCount - entity.getLastHurtByMobTimestamp() < 35)) {
         return false;
      }
      if (now - data.getLong(TAG_LAST_MIND_EYE) < 32L) {
         return false;
      }
      boolean evaded = EvasionMovementService.tryEvade(entity, target.position(), 5, true);
      if (evaded) {
         data.putLong(TAG_LAST_MIND_EYE, now);
         if (entity.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.END_ROD, entity.getX(), entity.getY() + 0.9, entity.getZ(), 10, 0.2, 0.25, 0.2, 0.025);
            level.playSound(null, entity.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 0.45F, 1.8F);
         }
      }
      return evaded;
   }

   private static long rememberTarget(OkitaSoujiSaberEntity entity, LivingEntity target, long now) {
      CompoundTag data = entity.getPersistentData();
      UUID targetId = target.getUUID();
      if (!data.hasUUID(TAG_TARGET_UUID) || !targetId.equals(data.getUUID(TAG_TARGET_UUID))) {
         data.putUUID(TAG_TARGET_UUID, targetId);
         data.putLong(TAG_TARGET_ACQUIRED, now);
         return 0L;
      }
      return Math.max(0L, now - data.getLong(TAG_TARGET_ACQUIRED));
   }

   private static boolean shouldUseShukuchi(OkitaSoujiSaberEntity entity, LivingEntity target, long targetAge, int nearbyEnemies) {
      if (!entity.getSensing().hasLineOfSight(target)) {
         return false;
      }
      if (targetAge <= 80L) {
         return true;
      }
      if (nearbyEnemies <= 1 && entity.getHealth() > entity.getMaxHealth() * 0.35F) {
         return true;
      }
      return isDangerousTarget(entity, target) || target.getHealth() <= target.getMaxHealth() * 0.45F;
   }

   private static boolean shouldUseMumyoudanZuki(OkitaSoujiSaberEntity entity, LivingEntity target, double distance,
                                                long targetAge, int nearbyEnemies, long now) {
      if (!entity.canUseMumyoudanZuki() || distance > 5.5 || !entity.getSensing().hasLineOfSight(target)) {
         return false;
      }
      double targetHealthRatio = target.getHealth() / Math.max(1.0F, target.getMaxHealth());
      if (targetHealthRatio <= 0.35 || target.isUsingItem()) {
         return true;
      }
      boolean dangerous = isDangerousTarget(entity, target);
      if (isShukuchiActive(entity) && (targetHealthRatio <= 0.70 || dangerous)) {
         return true;
      }
      if (nearbyEnemies >= 3 && targetHealthRatio <= 0.75) {
         return true;
      }
      if (entity.getHealth() <= entity.getMaxHealth() * 0.40F && targetHealthRatio <= 0.80) {
         return true;
      }
      return targetAge >= 160L && dangerous && now >= entity.getPersistentData().getLong(TAG_MUMYOUDAN_COOLDOWN);
   }

   private static boolean isDangerousTarget(OkitaSoujiSaberEntity entity, LivingEntity target) {
      double targetAttack = target.getAttributeValue(Attributes.ATTACK_DAMAGE);
      double selfAttack = Math.max(1.0, entity.getAttributeValue(Attributes.ATTACK_DAMAGE));
      return target.getMaxHealth() >= entity.getMaxHealth() * 0.75F || targetAttack >= selfAttack * 1.1;
   }

   private static int basicAttackInterval(OkitaSoujiSaberEntity entity, int nearbyEnemies, boolean emergency) {
      if (isShukuchiActive(entity)) {
         return 6;
      }
      if (nearbyEnemies >= 3 || emergency) {
         return 11;
      }
      return 8;
   }

   private static boolean tryPressureReposition(OkitaSoujiSaberEntity entity, LivingEntity target, long now) {
      CompoundTag data = entity.getPersistentData();
      if (now - data.getLong(TAG_LAST_PRESSURE_REPOSITION) < 38L || entity.isPerformingAction()) {
         return false;
      }
      Vec3 away = entity.position().subtract(target.position()).multiply(1.0, 0.0, 1.0);
      if (away.lengthSqr() < 1.0E-4) {
         away = entity.getLookAngle().scale(-1.0).multiply(1.0, 0.0, 1.0);
      }
      if (away.lengthSqr() < 1.0E-4) {
         return false;
      }
      away = away.normalize();
      Vec3 side = new Vec3(-away.z, 0.0, away.x);
      if (entity.getRandom().nextBoolean()) {
         side = side.scale(-1.0);
      }
      Vec3 destination = safePositionNear(entity, target.position().add(away.scale(1.9)).add(side.scale(1.3)));
      if (destination == null) {
         return false;
      }
      data.putLong(TAG_LAST_PRESSURE_REPOSITION, now);
      entity.getNavigation().moveTo(destination.x, destination.y, destination.z, isShukuchiActive(entity) ? 1.45 : 1.25);
      entity.faceToward(target.position());
      return true;
   }

   private static void retreatFrom(OkitaSoujiSaberEntity entity, LivingEntity target, double speed) {
      Vec3 away = entity.position().subtract(target.position()).multiply(1.0, 0.0, 1.0);
      if (away.lengthSqr() < 1.0E-4) {
         away = entity.getLookAngle().scale(-1.0).multiply(1.0, 0.0, 1.0);
      }
      if (away.lengthSqr() < 1.0E-4) {
         return;
      }
      Vec3 destination = entity.position().add(away.normalize().scale(7.0));
      entity.getNavigation().moveTo(destination.x, destination.y, destination.z, speed);
   }

   private static Vec3 findShukuchiDestination(OkitaSoujiSaberEntity entity, LivingEntity target) {
      Vec3 back = target.getLookAngle().multiply(1.0, 0.0, 1.0);
      if (back.lengthSqr() < 1.0E-4) {
         back = target.position().subtract(entity.position()).multiply(1.0, 0.0, 1.0);
      }
      if (back.lengthSqr() < 1.0E-4) {
         back = new Vec3(0.0, 0.0, 1.0);
      }
      back = back.normalize();
      Vec3 side = new Vec3(-back.z, 0.0, back.x);
      List<Vec3> offsets = List.of(back.scale(-1.25), side.scale(1.25), side.scale(-1.25), back.scale(-2.0), back.scale(-1.25).add(side.scale(0.7)), back.scale(-1.25).subtract(side.scale(0.7)));
      for (Vec3 offset : offsets) {
         Vec3 base = target.position().add(offset);
         Vec3 safe = safePositionNear(entity, base);
         if (safe != null) {
            return safe;
         }
      }
      return null;
   }

   private static Vec3 safePositionNear(OkitaSoujiSaberEntity entity, Vec3 base) {
      for (int y = 1; y >= -2; y--) {
         Vec3 candidate = new Vec3(base.x, base.y + y, base.z);
         AABB moved = entity.getBoundingBox().move(candidate.subtract(entity.position()));
         if (!entity.level().noCollision(entity, moved)) {
            continue;
         }
         BlockPos belowPos = BlockPos.containing(candidate.x, candidate.y - 0.08, candidate.z);
         BlockState below = entity.level().getBlockState(belowPos);
         if (below.isFaceSturdy(entity.level(), belowPos, net.minecraft.core.Direction.UP)) {
            return candidate;
         }
      }
      return null;
   }

   private static Vec3 findSummonPosition(ServerLevel level, OkitaSoujiSaberEntity entity, int index, int total) {
      double angle = Math.PI * 2.0 * index / Math.max(1, total);
      double radius = 2.2 + (index % 2) * 1.4;
      Vec3 base = entity.position().add(Math.cos(angle) * radius, 0.0, Math.sin(angle) * radius);
      for (int y = 1; y >= -3; y--) {
         Vec3 candidate = new Vec3(base.x, base.y + y, base.z);
         AABB box = new AABB(candidate.x - 0.3, candidate.y, candidate.z - 0.3, candidate.x + 0.3, candidate.y + 1.8, candidate.z + 0.3);
         BlockPos below = BlockPos.containing(candidate.x, candidate.y - 0.08, candidate.z);
         if (level.noCollision(box) && level.getBlockState(below).isFaceSturdy(level, below, net.minecraft.core.Direction.UP)) {
            return candidate;
         }
      }
      return entity.position();
   }

   private static int countNearbyEnemies(OkitaSoujiSaberEntity entity, ServerLevel level) {
      return level.getEntitiesOfClass(LivingEntity.class, entity.getBoundingBox().inflate(10.0),
         candidate -> isValidTarget(entity, candidate)).size();
   }

   private static int countActiveShinsengumi(ServerLevel level, OkitaSoujiSaberEntity entity) {
      return level.getEntitiesOfClass(OkitaShinsengumiEntity.class, entity.getBoundingBox().inflate(96.0),
         soldier -> soldier.isAlive() && soldier.getPersistentData().hasUUID(SHINSENGUMI_OWNER_TAG)
            && entity.getUUID().equals(soldier.getPersistentData().getUUID(SHINSENGUMI_OWNER_TAG))).size();
   }

   private static boolean isShukuchiActive(OkitaSoujiSaberEntity entity) {
      return entity.getPersistentData().getLong(TAG_SHUKUCHI_UNTIL) > entity.level().getGameTime();
   }

   private static boolean isValidTarget(OkitaSoujiSaberEntity entity, LivingEntity target) {
      return target != null && target.isAlive() && target != entity && !entity.isAlliedTo(target)
         && !EntityUtils.isImmunePlayerTarget(target) && !ServantCombatSystem.isUntargetable(target);
   }

   private static void updateModifier(AttributeInstance attribute, ResourceLocation id, double amount, AttributeModifier.Operation operation) {
      if (attribute == null) {
         return;
      }
      AttributeModifier existing = attribute.getModifier(id);
      if (Math.abs(amount) < 1.0E-6) {
         if (existing != null) {
            attribute.removeModifier(id);
         }
         return;
      }
      if (existing != null && existing.operation() == operation && Math.abs(existing.amount() - amount) < 1.0E-6) {
         return;
      }
      if (existing != null) {
         attribute.removeModifier(id);
      }
      attribute.addTransientModifier(new AttributeModifier(id, amount, operation));
   }
}
