package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantNavigationHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatSystem;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantIdentityHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantDefinition;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantTraitTag;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXServerEffects;

public final class UshiwakamaruCombatHelper {
   public static final String TAG_GUARANTEED_HIT_UNTIL = "UshiwakamaruGuaranteedHitUntil";
   private static final String TAG_LAST_CHARISMA = "UshiwakamaruLastCharisma";
   private static final String TAG_LAST_SIX_SECRET = "UshiwakamaruLastSixSecret";
   private static final String TAG_LAST_USUMIDORI = "UshiwakamaruLastUsumidori";
   private static final String TAG_LAST_BENKEI = "UshiwakamaruLastBenkei";
   private static final String TAG_LAST_EIGHT_BOAT = "UshiwakamaruLastEightBoat";
   private static final String TAG_LAST_SPIDER_SLAYER = "UshiwakamaruLastSpiderSlayer";
   private static final String TAG_LAST_MOONLIT_STEP = "UshiwakamaruLastMoonlitStep";
   private static final String TAG_LAST_SWEEPING_THRUST = "UshiwakamaruLastSweepingThrust";
   private static final String TAG_LAST_BASIC_ATTACK = "UshiwakamaruLastBasicAttack";
   public static final String TAG_EIGHT_BOAT_UNTIL = "UshiwakamaruEightBoatUntil";
   public static final String TAG_EIGHT_BOAT_NEXT_DASH = "UshiwakamaruEightBoatNextDash";
   private static final String TAG_CHARISMA_UNTIL = "UshiwakamaruCharismaUntil";
   private static final String TAG_SIX_SECRET_UNTIL = "UshiwakamaruSixSecretUntil";
   private static final String TAG_TENGU_VFX_ACTIVE = "UshiwakamaruTenguVfxActive";

   private static final int CHARISMA_COOLDOWN = 30 * 20;
   private static final int CHARISMA_DURATION = 20 * 20;
   private static final int SIX_SECRET_COOLDOWN = 25 * 20;
   private static final int SIX_SECRET_DURATION = 8 * 20;
   private static final int USUMIDORI_COOLDOWN = 12 * 20;
   private static final int BENKEI_COOLDOWN = 30 * 20;
   private static final int BENKEI_DURATION = 15 * 20;
   private static final int EIGHT_BOAT_COOLDOWN = 20 * 20;
   private static final int EIGHT_BOAT_DURATION = 15 * 20;
   private static final int SPIDER_SLAYER_COOLDOWN = 20 * 20;
   private static final int MOONLIT_STEP_COOLDOWN = 7 * 20;
   private static final int SWEEPING_THRUST_COOLDOWN = 6 * 20;
   private static final int BASIC_ATTACK_COOLDOWN = 16;
   private static final int CLONE_COUNT = 7;

   private static final ResourceLocation RIDING_SPEED_ID = id("ushiwakamaru_riding_speed");
   private static final ResourceLocation EIGHT_BOAT_SPEED_ID = id("ushiwakamaru_eight_boat_speed");
   private static final ResourceLocation CHARISMA_ATTACK_ID = id("ushiwakamaru_charisma_attack");
   private static final ResourceLocation SIX_SECRET_ALLY_SPEED_ID = id("ushiwakamaru_six_secret_ally_speed");
   private static final ResourceLocation SIX_SECRET_ENEMY_SPEED_ID = id("ushiwakamaru_six_secret_enemy_speed");

   private UshiwakamaruCombatHelper() {
   }

   public static void tick(UshiwakamaruRiderEntity entity) {
      if (!(entity.level() instanceof ServerLevel level)) {
         return;
      }
      if (entity.isClone()) {
         tickClone(entity, level);
         return;
      }

      long now = level.getGameTime();
      applyRidingMobility(entity);
      tickEightBoatMovement(entity, now);
      LivingEntity target = resolveTarget(entity);
      if (target == null) {
         entity.getPersistentData().remove(TAG_TENGU_VFX_ACTIVE);
         return;
      }
      if (!entity.getPersistentData().getBoolean(TAG_TENGU_VFX_ACTIVE)) {
         entity.getPersistentData().putBoolean(TAG_TENGU_VFX_ACTIVE, true);
         VFXServerEffects.spawn(level, "servant_ushiwakamaru_tengu_strategy", entity, 96.0);
      }
      entity.setTarget(target);
      entity.getLookControl().setLookAt(target, 45.0F, 45.0F);
      if (ServantCombatSystem.cannotAct(entity) || ServantCombatSystem.skillsSuppressed(entity) || entity.isPerformingAction()) {
         return;
      }

      boolean scanCombatArea = ((entity.tickCount + entity.getId()) & 1) == 0;
      List<LivingEntity> enemies = scanCombatArea ? nearbyEnemies(entity, 20.0) : List.of();
      int allies = scanCombatArea ? nearbyAllies(entity, 15.0).size() : 0;
      int phase = entity.getCombatPhase();
      double distance = entity.distanceTo(target);

      if (phase >= 3 && trySpiderSlayer(entity, level, target, enemies, now)) return;
      if (phase >= 3 && tryEightBoatLeap(entity, level, target, distance, now)) return;
      if (phase >= 2 && tryBenkeiShield(entity, level, target, now)) return;
      if (trySixSecret(entity, level, enemies, allies, now)) return;
      if (tryCharisma(entity, level, allies, now)) return;
      if (phase >= 2 && tryUsumidori(entity, level, target, distance, now)) return;
      if (tryMoonlitStep(entity, level, target, distance, now)) return;
      if (trySweepingThrust(entity, level, target, distance, now)) return;

      if (distance <= 3.1 && ready(entity.getPersistentData(), TAG_LAST_BASIC_ATTACK, now, BASIC_ATTACK_COOLDOWN)) {
         entity.getPersistentData().putLong(TAG_LAST_BASIC_ATTACK, now);
         entity.doHurtTarget(target);
      } else if (distance > 2.7) {
         ServantNavigationHelper.moveToTargetThrottled(entity, target, 1.35, now, 5, 0.45, "UshiwakamaruChasePath");
      }
   }

