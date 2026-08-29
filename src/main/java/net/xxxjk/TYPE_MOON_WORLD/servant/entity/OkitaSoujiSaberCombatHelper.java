package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import java.util.ArrayList;
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
import net.xxxjk.TYPE_MOON_WORLD.servant.skill.ServantNoblePhantasmResourceService;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import org.joml.Vector3f;

public final class OkitaSoujiSaberCombatHelper {
   private static final String TAG_FLAG_POOL = "OkitaFlagPool";
   private static final String TAG_FLAG_POOL_INIT = "OkitaFlagPoolInitialized";
   private static final String TAG_HAORI_INIT = "OkitaHaoriInitialized";
   private static final String TAG_SHUKUCHI_COOLDOWN = "OkitaShukuchiCooldown";
   private static final String TAG_SHUKUCHI_UNTIL = "OkitaShukuchiUntil";
   private static final String TAG_MUMYOUDAN_COOLDOWN = "OkitaMumyoudanCooldown";
   private static final String TAG_MUMYOUDAN_POWER_SCALE = "OkitaMumyoudanPowerScale";
   private static final String TAG_MUMYOUDAN_OVERDRAFT = "OkitaMumyoudanOverdraft";
   private static final String TAG_WEAK_UNTIL = "OkitaWeakConstitutionUntil";
   private static final String TAG_FLAG_COOLDOWN = "OkitaFlagCooldown";
   private static final String TAG_ICHIMONJI_COOLDOWN = "OkitaIchimonjiCooldown";
   private static final String TAG_KAIFUU_COOLDOWN = "OkitaKaifuuCooldown";
   private static final String TAG_HAORI_RUSH_COOLDOWN = "OkitaHaoriRushCooldown";
   private static final String TAG_HAORI_RUSH_UNTIL = "OkitaHaoriRushUntil";
   private static final String TAG_FEIGNED_RETREAT_COOLDOWN = "OkitaFeignedRetreatCooldown";
   private static final String TAG_STANCE_BREAK_COOLDOWN = "OkitaStanceBreakCooldown";
   private static final String TAG_COMMAND_COOLDOWN = "OkitaCommandCooldown";
   private static final String TAG_LAST_MIND_EYE = "OkitaLastMindEye";
   private static final String TAG_TARGET_UUID = "OkitaCurrentTarget";
   private static final String TAG_TARGET_ACQUIRED = "OkitaTargetAcquiredTick";
   private static final String TAG_LAST_PRESSURE_REPOSITION = "OkitaLastPressureReposition";
   private static final String SHINSENGUMI_OWNER_TAG = "OkitaShinsengumiOwner";
   private static final int FLAG_POOL_MAX = 13;
   private static final int SHUKUCHI_COOLDOWN = 10;
   private static final int SHUKUCHI_DURATION = 200;
   private static final double SHUKUCHI_MP_COST = 0.0;
   private static final int MUMYOUDAN_COOLDOWN = 300;
   private static final double MUMYOUDAN_MP_COST = 20.0;
   private static final int WEAK_DURATION = 300;
   private static final int FLAG_COOLDOWN = 2400;
   private static final double FLAG_MP_COST = 100.0;
   private static final int FLAG_DURATION = 2400;
   private static final int ICHIMONJI_COOLDOWN = 60;
   private static final int KAIFUU_COOLDOWN = 90;
   private static final int HAORI_RUSH_COOLDOWN = 260;
   private static final int HAORI_RUSH_DURATION = 200;
   private static final int FEIGNED_RETREAT_COOLDOWN = 140;
   private static final int STANCE_BREAK_COOLDOWN = 180;
   private static final int COMMAND_COOLDOWN = 80;
   private static final double ICHIMONJI_MP_COST = 8.0;
   private static final double KAIFUU_MP_COST = 10.0;
   private static final double HAORI_RUSH_MP_COST = 18.0;
   private static final double FEIGNED_RETREAT_MP_COST = 8.0;
   private static final double STANCE_BREAK_MP_COST = 14.0;
   private static final double COMMAND_MP_COST = 6.0;
   private static final ResourceLocation HAORI_HEALTH_ID = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "okita_haori_health");
   private static final ResourceLocation HAORI_ATTACK_ID = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "okita_haori_attack");
   private static final ResourceLocation HAORI_SPEED_ID = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "okita_haori_speed");
   private static final ResourceLocation HAORI_ATTACK_SPEED_ID = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "okita_haori_attack_speed");
   private static final ResourceLocation HAORI_RUSH_SPEED_ID = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "okita_haori_rush_speed");
   private static final ResourceLocation HAORI_RUSH_ATTACK_SPEED_ID = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "okita_haori_rush_attack_speed");
   private static final ResourceLocation HAORI_RUSH_ATTACK_ID = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "okita_haori_rush_attack");
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
         if (tryRetreatShukuchi(entity, target, level, now)) {
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
      if (tryHaoriRush(entity, level, now, targetAge, nearbyEnemies, emergency)) {
         return;
      }
      if (tryCommandShinsengumi(entity, target, level, now)) {
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
      if (distance <= 3.8 && shouldUseMumyoudan && tryMumyoudanZuki(entity, target, level, now)) {
         return;
      }
      if (tryStanceBreak(entity, target, level, now)) {
         return;
      }
      if (tryKaifuu(entity, level, now, nearbyEnemies)) {
         return;
      }
      if (tryFeignedRetreat(entity, target, level, now, nearbyEnemies, emergency)) {
         return;
      }
      if (tryIchimonji(entity, target, level, now, targetAge)) {
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
      ensureHaoriActive(entity);
      long now = level.getGameTime();
      CompoundTag data = entity.getPersistentData();
      boolean shukuchi = isShukuchiActive(entity);
      updateModifier(entity.getAttribute(Attributes.MOVEMENT_SPEED), SHUKUCHI_SPEED_ID,
         shukuchi ? 0.50 : 0.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
      updateModifier(entity.getAttribute(Attributes.ATTACK_SPEED), SHUKUCHI_ATTACK_SPEED_ID,
         shukuchi ? 0.40 : 0.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
      boolean haoriRush = data.getLong(TAG_HAORI_RUSH_UNTIL) > now;
      updateModifier(entity.getAttribute(Attributes.MOVEMENT_SPEED), HAORI_RUSH_SPEED_ID,
         haoriRush ? 0.35 : 0.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
      updateModifier(entity.getAttribute(Attributes.ATTACK_SPEED), HAORI_RUSH_ATTACK_SPEED_ID,
         haoriRush ? 0.35 : 0.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
      updateModifier(entity.getAttribute(Attributes.ATTACK_DAMAGE), HAORI_RUSH_ATTACK_ID,
         haoriRush ? 0.15 : 0.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
      boolean weak = isWeakConstitutionActive(entity);
      updateModifier(entity.getAttribute(Attributes.ATTACK_DAMAGE), WEAK_ATTACK_ID,
         weak ? -0.30 : 0.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
      updateModifier(entity.getAttribute(Attributes.MOVEMENT_SPEED), WEAK_SPEED_ID,
         weak ? -0.30 : 0.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
      if (weak && entity.getTarget() != null && now % 5L == 0L) {
         if (now % 10L == 0L && tryRetreatShukuchi(entity, entity.getTarget(), level, now)) {
            return;
         }
         retreatFrom(entity, entity.getTarget(), 1.15);
      }
   }

   public static void ensureHaoriActive(OkitaSoujiSaberEntity entity) {
      if (!(entity.level() instanceof ServerLevel) || !entity.isAlive()) {
         return;
      }
      initializeFlagPool(entity);
      applyHaori(entity);
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
      ServantNoblePhantasmResourceService.CastDecision resource =
         ServantNoblePhantasmResourceService.evaluateNpcCast(entity, MUMYOUDAN_MP_COST);
      int cooldown = MUMYOUDAN_COOLDOWN * (data.getBoolean(TAG_MUMYOUDAN_OVERDRAFT) ? 2 : 1);
      if (!entity.canUseMumyoudanZuki() || entity.isPerformingAction() || !resource.allowed()
         || ServantNoblePhantasmResourceService.isOverdraftWeak(entity)
         || now < data.getLong(TAG_MUMYOUDAN_COOLDOWN) || !entity.hasMasterNoblePhantasmPermission()
         || !entity.getSensing().hasLineOfSight(target)) {
         return false;
      }
      data.putDouble(TAG_MUMYOUDAN_POWER_SCALE, resource.powerScale());
      data.putBoolean(TAG_MUMYOUDAN_OVERDRAFT, resource.overdraft());
      ServantNoblePhantasmResourceService.commitNpcCast(entity, resource);
      data.putLong(TAG_MUMYOUDAN_COOLDOWN, now + cooldown);
      entity.faceToward(target.position());
      entity.triggerSlashAnimation();
      ServantVoiceHelper.tryPlayOkitaSoujiSaberNp(entity);

      float damage = (float)(entity.getAttributeValue(Attributes.ATTACK_DAMAGE) * 3.0
         * Math.max(0.2, Math.min(1.0, data.getDouble(TAG_MUMYOUDAN_POWER_SCALE))));
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
      if (entity.isPerformingAction() || ServantCombatSystem.cannotAct(entity)
         || entity.getCurrentMp() < FLAG_MP_COST || now < data.getLong(TAG_FLAG_COOLDOWN)
         || !entity.hasMasterNoblePhantasmPermission()) {
         return false;
      }
      if (enemies < 3 && !emergency) {
         return false;
      }
      int count = FLAG_POOL_MAX;
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

   private static boolean tryHaoriRush(OkitaSoujiSaberEntity entity, ServerLevel level, long now, long targetAge,
                                       int nearbyEnemies, boolean emergency) {
      CompoundTag data = entity.getPersistentData();
      if (entity.isPerformingAction() || ServantCombatSystem.cannotAct(entity) || entity.getCurrentMp() < HAORI_RUSH_MP_COST
         || now < data.getLong(TAG_HAORI_RUSH_COOLDOWN) || data.getLong(TAG_HAORI_RUSH_UNTIL) > now) {
         return false;
      }
      if (targetAge > 50L && nearbyEnemies < 3 && !emergency) {
         return false;
      }
      entity.setCurrentMp(Math.max(0.0, entity.getCurrentMp() - HAORI_RUSH_MP_COST));
      data.putLong(TAG_HAORI_RUSH_COOLDOWN, now + HAORI_RUSH_COOLDOWN);
      data.putLong(TAG_HAORI_RUSH_UNTIL, now + HAORI_RUSH_DURATION);
      entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, HAORI_RUSH_DURATION, 1, false, true, true));
      entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, HAORI_RUSH_DURATION, 0, false, true, true));
      level.sendParticles(ASAGI, entity.getX(), entity.getY() + 0.9, entity.getZ(), 36, 0.45, 0.45, 0.45, 0.04);
      level.playSound(null, entity.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 0.9F, 1.45F);
      return true;
   }

   private static boolean tryIchimonji(OkitaSoujiSaberEntity entity, LivingEntity target, ServerLevel level, long now, long targetAge) {
      CompoundTag data = entity.getPersistentData();
      double distance = entity.distanceTo(target);
      if (entity.isPerformingAction() || entity.getCurrentMp() < ICHIMONJI_MP_COST || now < data.getLong(TAG_ICHIMONJI_COOLDOWN)
         || distance > 4.8 || !entity.getSensing().hasLineOfSight(target) || targetAge < 5L) {
         return false;
      }
      entity.setCurrentMp(Math.max(0.0, entity.getCurrentMp() - ICHIMONJI_MP_COST));
      data.putLong(TAG_ICHIMONJI_COOLDOWN, now + ICHIMONJI_COOLDOWN);
      Vec3 direction = horizontalVector(entity, target.position().subtract(entity.position()));
      entity.faceToward(target.position());
      entity.setDeltaMovement(direction.x * 1.15, Math.max(0.08, entity.getDeltaMovement().y), direction.z * 1.15);
      entity.hasImpulse = true;
      entity.triggerSlashAnimation();
      float damage = (float)(entity.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.35 + 6.0);
      List<LivingEntity> hit = hitForward(entity, 4.8, 0.68, damage, false);
      spawnSlashLine(level, entity.position().add(0.0, 0.9, 0.0), direction, 4.8);
      level.playSound(null, entity.blockPosition(), hit.isEmpty() ? SoundEvents.TRIDENT_THROW.value() : SoundEvents.PLAYER_ATTACK_CRIT,
         SoundSource.HOSTILE, 0.9F, hit.isEmpty() ? 1.7F : 1.4F);
      return true;
   }

   private static boolean tryKaifuu(OkitaSoujiSaberEntity entity, ServerLevel level, long now, int nearbyEnemies) {
      CompoundTag data = entity.getPersistentData();
      if (entity.isPerformingAction() || entity.getCurrentMp() < KAIFUU_MP_COST || now < data.getLong(TAG_KAIFUU_COOLDOWN)
         || nearbyEnemies < 2) {
         return false;
      }
      float damage = (float)(entity.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.9 + 5.0);
      boolean hitAny = false;
      for (LivingEntity target : nearbyEnemies(entity, level, 3.2)) {
         if (target == null || !target.isAlive()) {
            continue;
         }
         hurtPhysical(entity, target, damage);
         Vec3 away = horizontalVector(entity, target.position().subtract(entity.position()));
         target.push(away.x * 0.95, 0.18, away.z * 0.95);
         target.hasImpulse = true;
         hitAny = true;
      }
      if (!hitAny) {
         return false;
      }
      entity.setCurrentMp(Math.max(0.0, entity.getCurrentMp() - KAIFUU_MP_COST));
      data.putLong(TAG_KAIFUU_COOLDOWN, now + KAIFUU_COOLDOWN);
      entity.triggerSlashAnimation();
      level.sendParticles(ParticleTypes.SWEEP_ATTACK, entity.getX(), entity.getY() + 0.8, entity.getZ(), 12, 1.0, 0.2, 1.0, 0.0);
      level.playSound(null, entity.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.0F, 1.35F);
      return true;
   }

   private static boolean tryFeignedRetreat(OkitaSoujiSaberEntity entity, LivingEntity target, ServerLevel level, long now,
                                            int nearbyEnemies, boolean emergency) {
      CompoundTag data = entity.getPersistentData();
      double distance = entity.distanceTo(target);
      if (entity.isPerformingAction() || entity.getCurrentMp() < FEIGNED_RETREAT_MP_COST || now < data.getLong(TAG_FEIGNED_RETREAT_COOLDOWN)
         || distance > 3.6) {
         return false;
      }
      if (!emergency && nearbyEnemies < 2 && distance > 2.4) {
         return false;
      }
      Vec3 away = entity.position().subtract(target.position()).multiply(1.0, 0.0, 1.0);
      if (away.lengthSqr() < 1.0E-4) {
         away = entity.getLookAngle().scale(-1.0).multiply(1.0, 0.0, 1.0);
      }
      if (away.lengthSqr() < 1.0E-4) {
         return false;
      }
      entity.setCurrentMp(Math.max(0.0, entity.getCurrentMp() - FEIGNED_RETREAT_MP_COST));
      data.putLong(TAG_FEIGNED_RETREAT_COOLDOWN, now + FEIGNED_RETREAT_COOLDOWN);
      double power = isWeakConstitutionActive(entity) ? 1.65 : 1.25;
      entity.setDeltaMovement(away.normalize().scale(power).add(0.0, 0.18, 0.0));
      entity.hasImpulse = true;
      entity.fallDistance = 0.0F;
      if (!isWeakConstitutionActive(entity)) {
         entity.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
         entity.removeEffect(MobEffects.DIG_SLOWDOWN);
      }
      level.sendParticles(ParticleTypes.SMOKE, entity.getX(), entity.getY() + 0.6, entity.getZ(), 20, 0.25, 0.35, 0.25, 0.05);
      level.playSound(null, entity.blockPosition(), SoundEvents.WIND_CHARGE_BURST.value(), SoundSource.HOSTILE, 0.8F, 1.25F);
      return true;
   }

   private static boolean tryStanceBreak(OkitaSoujiSaberEntity entity, LivingEntity target, ServerLevel level, long now) {
      CompoundTag data = entity.getPersistentData();
      double distance = entity.distanceTo(target);
      if (entity.isPerformingAction() || entity.getCurrentMp() < STANCE_BREAK_MP_COST || now < data.getLong(TAG_STANCE_BREAK_COOLDOWN)
         || distance > 4.2 || !entity.getSensing().hasLineOfSight(target)) {
         return false;
      }
      boolean defensive = target.isUsingItem() || target.hasEffect(MobEffects.DAMAGE_RESISTANCE) || target.hasEffect(MobEffects.ABSORPTION);
      if (!defensive && distance > 2.6) {
         return false;
      }
      entity.setCurrentMp(Math.max(0.0, entity.getCurrentMp() - STANCE_BREAK_MP_COST));
      data.putLong(TAG_STANCE_BREAK_COOLDOWN, now + STANCE_BREAK_COOLDOWN);
      target.stopUsingItem();
      target.removeEffect(MobEffects.DAMAGE_RESISTANCE);
      target.removeEffect(MobEffects.ABSORPTION);
      target.getPersistentData().putLong("OkitaMumyoudanDefenseDisabledUntil", now + 100L);
      hurtPhysical(entity, target, (float)(entity.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.15 + 8.0));
      entity.triggerSlashAnimation();
      spawnSlashLine(level, entity.position().add(0.0, 0.9, 0.0), target.position().subtract(entity.position()), 4.0);
      level.playSound(null, target.blockPosition(), SoundEvents.SHIELD_BREAK, SoundSource.HOSTILE, 0.9F, 1.25F);
      return true;
   }

   private static boolean tryCommandShinsengumi(OkitaSoujiSaberEntity entity, LivingEntity target, ServerLevel level, long now) {
      CompoundTag data = entity.getPersistentData();
      if (entity.isPerformingAction() || entity.getCurrentMp() < COMMAND_MP_COST || now < data.getLong(TAG_COMMAND_COOLDOWN)) {
         return false;
      }
      List<OkitaShinsengumiEntity> soldiers = ownedShinsengumi(level, entity, 96.0);
      if (soldiers.isEmpty()) {
         return false;
      }
      entity.setCurrentMp(Math.max(0.0, entity.getCurrentMp() - COMMAND_MP_COST));
      data.putLong(TAG_COMMAND_COOLDOWN, now + COMMAND_COOLDOWN);
      for (OkitaShinsengumiEntity soldier : soldiers) {
         soldier.assignOkitaOwnerTarget(entity, target);
      }
      level.sendParticles(ASAGI, entity.getX(), entity.getY() + 1.0, entity.getZ(), 24, 1.0, 0.35, 1.0, 0.035);
      level.playSound(null, entity.blockPosition(), SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.HOSTILE, 0.9F, 1.4F);
      return true;
   }

   private static void applyHaori(OkitaSoujiSaberEntity entity) {
      updateModifier(entity.getAttribute(Attributes.MAX_HEALTH), HAORI_HEALTH_ID, 0.0, AttributeModifier.Operation.ADD_VALUE);
      updateModifier(entity.getAttribute(Attributes.ATTACK_DAMAGE), HAORI_ATTACK_ID, 0.0, AttributeModifier.Operation.ADD_VALUE);
      updateModifier(entity.getAttribute(Attributes.MOVEMENT_SPEED), HAORI_SPEED_ID, 0.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
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
      if (!entity.canUseMumyoudanZuki() || distance > 6.0 || !entity.getSensing().hasLineOfSight(target)) {
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

   private static boolean tryRetreatShukuchi(OkitaSoujiSaberEntity entity, LivingEntity target, ServerLevel level, long now) {
      CompoundTag data = entity.getPersistentData();
      if (entity.isPerformingAction() || now < data.getLong(TAG_SHUKUCHI_COOLDOWN)) {
         return false;
      }
      Vec3 away = entity.position().subtract(target.position()).multiply(1.0, 0.0, 1.0);
      if (away.lengthSqr() < 1.0E-4) {
         away = entity.getLookAngle().scale(-1.0).multiply(1.0, 0.0, 1.0);
      }
      if (away.lengthSqr() < 1.0E-4) {
         return false;
      }
      Vec3 destination = safePositionNear(entity, entity.position().add(away.normalize().scale(5.0)));
      if (destination == null) {
         return false;
      }
      data.putLong(TAG_SHUKUCHI_COOLDOWN, now + SHUKUCHI_COOLDOWN);
      data.putLong(TAG_SHUKUCHI_UNTIL, now + SHUKUCHI_DURATION);
      Vec3 start = entity.position();
      entity.triggerTeleportAnimation();
      entity.teleportTo(destination.x, destination.y, destination.z);
      entity.faceToward(target.position());
      level.sendParticles(ASAGI, start.x, start.y + 0.45, start.z, 12, 0.25, 0.35, 0.25, 0.03);
      level.sendParticles(ASAGI, destination.x, destination.y + 0.45, destination.z, 18, 0.25, 0.35, 0.25, 0.03);
      level.playSound(null, entity.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 0.75F, 1.85F);
      return true;
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

   private static List<LivingEntity> nearbyEnemies(OkitaSoujiSaberEntity entity, ServerLevel level, double radius) {
      return new ArrayList<>(level.getEntitiesOfClass(LivingEntity.class, entity.getBoundingBox().inflate(radius),
         candidate -> isValidTarget(entity, candidate)));
   }

   private static List<OkitaShinsengumiEntity> ownedShinsengumi(ServerLevel level, OkitaSoujiSaberEntity entity, double radius) {
      return level.getEntitiesOfClass(OkitaShinsengumiEntity.class, entity.getBoundingBox().inflate(radius),
         soldier -> soldier.isAlive() && soldier.getPersistentData().hasUUID(SHINSENGUMI_OWNER_TAG)
            && entity.getUUID().equals(soldier.getPersistentData().getUUID(SHINSENGUMI_OWNER_TAG)));
   }

   private static List<LivingEntity> hitForward(OkitaSoujiSaberEntity entity, double range, double minDot, float damage, boolean bypassPhysical) {
      Vec3 direction = horizontalVector(entity, entity.getLookAngle());
      Vec3 origin = entity.position().add(0.0, entity.getBbHeight() * 0.5, 0.0);
      AABB area = entity.getBoundingBox().expandTowards(direction.scale(range)).inflate(1.3, 1.1, 1.3);
      List<LivingEntity> hit = new ArrayList<>();
      for (LivingEntity target : entity.level().getEntitiesOfClass(LivingEntity.class, area, candidate -> isValidTarget(entity, candidate))) {
         Vec3 relative = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0).subtract(origin);
         Vec3 horizontal = new Vec3(relative.x, 0.0, relative.z);
         if (relative.lengthSqr() > range * range || horizontal.lengthSqr() < 1.0E-4 || horizontal.normalize().dot(direction) < minDot) {
            continue;
         }
         if (bypassPhysical) {
            target.invulnerableTime = 0;
            target.hurt(entity.damageSources().source(OkitaSoujiSaberDamageTypes.MUMYOUDAN_ZUKI, entity), damage);
            target.invulnerableTime = 0;
         } else {
            hurtPhysical(entity, target, damage);
         }
         hit.add(target);
      }
      return hit;
   }

   private static Vec3 horizontalVector(OkitaSoujiSaberEntity entity, Vec3 vector) {
      Vec3 horizontal = new Vec3(vector.x, 0.0, vector.z);
      if (horizontal.lengthSqr() < 1.0E-4) {
         horizontal = new Vec3(entity.getLookAngle().x, 0.0, entity.getLookAngle().z);
      }
      return horizontal.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : horizontal.normalize();
   }

   private static boolean isShukuchiActive(OkitaSoujiSaberEntity entity) {
      return entity.getPersistentData().getLong(TAG_SHUKUCHI_UNTIL) > entity.level().getGameTime();
   }

   private static boolean isValidTarget(OkitaSoujiSaberEntity entity, LivingEntity target) {
      return target != null && target.isAlive() && target != entity && !entity.isAlliedTo(target)
         && !EntityUtils.isImmunePlayerTarget(target) && !ServantCombatSystem.isUntargetable(target);
   }

   private static LivingEntity findEnemyLookTarget(OkitaSoujiSaberEntity entity, double range, double inflate) {
      Vec3 eye = entity.getEyePosition();
      Vec3 look = entity.getLookAngle().normalize();
      AABB box = entity.getBoundingBox().expandTowards(look.scale(range)).inflate(inflate);
      LivingEntity best = null;
      double bestScore = 0.78;
      for (LivingEntity living : entity.level().getEntitiesOfClass(LivingEntity.class, box,
         candidate -> isValidTarget(entity, candidate) && entity.hasLineOfSight(candidate))) {
         Vec3 to = living.position().add(0.0, living.getBbHeight() * 0.5, 0.0).subtract(eye);
         double distance = to.length();
         if (distance <= 0.01 || distance > range) {
            continue;
         }
         double score = look.dot(to.normalize());
         if (score > bestScore) {
            bestScore = score;
            best = living;
         }
      }
      return best;
   }

   private static LivingEntity findNearestEnemy(OkitaSoujiSaberEntity entity, double radius) {
      LivingEntity best = null;
      double bestDistance = radius * radius + 1.0;
      for (LivingEntity living : nearbyEnemies(entity, (ServerLevel)entity.level(), radius)) {
         double distance = entity.distanceToSqr(living);
         if (distance < bestDistance) {
            bestDistance = distance;
            best = living;
         }
      }
      return best;
   }

   private static Vec3 findBehindTarget(OkitaSoujiSaberEntity entity, LivingEntity target) {
      Vec3 back = target.getLookAngle().multiply(1.0, 0.0, 1.0);
      if (back.lengthSqr() < 1.0E-4) {
         back = target.position().subtract(entity.position()).multiply(1.0, 0.0, 1.0);
      }
      if (back.lengthSqr() < 1.0E-4) {
         back = new Vec3(0.0, 0.0, 1.0);
      }
      back = back.normalize();
      Vec3 side = new Vec3(-back.z, 0.0, back.x);
      Vec3[] candidates = {
         target.position().subtract(back.scale(1.25)),
         target.position().add(side.scale(1.25)),
         target.position().subtract(side.scale(1.25)),
         target.position().subtract(back.scale(2.0))
      };
      for (Vec3 candidate : candidates) {
         Vec3 safe = safePositionNear(entity, candidate);
         if (safe != null) {
            return safe;
         }
      }
      return null;
   }

   private static Vec3 findLookBlockDestination(OkitaSoujiSaberEntity entity, double range) {
      if (!(entity.level() instanceof ServerLevel level)) {
         return null;
      }
      Vec3 eye = entity.getEyePosition();
      Vec3 look = entity.getLookAngle().normalize();
      var hit = level.clip(new net.minecraft.world.level.ClipContext(eye, eye.add(look.scale(range)), net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, entity));
      if (hit.getType() != net.minecraft.world.phys.HitResult.Type.BLOCK) {
         return null;
      }
      var blockHit = (net.minecraft.world.phys.BlockHitResult)hit;
      Vec3 base = blockHit.getLocation().subtract(look.scale(0.35));
      Vec3 safe = safePositionNear(entity, base);
      return safe != null ? safe : safePositionNear(entity, Vec3.atBottomCenterOf(blockHit.getBlockPos().relative(blockHit.getDirection())));
   }

   private static boolean trySafeTeleport(OkitaSoujiSaberEntity entity, Vec3 destination) {
      Vec3 safe = safePositionNear(entity, destination);
      if (safe == null) {
         return false;
      }
      entity.teleportTo(safe.x, safe.y, safe.z);
      entity.fallDistance = 0.0F;
      return true;
   }

   private static void spawnStepEffects(ServerLevel level, Vec3 start, Vec3 destination, SoundSource source) {
      level.sendParticles(ASAGI, start.x, start.y + 0.45, start.z, 16, 0.25, 0.35, 0.25, 0.03);
      level.sendParticles(ASAGI, destination.x, destination.y + 0.45, destination.z, 20, 0.25, 0.35, 0.25, 0.03);
      level.playSound(null, BlockPos.containing(destination), SoundEvents.ENDERMAN_TELEPORT, source, 0.85F, 1.75F);
   }

   private static void spawnSlashLine(ServerLevel level, Vec3 origin, Vec3 direction, double length) {
      Vec3 dir = new Vec3(direction.x, 0.0, direction.z);
      if (dir.lengthSqr() < 1.0E-4) {
         dir = new Vec3(0.0, 0.0, 1.0);
      }
      dir = dir.normalize();
      for (double d = 0.0; d <= length; d += 0.45) {
         Vec3 point = origin.add(dir.scale(d));
         level.sendParticles(ASAGI, point.x, point.y, point.z, 2, 0.03, 0.03, 0.03, 0.0);
      }
   }

   private static void hurtPhysical(OkitaSoujiSaberEntity entity, LivingEntity target, float damage) {
      target.invulnerableTime = 0;
      target.hurt(entity.damageSources().mobAttack(entity), damage);
      target.invulnerableTime = 0;
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