   public static void tickPersistentState(UshiwakamaruRiderEntity entity) {
      if (!(entity.level() instanceof ServerLevel level)) {
         return;
      }
      long now = level.getGameTime();
      CompoundTag data = entity.getPersistentData();
      applyRidingMobility(entity);
      if (data.getLong(TAG_EIGHT_BOAT_UNTIL) <= now) {
         removeModifier(entity.getAttribute(Attributes.MOVEMENT_SPEED), EIGHT_BOAT_SPEED_ID);
         data.remove(TAG_EIGHT_BOAT_UNTIL);
         data.remove(TAG_EIGHT_BOAT_NEXT_DASH);
         if (!entity.isClone() && data.hasUUID(UshiwakamaruRiderEntity.TAG_EIGHT_BOAT_TARGET)) {
            cleanupExistingClones(entity, level);
            data.remove(UshiwakamaruRiderEntity.TAG_EIGHT_BOAT_TARGET);
            entity.setEightBoatTarget(null);
         }
      } else {
         updateModifier(entity.getAttribute(Attributes.MOVEMENT_SPEED), EIGHT_BOAT_SPEED_ID, 2.0,
            AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
      }
      if (entity.isClone()) return;
      if (data.hasUUID(UshiwakamaruRiderEntity.TAG_EIGHT_BOAT_TARGET) && !isEightBoatTargetAlive(entity)) {
         cleanupExistingClones(entity, level);
         data.remove(UshiwakamaruRiderEntity.TAG_EIGHT_BOAT_TARGET);
         entity.setEightBoatTarget(null);
      }
      if (data.getLong(UshiwakamaruRiderEntity.TAG_SHIELD_EXPIRES) > now) {
         if ((entity.tickCount & 3) == 0) {
            spawnShieldParticles(entity, level);
         }
      } else {
         data.remove(UshiwakamaruRiderEntity.TAG_SHIELD_EXPIRES);
         data.remove(UshiwakamaruRiderEntity.TAG_SHIELD_HP);
      }
   }

   public static boolean trySwallowDodge(UshiwakamaruRiderEntity entity, DamageSource source) {
      if (entity == null || source == null || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
         return false;
      }
      Entity direct = source.getDirectEntity();
      Entity attacker = source.getEntity();
      if (!(attacker instanceof LivingEntity) || direct != attacker || entity.distanceToSqr(attacker) > 25.0) {
         return false;
      }
      boolean fromAbove = attacker.getY() > entity.getY() + entity.getBbHeight() * 0.75;
      if (entity.getRandom().nextFloat() >= UshiwakamaruCombatRules.swallowDodgeChance(fromAbove)) {
         return false;
      }
      Vec3 away = entity.position().subtract(attacker.position()).multiply(1.0, 0.0, 1.0);
      if (away.lengthSqr() < 1.0E-4) away = new Vec3(1.0, 0.0, 0.0);
      away = away.normalize();
      entity.setDeltaMovement(away.x * 0.95, Math.max(0.18, entity.getDeltaMovement().y), away.z * 0.95);
      entity.hurtMarked = true;
      if (entity.level() instanceof ServerLevel level) {
         VFXServerEffects.spawn(level, "servant_ushiwakamaru_swallow_dodge", entity, 96.0);
         level.sendParticles(ParticleTypes.CLOUD, entity.getX(), entity.getY() + 0.8, entity.getZ(), 14, 0.35, 0.25, 0.35, 0.05);
         level.playSound(null, entity.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 0.8F, 1.7F);
      }
      return true;
   }

   public static boolean isGuaranteedHit(DamageSource source, long now) {
      return source != null && source.getEntity() != null
         && source.getEntity().getPersistentData().getLong(TAG_GUARANTEED_HIT_UNTIL) >= now;
   }

   public static boolean tryAbsorbShieldDamage(UshiwakamaruRiderEntity entity, DamageSource source, float amount) {
      if (entity == null || entity.isClone() || amount <= 0.0F || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
         return false;
      }
      CompoundTag data = entity.getPersistentData();
      long now = entity.level().getGameTime();
      if (data.getLong(UshiwakamaruRiderEntity.TAG_SHIELD_EXPIRES) <= now || data.getFloat(UshiwakamaruRiderEntity.TAG_SHIELD_HP) <= 0.0F) {
         return false;
      }
      UshiwakamaruCombatRules.ShieldHit hit = UshiwakamaruCombatRules.absorbShieldHit(
         data.getFloat(UshiwakamaruRiderEntity.TAG_SHIELD_HP), amount
      );
      data.putFloat(UshiwakamaruRiderEntity.TAG_SHIELD_HP, hit.remainingShieldHp());
      if (entity.level() instanceof ServerLevel level) {
         level.sendParticles(ParticleTypes.END_ROD, entity.getX(), entity.getY() + 1.0, entity.getZ(), 20, 0.55, 0.65, 0.55, 0.04);
         level.playSound(null, entity.blockPosition(), hit.broken() ? SoundEvents.GLASS_BREAK : SoundEvents.SHIELD_BLOCK,
            SoundSource.HOSTILE, 1.1F, hit.broken() ? 0.65F : 1.25F);
      }
      if (hit.broken()) {
         data.remove(UshiwakamaruRiderEntity.TAG_SHIELD_EXPIRES);
         data.remove(UshiwakamaruRiderEntity.TAG_SHIELD_HP);
      }
      return true;
   }

   public static float applyRidingDamageReduction(UshiwakamaruRiderEntity entity, DamageSource source, float amount) {
      if (entity == null || source == null || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
         return amount;
      }
      return UshiwakamaruCombatRules.applyRidingDefense(amount);
   }

   public static void cleanup(UshiwakamaruRiderEntity entity) {
      if (!(entity.level() instanceof ServerLevel level)) {
         return;
      }
      removeModifier(entity.getAttribute(Attributes.MOVEMENT_SPEED), RIDING_SPEED_ID);
      removeModifier(entity.getAttribute(Attributes.MOVEMENT_SPEED), EIGHT_BOAT_SPEED_ID);
      for (UshiwakamaruRiderEntity clone : level.getEntitiesOfClass(
         UshiwakamaruRiderEntity.class, entity.getBoundingBox().inflate(96.0), candidate -> candidate.isClone()
            && candidate.getPersistentData().hasUUID(UshiwakamaruRiderEntity.TAG_CLONE_OWNER)
            && entity.getUUID().equals(candidate.getPersistentData().getUUID(UshiwakamaruRiderEntity.TAG_CLONE_OWNER)))) {
         clone.discard();
      }
      entity.getPersistentData().remove(UshiwakamaruRiderEntity.TAG_EIGHT_BOAT_TARGET);
      entity.setEightBoatTarget(null);
   }

   private static boolean tryCharisma(UshiwakamaruRiderEntity entity, ServerLevel level, int allies, long now) {
      CompoundTag data = entity.getPersistentData();
      if (allies <= 0 || entity.getCurrentMp() < 15.0 || !ready(data, TAG_LAST_CHARISMA, now, CHARISMA_COOLDOWN)) {
         return false;
      }
      entity.setCurrentMp(entity.getCurrentMp() - 15.0);
      data.putLong(TAG_LAST_CHARISMA, now);
      entity.triggerNamedActionAnimation("command");
      VFXServerEffects.spawn(level, "servant_ushiwakamaru_charisma", entity, 96.0);
      for (LivingEntity ally : nearbyAllies(entity, 15.0)) {
         applyTimedModifier(ally, Attributes.ATTACK_DAMAGE, CHARISMA_ATTACK_ID, 0.15,
            AttributeModifier.Operation.ADD_MULTIPLIED_BASE, TAG_CHARISMA_UNTIL, now + CHARISMA_DURATION, CHARISMA_DURATION);
      }
      level.sendParticles(ParticleTypes.HAPPY_VILLAGER, entity.getX(), entity.getY() + 1.0, entity.getZ(), 28, 1.2, 0.7, 1.2, 0.05);
      level.playSound(null, entity.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 1.0F, 1.35F);
      return true;
   }

   private static boolean trySixSecret(UshiwakamaruRiderEntity entity, ServerLevel level, List<LivingEntity> enemies, int allies, long now) {
      CompoundTag data = entity.getPersistentData();
      if (entity.getCurrentMp() < 25.0 || !ready(data, TAG_LAST_SIX_SECRET, now, SIX_SECRET_COOLDOWN)) {
         return false;
      }
      if (enemies.size() < 2 && allies <= 0 && entity.getRandom().nextFloat() > 0.18F) {
         return false;
      }
      entity.setCurrentMp(entity.getCurrentMp() - 25.0);
      data.putLong(TAG_LAST_SIX_SECRET, now);
      entity.triggerNamedActionAnimation("six_secret");
      VFXServerEffects.spawn(level, "servant_ushiwakamaru_six_secret", entity, 96.0);
      long until = now + SIX_SECRET_DURATION;
      AABB area = entity.getBoundingBox().inflate(15.0);
      for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, area, LivingEntity::isAlive)) {
         boolean ally = living == entity || entity.isAlliedTo(living);
         applyTimedModifier(living, Attributes.MOVEMENT_SPEED,
            ally ? SIX_SECRET_ALLY_SPEED_ID : SIX_SECRET_ENEMY_SPEED_ID,
            ally ? 0.30 : -0.30, AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
            TAG_SIX_SECRET_UNTIL, until, SIX_SECRET_DURATION);
      }
      level.sendParticles(ParticleTypes.ENCHANT, entity.getX(), entity.getY() + 0.3, entity.getZ(), 80, 7.0, 0.5, 7.0, 0.05);
      level.playSound(null, entity.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.HOSTILE, 1.1F, 0.85F);
      return true;
   }

   private static boolean tryUsumidori(UshiwakamaruRiderEntity entity, ServerLevel level, LivingEntity target, double distance, long now) {
      CompoundTag data = entity.getPersistentData();
      if (distance < 2.8 || distance > 14.0 || !entity.hasLineOfSight(target)
         || entity.getCurrentMp() < 15.0 || !ready(data, TAG_LAST_USUMIDORI, now, USUMIDORI_COOLDOWN)) {
         return false;
      }
      entity.setCurrentMp(entity.getCurrentMp() - 15.0);
      data.putLong(TAG_LAST_USUMIDORI, now);
      entity.faceToward(target.position());
      entity.triggerDashAnimation();
      Vec3 direction = target.position().subtract(entity.position());
      VFXServerEffects.spawnOriented(level, "servant_ushiwakamaru_usumidori",
         entity.position().add(0.0, entity.getBbHeight() * 0.48, 0.0), direction, 128.0);
      if (direction.lengthSqr() > 1.0E-4) {
         direction = direction.normalize();
         entity.setDeltaMovement(direction.x * 1.8, Math.max(0.18, direction.y * 0.6), direction.z * 1.8);
         entity.hurtMarked = true;
      }
      data.putLong(TAG_GUARANTEED_HIT_UNTIL, now + 1L);
      target.invulnerableTime = 0;
      target.hurt(entity.damageSources().mobAttack(entity), 150.0F);
      target.invulnerableTime = 0;
      data.remove(TAG_GUARANTEED_HIT_UNTIL);
      VFXServerEffects.spawn(level, "servant_ushiwakamaru_usumidori_impact",
         target.position().add(0.0, target.getBbHeight() * 0.5, 0.0), 128.0);
      level.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY() + target.getBbHeight() * 0.55, target.getZ(), 16, 0.5, 0.5, 0.5, 0.0);
      level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + 0.8, target.getZ(), 35, 0.65, 0.65, 0.65, 0.12);
      level.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.HOSTILE, 1.4F, 1.1F);
      ServantVoiceHelper.tryPlayAttack(entity);
      return true;
   }

   private static boolean tryMoonlitStep(UshiwakamaruRiderEntity entity, ServerLevel level, LivingEntity target, double distance, long now) {
      CompoundTag data = entity.getPersistentData();
      if (distance < 4.0 || distance > 12.0 || !entity.hasLineOfSight(target) || entity.getCurrentMp() < 6.0
         || !ready(data, TAG_LAST_MOONLIT_STEP, now, MOONLIT_STEP_COOLDOWN)) {
         return false;
      }
      Vec3 destination = findTeleportNearTarget(entity, target, level);
      if (destination == null) {
         return false;
      }

      Vec3 start = entity.position();
      entity.setCurrentMp(entity.getCurrentMp() - 6.0);
      data.putLong(TAG_LAST_MOONLIT_STEP, now);
      level.sendParticles(ParticleTypes.REVERSE_PORTAL, start.x, start.y + entity.getBbHeight() * 0.5, start.z,
         18, 0.28, 0.45, 0.28, 0.08);
      entity.teleportTo(destination.x, destination.y, destination.z);
      entity.getNavigation().stop();
      entity.setDeltaMovement(Vec3.ZERO);
      entity.fallDistance = 0.0F;
      entity.setTarget(target);
      entity.faceToward(target.position());
      entity.triggerDashAnimation();
      dealMeleeDamage(entity, target, (float)(entity.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.2 + 6.0));
      Vec3 direction = target.position().subtract(entity.position());
      VFXServerEffects.spawnOriented(level, "servant_ushiwakamaru_slash",
         entity.position().add(0.0, entity.getBbHeight() * 0.48, 0.0), direction, 96.0);
      level.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY() + target.getBbHeight() * 0.55,
         target.getZ(), 2, 0.0, 0.0, 0.0, 0.0);
      level.sendParticles(ParticleTypes.END_ROD, entity.getX(), entity.getY() + 0.8, entity.getZ(),
         14, 0.3, 0.45, 0.3, 0.04);
      level.playSound(null, entity.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 0.8F, 1.55F);
      ServantVoiceHelper.tryPlayAttack(entity);
      return true;
   }

   private static boolean trySweepingThrust(UshiwakamaruRiderEntity entity, ServerLevel level, LivingEntity target, double distance, long now) {
      CompoundTag data = entity.getPersistentData();
      if (distance > 4.6 || entity.getCurrentMp() < 8.0
         || !ready(data, TAG_LAST_SWEEPING_THRUST, now, SWEEPING_THRUST_COOLDOWN)) {
         return false;
      }
      Vec3 direction = target.position().subtract(entity.position()).multiply(1.0, 0.0, 1.0);
      if (direction.lengthSqr() < 1.0E-4) {
         direction = entity.getLookAngle().multiply(1.0, 0.0, 1.0);
      }
      if (direction.lengthSqr() < 1.0E-4) {
         return false;
      }
      Vec3 thrustDirection = direction.normalize();
      Vec3 origin = entity.position().add(0.0, entity.getBbHeight() * 0.5, 0.0);
      Vec3 end = origin.add(thrustDirection.scale(5.0));
      AABB area = new AABB(origin, end).inflate(1.35, 1.25, 1.35);
      List<LivingEntity> victims = level.getEntitiesOfClass(LivingEntity.class, area,
         living -> EntityUtils.isValidCombatTarget(entity, living) && isInsideThrust(entity, living, thrustDirection));
      if (victims.isEmpty()) {
         return false;
      }

      entity.setCurrentMp(entity.getCurrentMp() - 8.0);
      data.putLong(TAG_LAST_SWEEPING_THRUST, now);
      entity.faceToward(target.position());
      entity.triggerSlashAnimation();
      entity.setDeltaMovement(thrustDirection.x * 1.05, Math.max(0.12, entity.getDeltaMovement().y), thrustDirection.z * 1.05);
      entity.hurtMarked = true;
      float damage = (float)(entity.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.9 + 5.0);
      for (LivingEntity victim : victims) {
         dealMeleeDamage(entity, victim, damage);
         victim.push(thrustDirection.x * 0.7, 0.1, thrustDirection.z * 0.7);
         victim.hurtMarked = true;
      }
      VFXServerEffects.spawnOriented(level, "servant_ushiwakamaru_slash", origin, thrustDirection, 96.0);
      for (int step = 1; step <= 8; step++) {
         Vec3 point = origin.add(thrustDirection.scale(step * 0.58));
         level.sendParticles(step % 3 == 0 ? ParticleTypes.SWEEP_ATTACK : ParticleTypes.END_ROD,
            point.x, point.y, point.z, 1, 0.05, 0.05, 0.05, 0.0);
      }
      level.playSound(null, entity.blockPosition(), SoundEvents.TRIDENT_THROW.value(), SoundSource.HOSTILE, 0.95F, 1.65F);
      ServantVoiceHelper.tryPlayAttack(entity);
      return true;
   }

   private static Vec3 findTeleportNearTarget(UshiwakamaruRiderEntity entity, LivingEntity target, ServerLevel level) {
      Vec3 targetForward = target.getLookAngle().multiply(1.0, 0.0, 1.0);
      if (targetForward.lengthSqr() < 1.0E-4) {
         targetForward = target.position().subtract(entity.position()).multiply(1.0, 0.0, 1.0);
      }
      if (targetForward.lengthSqr() < 1.0E-4) targetForward = new Vec3(0.0, 0.0, 1.0);
      targetForward = targetForward.normalize();
      Vec3 side = new Vec3(-targetForward.z, 0.0, targetForward.x);
      Vec3[] candidates = {
         target.position().subtract(targetForward.scale(1.35)),
         target.position().add(side.scale(1.45)),
         target.position().subtract(side.scale(1.45)),
         target.position().add(targetForward.scale(1.35))
      };
      for (Vec3 candidate : candidates) {
         Vec3 destination = new Vec3(candidate.x, target.getY(), candidate.z);
         BlockPos blockPos = BlockPos.containing(destination);
         if (level.isInWorldBounds(blockPos)
            && level.noCollision(entity, entity.getBoundingBox().move(destination.subtract(entity.position())))) {
            return destination;
         }
      }
      return null;
   }

   private static boolean isInsideThrust(UshiwakamaruRiderEntity entity, LivingEntity target, Vec3 direction) {
      Vec3 offset = target.position().subtract(entity.position()).multiply(1.0, 0.0, 1.0);
      double forward = offset.dot(direction);
      Vec3 lateral = offset.subtract(direction.scale(forward));
      return forward >= -0.35 && forward <= 5.2 && lateral.lengthSqr() <= 2.25
         && Math.abs(target.getY() - entity.getY()) <= 2.5;
   }

   private static void dealMeleeDamage(UshiwakamaruRiderEntity entity, LivingEntity target, float damage) {
      target.invulnerableTime = 0;
      target.hurt(entity.damageSources().mobAttack(entity), damage);
      target.invulnerableTime = 0;
   }

   private static boolean tryBenkeiShield(UshiwakamaruRiderEntity entity, ServerLevel level, LivingEntity target, long now) {
      CompoundTag data = entity.getPersistentData();
      if (data.getLong(UshiwakamaruRiderEntity.TAG_SHIELD_EXPIRES) > now || entity.getCurrentMp() < 20.0
         || !ready(data, TAG_LAST_BENKEI, now, BENKEI_COOLDOWN)) {
         return false;
      }
      boolean threateningAction = target instanceof ServantEntity servant && servant.isPerformingAction();
      boolean urgent = entity.getHealth() <= entity.getMaxHealth() * 0.40F;
      boolean recentlyHurt = now - data.getLong("LastHurtTick") <= 12L && entity.getHealth() <= entity.getMaxHealth() * 0.65F;
      if (!urgent && !recentlyHurt && !threateningAction) {
         return false;
      }
      entity.setCurrentMp(entity.getCurrentMp() - 20.0);
      data.putLong(TAG_LAST_BENKEI, now);
      data.putFloat(UshiwakamaruRiderEntity.TAG_SHIELD_HP, UshiwakamaruCombatRules.SHIELD_MAX_HP);
      data.putLong(UshiwakamaruRiderEntity.TAG_SHIELD_EXPIRES, now + BENKEI_DURATION);
      entity.triggerShieldAnimation();
      VFXServerEffects.spawn(level, "servant_ushiwakamaru_benkei_shield", entity, 96.0);
      spawnShieldParticles(entity, level);
      level.playSound(null, entity.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.HOSTILE, 1.2F, 0.75F);
      return true;
   }

   private static boolean tryEightBoatLeap(UshiwakamaruRiderEntity entity, ServerLevel level, LivingEntity target, double distance, long now) {
      CompoundTag data = entity.getPersistentData();
      if (data.getLong(TAG_EIGHT_BOAT_UNTIL) > now || entity.getCurrentMp() < 20.0
         || !ready(data, TAG_LAST_EIGHT_BOAT, now, EIGHT_BOAT_COOLDOWN)) {
         return false;
      }
      boolean lowHealth = entity.getHealth() <= entity.getMaxHealth() * 0.30F;
      boolean obstructed = !entity.hasLineOfSight(target) || Math.abs(target.getY() - entity.getY()) >= 2.0;
      if (distance < 8.0 && !lowHealth && !obstructed) {
         return false;
      }
      entity.setCurrentMp(entity.getCurrentMp() - 20.0);
      data.putLong(TAG_LAST_EIGHT_BOAT, now);
      data.putLong(TAG_EIGHT_BOAT_UNTIL, now + EIGHT_BOAT_DURATION);
      data.putLong(TAG_EIGHT_BOAT_NEXT_DASH, now);
      data.putUUID(UshiwakamaruRiderEntity.TAG_EIGHT_BOAT_TARGET, target.getUUID());
      entity.setEightBoatTarget(target);
      updateModifier(entity.getAttribute(Attributes.MOVEMENT_SPEED), EIGHT_BOAT_SPEED_ID, 2.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
      entity.triggerEightBoatAnimation();
      VFXServerEffects.spawn(level, "servant_ushiwakamaru_eight_boat", entity, 128.0);
      spawnClones(entity, level, now + EIGHT_BOAT_DURATION);
      ServantVoiceHelper.tryPlayUshiwakamaruNp(entity);
      level.sendParticles(ParticleTypes.POOF, entity.getX(), entity.getY() + 0.9, entity.getZ(), 60, 1.4, 0.8, 1.4, 0.12);
      level.playSound(null, entity.blockPosition(), SoundEvents.WIND_CHARGE_BURST.value(), SoundSource.HOSTILE, 1.3F, 1.25F);
      return true;
   }

   private static boolean trySpiderSlayer(UshiwakamaruRiderEntity entity, ServerLevel level, LivingEntity target, List<LivingEntity> enemies, long now) {
      CompoundTag data = entity.getPersistentData();
      if (entity.getCurrentMp() < 30.0 || !ready(data, TAG_LAST_SPIDER_SLAYER, now, SPIDER_SLAYER_COOLDOWN)) {
         return false;
      }
      if (enemies.size() < 3 && !isDemonic(target)) {
         return false;
      }
      entity.setCurrentMp(entity.getCurrentMp() - 30.0);
      data.putLong(TAG_LAST_SPIDER_SLAYER, now);
      entity.triggerSpiderSlayerAnimation();
      VFXServerEffects.spawn(level, "servant_ushiwakamaru_spider_slayer", entity, 128.0);
      for (LivingEntity enemy : nearbyEnemies(entity, 15.0)) {
         float damage = isDemonic(enemy) ? 150.0F : 100.0F;
         enemy.invulnerableTime = 0;
         enemy.hurt(entity.damageSources().magic(), damage);
         enemy.invulnerableTime = 0;
         VFXServerEffects.spawn(level, "servant_ushiwakamaru_spider_slayer_impact",
            enemy.position().add(0.0, enemy.getBbHeight() * 0.5, 0.0), 128.0);
      }
      level.sendParticles(ParticleTypes.SONIC_BOOM, entity.getX(), entity.getY() + 1.0, entity.getZ(), 20, 5.5, 1.0, 5.5, 0.0);
      level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, entity.getX(), entity.getY() + 0.8, entity.getZ(), 90, 6.5, 1.0, 6.5, 0.08);
      level.playSound(null, entity.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 1.4F, 1.35F);
      return true;
   }

   private static void tickEightBoatMovement(UshiwakamaruRiderEntity entity, long now) {
      CompoundTag data = entity.getPersistentData();
      if (data.getLong(TAG_EIGHT_BOAT_UNTIL) <= now) return;
      updateModifier(entity.getAttribute(Attributes.MOVEMENT_SPEED), EIGHT_BOAT_SPEED_ID, 2.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
      if (now < data.getLong(TAG_EIGHT_BOAT_NEXT_DASH)) return;
      LivingEntity target = entity.getTarget();
      if (!EntityUtils.isValidCombatTarget(entity, target)) return;
      Vec3 toTarget = target.position().subtract(entity.position());
      Vec3 horizontal = new Vec3(toTarget.x, 0.0, toTarget.z);
      boolean needsJump = entity.onGround() && (entity.horizontalCollision || toTarget.y > 1.15);
      if (horizontal.lengthSqr() <= 6.25 && !needsJump) return;
      if (horizontal.lengthSqr() < 1.0E-4) {
         Vec3 look = entity.getLookAngle();
         horizontal = new Vec3(look.x, 0.0, look.z);
      }
      if (horizontal.lengthSqr() < 1.0E-4) horizontal = new Vec3(0.0, 0.0, 1.0);
      horizontal = horizontal.normalize();
      Vec3 motion = entity.getDeltaMovement();
      entity.setDeltaMovement(horizontal.x * 1.35, needsJump ? 0.82 : motion.y, horizontal.z * 1.35);
      data.putLong(TAG_EIGHT_BOAT_NEXT_DASH, now + 5L);
      entity.hurtMarked = true;
      entity.fallDistance = 0.0F;
   }

   public static boolean isEightBoatTargetAlive(UshiwakamaruRiderEntity owner) {
      if (!(owner.level() instanceof ServerLevel level)) return false;
      CompoundTag data = owner.getPersistentData();
      if (!data.hasUUID(UshiwakamaruRiderEntity.TAG_EIGHT_BOAT_TARGET)) return false;
      java.util.UUID targetId = data.getUUID(UshiwakamaruRiderEntity.TAG_EIGHT_BOAT_TARGET);
      LivingEntity stored = owner.getEightBoatTarget();
      if (stored != null && targetId.equals(stored.getUUID())) {
         return stored.isAlive() && !stored.isRemoved();
      }
      LivingEntity current = owner.getTarget();
      if (current != null && targetId.equals(current.getUUID())) {
         return current.isAlive() && !current.isRemoved();
      }
      Entity target = level.getEntity(targetId);
      return target instanceof LivingEntity living && living.isAlive() && !living.isRemoved();
   }

   private static void tickClone(UshiwakamaruRiderEntity clone, ServerLevel level) {
      LivingEntity target = resolveCloneTarget(clone, level);
      if (target == null) {
         LivingEntity owner = clone.getOwnerEntity();
         if (owner instanceof net.minecraft.server.level.ServerPlayer) {
            followPlayerOwner(clone, owner, level.getGameTime());
         }
         return;
      }
      clone.setTarget(target);
      clone.getLookControl().setLookAt(target, 50.0F, 50.0F);
      long now = level.getGameTime();
      applyRidingMobility(clone);
      tickEightBoatMovement(clone, now);
      if (ServantCombatSystem.tickBeforeAi(clone)) return;
      double distance = clone.distanceTo(target);
      if (distance <= 2.8 && ready(clone.getPersistentData(), TAG_LAST_BASIC_ATTACK, now, BASIC_ATTACK_COOLDOWN)) {
         clone.getPersistentData().putLong(TAG_LAST_BASIC_ATTACK, now);
         clone.doHurtTarget(target);
      } else {
         ServantNavigationHelper.moveToTargetThrottled(clone, target, 1.35, now, 5, 0.45, "UshiwakamaruClonePath");
         if (clone.horizontalCollision || target.getY() > clone.getY() + 1.0) {
            Vec3 motion = clone.getDeltaMovement();
            clone.setDeltaMovement(motion.x, Math.max(1.26, motion.y), motion.z);
            clone.hurtMarked = true;
         }
      }
   }

   private static void followPlayerOwner(UshiwakamaruRiderEntity clone, LivingEntity owner, long now) {
      clone.setTarget(null);
      clone.setEightBoatTarget(null);
      clone.getPersistentData().remove(UshiwakamaruRiderEntity.TAG_EIGHT_BOAT_TARGET);
      clone.getLookControl().setLookAt(owner, 30.0F, 30.0F);
      applyRidingMobility(clone);
      if (clone.distanceToSqr(owner) > 9.0) {
         ServantNavigationHelper.moveToTargetThrottled(clone, owner, 1.35, now, 5, 0.45,
            "UshiwakamaruCloneOwnerPath");
         if (clone.horizontalCollision || owner.getY() > clone.getY() + 1.0) {
            Vec3 motion = clone.getDeltaMovement();
            clone.setDeltaMovement(motion.x, Math.max(1.0, motion.y), motion.z);
            clone.hurtMarked = true;
         }
      } else {
         clone.getNavigation().stop();
      }
   }

   private static void spawnClones(UshiwakamaruRiderEntity owner, ServerLevel level, long expiresAt) {
      cleanupExistingClones(owner, level);
      net.minecraft.nbt.ListTag cloneUuids = new net.minecraft.nbt.ListTag();
      List<UshiwakamaruRiderEntity> spawnedClones = new java.util.ArrayList<>(CLONE_COUNT);
      for (int index = 0; index < CLONE_COUNT; index++) {
         UshiwakamaruRiderEntity clone = ModEntities.USHIWAKAMARU_RIDER.get().create(level);
         if (clone == null) continue;
         double angle = Math.PI * 2.0 * index / CLONE_COUNT;
         clone.moveTo(owner.getX() + Math.cos(angle) * 1.8, owner.getY() + 0.1, owner.getZ() + Math.sin(angle) * 1.8, owner.getYRot(), 0.0F);
         clone.initClone(owner, expiresAt);
         clone.getPersistentData().putLong(TAG_EIGHT_BOAT_UNTIL, expiresAt);
         clone.getPersistentData().putLong(TAG_EIGHT_BOAT_NEXT_DASH, level.getGameTime());
         if (level.addFreshEntity(clone)) {
            spawnedClones.add(clone);
            cloneUuids.add(net.minecraft.nbt.StringTag.valueOf(clone.getUUID().toString()));
            VFXServerEffects.spawn(level, "servant_ushiwakamaru_clone_manifest", clone, 128.0);
         }
      }
      for (UshiwakamaruRiderEntity clone : spawnedClones) {
         clone.setTarget(resolveNearbyCloneTarget(clone, level));
      }
      owner.getPersistentData().put(UshiwakamaruRiderEntity.TAG_CLONE_UUIDS, cloneUuids);
   }

   private static LivingEntity resolveCloneTarget(UshiwakamaruRiderEntity clone, ServerLevel level) {
      LivingEntity current = clone.getTarget();
      if (EntityUtils.isValidCombatTarget(clone, current)) return current;
      Entity ownerEntity = clone.getOwnerEntity();
      if (ownerEntity instanceof UshiwakamaruRiderEntity owner) {
         LivingEntity ownerTarget = owner.getTarget();
         if (EntityUtils.isValidCombatTarget(clone, ownerTarget)) return ownerTarget;
      }
      if (clone.getPersistentData().hasUUID(UshiwakamaruRiderEntity.TAG_EIGHT_BOAT_TARGET)) {
         Entity storedTarget = level.getEntity(clone.getPersistentData().getUUID(UshiwakamaruRiderEntity.TAG_EIGHT_BOAT_TARGET));
         if (storedTarget instanceof LivingEntity living && EntityUtils.isValidCombatTarget(clone, living)) return living;
      }
      return resolveNearbyCloneTarget(clone, level);
   }

   private static LivingEntity resolveNearbyCloneTarget(UshiwakamaruRiderEntity clone, ServerLevel level) {
      List<LivingEntity> enemies = nearbyEnemies(clone, 20.0);
      if (enemies.isEmpty()) return null;

      return enemies.stream().min(java.util.Comparator.comparingDouble(enemy -> {
         long assignedClones = level.getEntitiesOfClass(
            UshiwakamaruRiderEntity.class,
            clone.getBoundingBox().inflate(32.0),
            sibling -> sibling != clone && sibling.isClone() && sibling.getTarget() == enemy
               && sibling.getPersistentData().hasUUID(UshiwakamaruRiderEntity.TAG_CLONE_OWNER)
               && clone.getPersistentData().getUUID(UshiwakamaruRiderEntity.TAG_CLONE_OWNER)
                  .equals(sibling.getPersistentData().getUUID(UshiwakamaruRiderEntity.TAG_CLONE_OWNER))
         ).size();
         return clone.distanceToSqr(enemy) + assignedClones * 36.0;
      })).orElse(null);
   }

   private static void cleanupExistingClones(UshiwakamaruRiderEntity owner, ServerLevel level) {
      for (UshiwakamaruRiderEntity clone : level.getEntitiesOfClass(
         UshiwakamaruRiderEntity.class, owner.getBoundingBox().inflate(96.0), candidate -> candidate.isClone()
            && candidate.getPersistentData().hasUUID(UshiwakamaruRiderEntity.TAG_CLONE_OWNER)
            && owner.getUUID().equals(candidate.getPersistentData().getUUID(UshiwakamaruRiderEntity.TAG_CLONE_OWNER)))) {
         clone.discard();
      }
      owner.getPersistentData().remove(UshiwakamaruRiderEntity.TAG_CLONE_UUIDS);
   }

   private static void spawnShieldParticles(UshiwakamaruRiderEntity entity, ServerLevel level) {
      Vec3 center = entity.position().add(0.0, 0.08, 0.0);
      double radius = 2.05;
      int phase = entity.tickCount % 3;
      for (int latitude = 0; latitude < 3; latitude++) {
         double elevation = latitude * Math.PI / 6.0;
         double horizontalRadius = Math.cos(elevation) * radius;
         double height = Math.sin(elevation) * radius;
         for (int azimuth = 0; azimuth < 12; azimuth++) {
            if ((latitude * 4 + azimuth) % 3 != phase) continue;
            double angle = Math.PI * 2.0 * azimuth / 12.0 + entity.tickCount * 0.025;
            Vec3 point = center.add(Math.cos(angle) * horizontalRadius, height, Math.sin(angle) * horizontalRadius);
            level.sendParticles(ParticleTypes.END_ROD, point.x, point.y, point.z, 1, 0.02, 0.02, 0.02, 0.0);
         }
      }
      level.sendParticles(ParticleTypes.END_ROD, center.x, center.y + radius, center.z, 1, 0.02, 0.02, 0.02, 0.0);
   }

   private static void applyRidingMobility(UshiwakamaruRiderEntity entity) {
      updateModifier(entity.getAttribute(Attributes.MOVEMENT_SPEED), RIDING_SPEED_ID, 0.30, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
   }

   private static List<LivingEntity> nearbyEnemies(UshiwakamaruRiderEntity entity, double radius) {
      return entity.level().getEntitiesOfClass(LivingEntity.class, entity.getBoundingBox().inflate(radius),
         living -> EntityUtils.isValidCombatTarget(entity, living));
   }

   private static List<LivingEntity> nearbyAllies(UshiwakamaruRiderEntity entity, double radius) {
      return entity.level().getEntitiesOfClass(LivingEntity.class, entity.getBoundingBox().inflate(radius),
         living -> living.isAlive() && (living == entity || entity.isAlliedTo(living)));
   }

   private static LivingEntity resolveTarget(UshiwakamaruRiderEntity entity) {
      LivingEntity current = entity.getTarget();
      if (EntityUtils.isValidCombatTarget(entity, current)) return current;
      return nearbyEnemies(entity, 20.0).stream()
         .min(java.util.Comparator.comparingDouble(entity::distanceToSqr))
         .orElse(null);
   }

   private static boolean isDemonic(LivingEntity target) {
      ServantDefinition definition = ServantIdentityHelper.definitionOf(target);
      return definition != null && definition.traits().contains(ServantTraitTag.DEMONIC);
   }

   private static boolean ready(CompoundTag data, String key, long now, int cooldown) {
      long last = data.getLong(key);
      return last <= 0L || now - last >= cooldown;
   }

   private static void applyTimedModifier(LivingEntity entity, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
                                          ResourceLocation id, double amount, AttributeModifier.Operation operation,
                                          String expiryTag, long expiresAt, int duration) {
      updateModifier(entity.getAttribute(attribute), id, amount, operation);
      entity.getPersistentData().putLong(expiryTag, expiresAt);
      TYPE_MOON_WORLD.queueServerWork(duration, () -> {
         if (entity.getPersistentData().getLong(expiryTag) <= entity.level().getGameTime()) {
            removeModifier(entity.getAttribute(attribute), id);
            entity.getPersistentData().remove(expiryTag);
         }
      });
   }

   private static void updateModifier(AttributeInstance attribute, ResourceLocation id, double amount, AttributeModifier.Operation operation) {
      if (attribute == null) return;
      AttributeModifier existing = attribute.getModifier(id);
      if (existing != null && existing.operation() == operation && Math.abs(existing.amount() - amount) < 1.0E-6) return;
      if (existing != null) attribute.removeModifier(id);
      attribute.addTransientModifier(new AttributeModifier(id, amount, operation));
   }

   private static void removeModifier(AttributeInstance attribute, ResourceLocation id) {
      if (attribute != null && attribute.getModifier(id) != null) attribute.removeModifier(id);
   }

   private static ResourceLocation id(String path) {
      return ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, path);
   }
}
