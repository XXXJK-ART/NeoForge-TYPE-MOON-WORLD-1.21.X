package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import java.util.Collection;
import java.util.List;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantAiContext;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantNavigationHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatSystem;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.skill.ServantNoblePhantasmResourceService;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXServerEffects;
import org.joml.Vector3f;

public final class LiShuwenCombatHelper {
   private static final String TAG_LAST_CIRCLE_REALM = "LiShuwenLastCircleRealm";
   private static final String TAG_LAST_YIN_YANG = "LiShuwenLastYinYang";
   private static final String TAG_YIN_YANG_UNTIL = "LiShuwenYinYangUntil";
   private static final String TAG_YIN_YANG_WEAK_NULL = "LiShuwenYinYangWeakNull";
   private static final String TAG_LAST_PUNCH = "LiShuwenLastPunch";
   private static final String TAG_LAST_SHOULDER = "LiShuwenLastShoulder";
   private static final String TAG_LAST_TREMOR = "LiShuwenLastTremor";
   private static final String TAG_LAST_COUNTER = "LiShuwenLastCounter";
   private static final String TAG_LAST_WU_ER_DA = "LiShuwenLastWuErDa";
   private static final String TAG_WU_ER_DA_POWER_SCALE = "LiShuwenWuErDaPowerScale";
   private static final String TAG_WU_ER_DA_OVERDRAFT = "LiShuwenWuErDaOverdraft";
   private static final String TAG_WU_ER_DA_RELEASE = "LiShuwenWuErDaRelease";
   private static final String TAG_WU_ER_DA_TARGET = "LiShuwenWuErDaTarget";
   private static final String TAG_CIRCLE_DODGE_UNTIL = "LiShuwenCircleDodgeUntil";
   private static final String TAG_CHINESE_MARTIAL_ARTS_ACTIVE = "LiShuwenChineseMartialArtsActive";
   private static final String TAG_LAST_SELF_STATE_TICK = "LiShuwenLastSelfStateTick";
   private static final String TAG_LAST_STEALTH_TARGET_CLEAR = "LiShuwenLastStealthTargetClear";
   private static final String TAG_UNTARGETABLE_UNTIL = "ServantCombat.UntargetableUntil";
   private static final String TAG_STUN_UNTIL = "ServantCombat.StunUntil";
   private static final String TAG_ARMOR_BREAK_UNTIL = "LiShuwenArmorBreakUntil";
   private static final int CIRCLE_REALM_COOLDOWN = 120;
   private static final int CIRCLE_REALM_DURATION = 100;
   private static final int YIN_YANG_COOLDOWN = 120;
   private static final int YIN_YANG_DURATION = 200;
   private static final int PUNCH_COOLDOWN = 32;
   private static final int SHOULDER_COOLDOWN = 70;
   private static final int TREMOR_COOLDOWN = 95;
   private static final int COUNTER_COOLDOWN = 70;
   private static final int WU_ER_DA_COOLDOWN = 500;
   private static final int WU_ER_DA_WINDUP = 0;
   private static final double WU_ER_DA_RANGE = 3.0;
   public static final double CIRCLE_REALM_MP_COST = 1.0;
   public static final double YIN_YANG_MP_COST = 3.0;
   public static final double WU_ER_DA_MP_COST = 8.0;
   public static final double SHOULDER_CHECK_MP_COST = 2.0;
   public static final double TREMOR_PALM_MP_COST = 2.0;
   public static final ResourceKey<DamageType> WU_ER_DA = ResourceKey.create(
      Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "wu_er_da"));
   private static final ResourceLocation YIN_YANG_ATTACK_ID = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "li_shuwen_yin_yang_attack");
   private static final ResourceLocation ARMOR_BREAK_ID = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "li_shuwen_wu_er_da_armor_break");
   private static final ResourceLocation CHINESE_MARTIAL_ARTS_ARMOR_ID = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "li_shuwen_chinese_martial_arts_armor");
   private static final DustParticleOptions INK = new DustParticleOptions(new Vector3f(0.02F, 0.02F, 0.02F), 1.25F);

   private LiShuwenCombatHelper() {
   }

   public static void tick(LiShuwenEntity entity, ServantAiContext context) {
      if (!(entity.level() instanceof ServerLevel level)) {
         return;
      }

      long now = level.getGameTime();
      tickSelfState(entity);
      if (tickWuErDaRelease(entity, level, now)) {
         return;
      }

      LivingEntity target = context.target();
      if (target == null || target.isDeadOrDying() || EntityUtils.isImmunePlayerTarget(target)) {
         target = entity.getTarget();
      }
      if (target == null || target.isDeadOrDying() || EntityUtils.isImmunePlayerTarget(target)) {
         entity.setTarget(null);
         return;
      }

      entity.setTarget(target);
      entity.getLookControl().setLookAt(target, 45.0F, 45.0F);
      if (ServantCombatSystem.cannotAct(entity) || ServantCombatSystem.skillsSuppressed(entity) || entity.isPerformingAction()) {
         return;
      }

      double distance = entity.distanceTo(target);
      double healthRatio = entity.getHealth() / Math.max(1.0F, entity.getMaxHealth());
      if ((healthRatio <= 0.30 || distance > 4.0) && tryCircleRealm(entity, level, now)) {
         return;
      }
      if (healthRatio <= 0.80 && distance <= WU_ER_DA_RANGE && tryBeginWuErDa(entity, target, level, now)) {
         return;
      }
      if (healthRatio <= 0.75 && tryYinYang(entity, level, now)) {
         return;
      }
      if (tryCounterPressure(entity, target, level, now, distance)) {
         return;
      }
      if (tryTremorPalm(entity, target, level, now, distance)) {
         return;
      }
      if (tryShoulderCheck(entity, target, level, now, distance)) {
         return;
      }
      if (tryBajiPunch(entity, target, level, now, distance)) {
         return;
      }

      if (distance > 2.2) {
         ServantNavigationHelper.moveToTargetThrottled(entity, target, 1.45, now, ServantNavigationHelper.SHORT_REPATH_INTERVAL, 0.55, "LiShuwenChasePath");
      } else {
         entity.triggerPunchAnimation();
         entity.doHurtTarget(target);
      }
   }

   public static void tickSelfState(LiShuwenEntity entity) {
      if (!(entity.level() instanceof ServerLevel level)) {
         return;
      }
      long now = level.getGameTime();
      CompoundTag data = entity.getPersistentData();
      if (data.contains(TAG_LAST_SELF_STATE_TICK) && data.getLong(TAG_LAST_SELF_STATE_TICK) == now) {
         return;
      }
      data.putLong(TAG_LAST_SELF_STATE_TICK, now);
      tickYinYangCleanup(entity, now);
      clearMentalEffects(entity);
      if (entity.hasEffect(MobEffects.INVISIBILITY)) {
         if (now - data.getLong(TAG_LAST_STEALTH_TARGET_CLEAR) >= 10L) {
            data.putLong(TAG_LAST_STEALTH_TARGET_CLEAR, now);
            clearNonServantTargeting(entity, level);
         }
      }
      if (entity.getTarget() == null) {
         entity.removeEffect(MobEffects.INVISIBILITY);
         data.remove(TAG_UNTARGETABLE_UNTIL);
      }
   }

   public static boolean tryConsumeCircleRealmDodge(LiShuwenEntity entity, DamageSource source) {
      if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || source.is(DamageTypeTags.BYPASSES_RESISTANCE)) {
         return false;
      }
      long now = entity.level().getGameTime();
      CompoundTag data = entity.getPersistentData();
      if (data.getLong(TAG_CIRCLE_DODGE_UNTIL) <= now) {
         return false;
      }
      data.remove(TAG_CIRCLE_DODGE_UNTIL);
      spawnCircleDodgeFx(entity);
      return true;
   }

   public static float applyIncomingDamageModifiers(LiShuwenEntity entity, DamageSource source, float amount) {
      if (entity == null || source == null || amount <= 0.0F) {
         return amount;
      }
      float reduced = amount;
      LivingEntity attacker = source.getEntity() instanceof LivingEntity living ? living : source.getDirectEntity() instanceof LivingEntity living ? living : null;
      if (attacker != null && entity.distanceTo(attacker) <= 4.0F) {
         reduced *= 0.5F;
      }
      if (entity.hasEffect(MobEffects.INVISIBILITY)) {
         reduced *= 0.05F;
      }
      return reduced;
   }

   public static boolean hasChineseMartialArts(LivingEntity entity) {
      return entity != null && entity.getPersistentData().getBoolean(TAG_CHINESE_MARTIAL_ARTS_ACTIVE);
   }

   public static void applyChineseMartialArtsAttributes(LiShuwenEntity entity) {
      if (entity == null) {
         return;
      }
      AttributeInstance armor = entity.getAttribute(Attributes.ARMOR);
      AttributeInstance toughness = entity.getAttribute(Attributes.ARMOR_TOUGHNESS);
      if (armor == null) {
         return;
      }
      if (hasChineseMartialArts(entity)) {
         updateModifier(armor, CHINESE_MARTIAL_ARTS_ARMOR_ID, 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
         if (toughness != null) {
            updateModifier(toughness, CHINESE_MARTIAL_ARTS_ARMOR_ID, Math.max(0.0, entity.getAttributeBaseValue(Attributes.ARMOR_TOUGHNESS)), AttributeModifier.Operation.ADD_VALUE);
         }
      } else if (armor.getModifier(CHINESE_MARTIAL_ARTS_ARMOR_ID) != null) {
         armor.removeModifier(CHINESE_MARTIAL_ARTS_ARMOR_ID);
         if (toughness != null && toughness.getModifier(CHINESE_MARTIAL_ARTS_ARMOR_ID) != null) {
            toughness.removeModifier(CHINESE_MARTIAL_ARTS_ARMOR_ID);
         }
      }
   }

   public static void spawnMartialHitFx(LiShuwenEntity entity, LivingEntity target) {
      if (!(entity.level() instanceof ServerLevel level)) {
         return;
      }
      level.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY() + target.getBbHeight() * 0.55, target.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
      level.sendParticles(INK, target.getX(), target.getY() + target.getBbHeight() * 0.55, target.getZ(), 8, 0.22, 0.18, 0.22, 0.02);
      level.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.HOSTILE, 0.8F, 0.72F);
   }

   public static void performPursuitGapClose(LiShuwenEntity entity, LivingEntity target, double speed, double verticalBoost, float damageScale) {
      if (entity == null || target == null || !target.isAlive()) {
         return;
      }
      Vec3 dir = horizontalDirection(entity, target);
      Vec3 start = entity.position();
      Vec3 arrive = target.position().subtract(dir.scale(1.15));
      entity.setDeltaMovement(dir.x * speed, Math.max(entity.getDeltaMovement().y, verticalBoost), dir.z * speed);
      entity.hasImpulse = true;
      entity.faceToward(target.position());
      if (entity.level() instanceof ServerLevel level) {
         level.sendParticles(ParticleTypes.CLOUD, start.x, start.y + entity.getBbHeight() * 0.45, start.z, 8, 0.12, 0.08, 0.12, 0.02);
         level.sendParticles(INK, start.x, start.y + entity.getBbHeight() * 0.45, start.z, 12, 0.18, 0.14, 0.18, 0.02);
      }
      if (entity.distanceTo(target) <= 3.2) {
         dealMartialDamage(entity, target, damageScale);
         push(target, dir, 0.75, 0.12);
         spawnMartialHitFx(entity, target);
         return;
      }
      if (entity.level() instanceof ServerLevel level) {
         entity.teleportTo(arrive.x, arrive.y, arrive.z);
         entity.setDeltaMovement(Vec3.ZERO);
         entity.hasImpulse = true;
         level.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY() + target.getBbHeight() * 0.55, target.getZ(), 2, 0.0, 0.0, 0.0, 0.0);
      }
      dealMartialDamage(entity, target, damageScale);
      push(target, dir, 0.6, 0.10);
      spawnMartialHitFx(entity, target);
   }

   private static boolean tryCircleRealm(LiShuwenEntity entity, ServerLevel level, long now) {
      CompoundTag data = entity.getPersistentData();
      if (entity.getCurrentMp() < CIRCLE_REALM_MP_COST || now - data.getLong(TAG_LAST_CIRCLE_REALM) < CIRCLE_REALM_COOLDOWN) {
         return false;
      }
      data.putLong(TAG_LAST_CIRCLE_REALM, now);
      data.putLong(TAG_CIRCLE_DODGE_UNTIL, now + CIRCLE_REALM_DURATION + 20L);
      data.putLong(TAG_UNTARGETABLE_UNTIL, now + CIRCLE_REALM_DURATION);
      entity.setCurrentMp(Math.max(0.0, entity.getCurrentMp() - CIRCLE_REALM_MP_COST));
      entity.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, CIRCLE_REALM_DURATION, 0, false, false, true));
      entity.triggerStepAnimation();
      entity.getNavigation().stop();
      VFXServerEffects.spawn(level, "servant_li_shuwen_quanjing", entity, 64.0);
      spawnCircleDodgeFx(entity);
      return true;
   }

   private static boolean tryYinYang(LiShuwenEntity entity, ServerLevel level, long now) {
      CompoundTag data = entity.getPersistentData();
      if (entity.getCurrentMp() < YIN_YANG_MP_COST || now - data.getLong(TAG_LAST_YIN_YANG) < YIN_YANG_COOLDOWN) {
         return false;
      }
      data.putLong(TAG_LAST_YIN_YANG, now);
      data.putLong(TAG_YIN_YANG_UNTIL, now + YIN_YANG_DURATION);
      data.putBoolean(TAG_YIN_YANG_WEAK_NULL, true);
      entity.setCurrentMp(Math.max(0.0, entity.getCurrentMp() - YIN_YANG_MP_COST));
      updateModifier(entity.getAttribute(Attributes.ATTACK_DAMAGE), YIN_YANG_ATTACK_ID, 0.30, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
      entity.triggerStepAnimation();
      VFXServerEffects.spawn(level, "servant_li_shuwen_yinyang", entity, 64.0);
      level.playSound(null, entity.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 0.8F, 0.55F);
      return true;
   }

   private static void tickYinYangCleanup(LiShuwenEntity entity, long now) {
      CompoundTag data = entity.getPersistentData();
      if (data.getLong(TAG_YIN_YANG_UNTIL) > now) {
         if (data.getBoolean(TAG_YIN_YANG_WEAK_NULL) && removeOneHarmfulEffect(entity)) {
            data.putBoolean(TAG_YIN_YANG_WEAK_NULL, false);
         }
         return;
      }
      removeModifier(entity.getAttribute(Attributes.ATTACK_DAMAGE), YIN_YANG_ATTACK_ID);
      data.remove(TAG_YIN_YANG_UNTIL);
      data.remove(TAG_YIN_YANG_WEAK_NULL);
   }

   private static boolean tryBeginWuErDa(LiShuwenEntity entity, LivingEntity target, ServerLevel level, long now) {
      CompoundTag data = entity.getPersistentData();
      ServantNoblePhantasmResourceService.CastDecision resource =
         ServantNoblePhantasmResourceService.evaluateNpcCast(entity, WU_ER_DA_MP_COST);
      int previousCooldown = data.getBoolean(TAG_WU_ER_DA_OVERDRAFT) ? WU_ER_DA_COOLDOWN * 2 : WU_ER_DA_COOLDOWN;
      if (!resource.allowed() || ServantNoblePhantasmResourceService.isOverdraftWeak(entity)
         || now - data.getLong(TAG_LAST_WU_ER_DA) < previousCooldown
         || data.getLong(TAG_WU_ER_DA_RELEASE) > now) {
         return false;
      }
      if (!entity.getSensing().hasLineOfSight(target)) {
         return false;
      }
      data.putLong(TAG_LAST_WU_ER_DA, now);
      data.putLong(TAG_WU_ER_DA_RELEASE, now + WU_ER_DA_WINDUP);
      data.putInt(TAG_WU_ER_DA_TARGET, target.getId());
      data.putDouble(TAG_WU_ER_DA_POWER_SCALE, resource.powerScale());
      data.putBoolean(TAG_WU_ER_DA_OVERDRAFT, resource.overdraft());
      ServantNoblePhantasmResourceService.commitNpcCast(entity, resource);
      entity.setWuErDaTargeting(true);
      entity.faceToward(target.position());
      entity.triggerWuErDaAnimation();
      entity.getNavigation().stop();
      if (WU_ER_DA_WINDUP <= 0) {
         data.remove(TAG_WU_ER_DA_RELEASE);
         data.remove(TAG_WU_ER_DA_TARGET);
         entity.setWuErDaTargeting(false);
         resolveWuErDa(entity, target, level);
         return true;
      }
      ServantCombatSystem.broadcastNoblePhantasmWindup(entity, target, WU_ER_DA_WINDUP, false);
      VFXServerEffects.spawn(level, "servant_li_shuwen_wu_er_da_windup", entity, 96.0);
      level.playSound(null, entity.blockPosition(), SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.HOSTILE, 0.55F, 0.45F);
      return true;
   }

   private static boolean tickWuErDaRelease(LiShuwenEntity entity, ServerLevel level, long now) {
      CompoundTag data = entity.getPersistentData();
      long release = data.getLong(TAG_WU_ER_DA_RELEASE);
      if (release <= 0L) {
         return false;
      }
      if (release > now) {
         LivingEntity target = getTargetById(level, data.getInt(TAG_WU_ER_DA_TARGET));
         if (target != null && target.isAlive()) {
            entity.faceToward(target.position());
            entity.getLookControl().setLookAt(target, 70.0F, 70.0F);
         }
         return true;
      }
      data.remove(TAG_WU_ER_DA_RELEASE);
      entity.setWuErDaTargeting(false);
      LivingEntity target = getTargetById(level, data.getInt(TAG_WU_ER_DA_TARGET));
      data.remove(TAG_WU_ER_DA_TARGET);
      if (target == null || !target.isAlive() || entity.distanceTo(target) > WU_ER_DA_RANGE + 0.75 || !entity.getSensing().hasLineOfSight(target)) {
         return true;
      }
      resolveWuErDa(entity, target, level);
      return true;
   }

   private static void resolveWuErDa(LiShuwenEntity entity, LivingEntity target, ServerLevel level) {
      if (EntityUtils.isImmunePlayerTarget(target)) {
         entity.setTarget(null);
         return;
      }
      resolveWuErDa(entity, target, level, entity.getRandom().nextFloat());
   }

   public static void resolveWuErDa(LivingEntity attacker, LivingEntity target, ServerLevel level, float roll) {
      if (attacker == null || target == null || level == null || EntityUtils.isImmunePlayerTarget(target)) {
         return;
      }
      applyArmorBreak(target, level.getGameTime() + 60L);
      boolean instantDeathTarget = isInstantDeathTarget(target);
      float chance = instantDeathChance(target);
      boolean killed = instantDeathTarget && roll < chance;
      if (HeraclesGodHandHelper.isAdaptedToZabaniya(target)) {
         HeraclesGodHandHelper.applyAdaptedSlow(target, 80);
         killed = false;
      }
      if (killed) {
         if (ArtoriaPendragonCombatHelper.tryNegateCertainHitOrDeath(target, "wu_er_da")) {
            spawnWuErDaImpact(level, target, false);
            return;
         }
         if (HeraclesGodHandHelper.consumeLifeForZabaniya(target)) {
            spawnWuErDaImpact(level, target, true);
            return;
         }
         DamageSource source = wuErDaDamage(attacker);
         target.invulnerableTime = 0;
         target.hurt(source, Math.max(target.getMaxHealth() * 2.0F, 500.0F));
         target.invulnerableTime = 0;
         if (target.isAlive()) {
            target.setHealth(0.0F);
            target.die(source);
         }
      } else {
         float damage = Math.max(200.0F, target.getHealth() * 0.90F);
         target.invulnerableTime = 0;
         target.hurt(wuErDaDamage(attacker), damage);
         target.invulnerableTime = 0;
         applyQiSwallow(target, level.getGameTime() + 20L);
      }
      spawnWuErDaImpact(level, target, killed);
   }

   private static DamageSource wuErDaDamage(LivingEntity attacker) {
      return attacker.damageSources().source(WU_ER_DA, attacker);
   }

   private static boolean tryBajiPunch(LiShuwenEntity entity, LivingEntity target, ServerLevel level, long now, double distance) {
      CompoundTag data = entity.getPersistentData();
      if (distance > 2.8 || now - data.getLong(TAG_LAST_PUNCH) < PUNCH_COOLDOWN) {
         return false;
      }
      data.putLong(TAG_LAST_PUNCH, now);
      entity.faceToward(target.position());
      entity.triggerPunchAnimation();
      dealMartialDamage(entity, target, 1.15F);
      push(target, entity.position().subtract(target.position()).scale(-1.0), 0.45, 0.08);
      spawnMartialHitFx(entity, target);
      return true;
   }

   private static boolean tryShoulderCheck(LiShuwenEntity entity, LivingEntity target, ServerLevel level, long now, double distance) {
      CompoundTag data = entity.getPersistentData();
      if (distance < 2.4 || distance > 6.5 || entity.getCurrentMp() < SHOULDER_CHECK_MP_COST || now - data.getLong(TAG_LAST_SHOULDER) < SHOULDER_COOLDOWN) {
         return false;
      }
      data.putLong(TAG_LAST_SHOULDER, now);
      entity.setCurrentMp(Math.max(0.0, entity.getCurrentMp() - SHOULDER_CHECK_MP_COST));
      Vec3 dir = horizontalDirection(entity, target);
      entity.faceVector(dir);
      entity.triggerStepAnimation();
      entity.setDeltaMovement(dir.x * 1.55, Math.max(entity.getDeltaMovement().y, 0.12), dir.z * 1.55);
      entity.hasImpulse = true;
      dealMartialDamage(entity, target, 1.0F);
      push(target, dir, 1.0, 0.18);
      level.sendParticles(INK, entity.getX(), entity.getY() + 0.35, entity.getZ(), 14, 0.25, 0.15, 0.25, 0.03);
      level.playSound(null, entity.blockPosition(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.HOSTILE, 0.9F, 0.75F);
      return true;
   }

   private static boolean tryTremorPalm(LiShuwenEntity entity, LivingEntity target, ServerLevel level, long now, double distance) {
      CompoundTag data = entity.getPersistentData();
      if (distance > 3.4 || entity.getCurrentMp() < TREMOR_PALM_MP_COST || now - data.getLong(TAG_LAST_TREMOR) < TREMOR_COOLDOWN) {
         return false;
      }
      data.putLong(TAG_LAST_TREMOR, now);
      entity.setCurrentMp(Math.max(0.0, entity.getCurrentMp() - TREMOR_PALM_MP_COST));
      entity.faceToward(target.position());
      entity.triggerPunchAnimation();
      for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, entity.getBoundingBox().inflate(3.2), e -> canHit(entity, e))) {
         dealMartialDamage(entity, living, 0.82F);
         living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1, false, true));
         push(living, living.position().subtract(entity.position()), 0.65, 0.12);
      }
      level.sendParticles(INK, entity.getX(), entity.getY() + 0.08, entity.getZ(), 26, 1.1, 0.06, 1.1, 0.025);
      level.playSound(null, entity.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 0.45F, 1.65F);
      return true;
   }

   private static boolean tryCounterPressure(LiShuwenEntity entity, LivingEntity target, ServerLevel level, long now, double distance) {
      CompoundTag data = entity.getPersistentData();
      if (distance > 3.2 || now - data.getLong(TAG_LAST_COUNTER) < COUNTER_COOLDOWN) {
         return false;
      }
      LivingEntity hurtBy = entity.getLastHurtByMob();
      if (hurtBy != target || entity.tickCount - entity.getLastHurtByMobTimestamp() > 45) {
         return false;
      }
      data.putLong(TAG_LAST_COUNTER, now);
      entity.faceToward(target.position());
      entity.triggerPunchAnimation();
      dealMartialDamage(entity, target, 1.45F);
      target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 50, 0, false, true));
      spawnMartialHitFx(entity, target);
      return true;
   }

   private static void dealMartialDamage(LiShuwenEntity entity, LivingEntity target, float scale) {
      float armorPierceBonus = (float)(target.getAttributeValue(Attributes.ARMOR) * 0.08F);
      float yinYang = entity.getPersistentData().getLong(TAG_YIN_YANG_UNTIL) > entity.level().getGameTime() ? 1.30F : 1.0F;
      float damage = (float)(entity.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.30F * scale * yinYang + armorPierceBonus);
      target.invulnerableTime = 0;
      target.hurt(entity.damageSources().mobAttack(entity), damage);
      target.invulnerableTime = 0;
      if (entity.hasEffect(MobEffects.INVISIBILITY)) {
         entity.removeEffect(MobEffects.INVISIBILITY);
      }
   }

   private static float instantDeathChance(LivingEntity target) {
      return 0.60F;
   }

   private static boolean isInstantDeathTarget(LivingEntity target) {
      if (target == null || !target.isAlive() || EntityUtils.isImmunePlayerTarget(target)) {
         return false;
      }
      return CursedArmHassanCombatHelper.isHumanoidInstantDeathTarget(target);
   }

   private static void applyArmorBreak(LivingEntity target, long until) {
      AttributeInstance armor = target.getAttribute(Attributes.ARMOR);
      if (armor != null && armor.getModifier(ARMOR_BREAK_ID) == null) {
         armor.addTransientModifier(new AttributeModifier(ARMOR_BREAK_ID, -0.30, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
      }
      target.getPersistentData().putLong(TAG_ARMOR_BREAK_UNTIL, until);
      TYPE_MOON_WORLD.queueServerWork(65, () -> {
         if (target.isAlive() && target.getPersistentData().getLong(TAG_ARMOR_BREAK_UNTIL) <= target.level().getGameTime()) {
            removeModifier(target.getAttribute(Attributes.ARMOR), ARMOR_BREAK_ID);
            target.getPersistentData().remove(TAG_ARMOR_BREAK_UNTIL);
         }
      });
   }

   private static void applyQiSwallow(LivingEntity target, long until) {
      target.getPersistentData().putLong(TAG_STUN_UNTIL, until);
      target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 4, false, true, true));
      target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 20, 1, false, true, true));
   }

   private static void clearMentalEffects(LiShuwenEntity entity) {
      List<net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect>> effects = List.of(MobEffects.CONFUSION, MobEffects.BLINDNESS, MobEffects.DARKNESS);
      for (net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect : effects) {
         if (entity.hasEffect(effect)) {
            entity.removeEffect(effect);
         }
      }
      if (!(entity.level() instanceof ServerLevel level) || entity.tickCount % 20 != 0) {
         return;
      }
      for (ServantEntity ally : level.getEntitiesOfClass(ServantEntity.class, entity.getBoundingBox().inflate(20.0), e -> e.isAlive() && e.isAlliedTo(entity))) {
         for (net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect : effects) {
            if (ally.hasEffect(effect)) {
               ally.removeEffect(effect);
            }
         }
      }
   }

   private static boolean removeOneHarmfulEffect(LivingEntity entity) {
      Collection<MobEffectInstance> active = List.copyOf(entity.getActiveEffects());
      for (MobEffectInstance effect : active) {
         if (!effect.getEffect().value().isBeneficial()) {
            entity.removeEffect(effect.getEffect());
            return true;
         }
      }
      return false;
   }

   private static void clearNonServantTargeting(LiShuwenEntity entity, ServerLevel level) {
      for (net.minecraft.world.entity.Mob mob : level.getEntitiesOfClass(net.minecraft.world.entity.Mob.class, entity.getBoundingBox().inflate(18.0), mob -> mob.getTarget() == entity && !(mob instanceof ServantEntity))) {
         mob.setTarget(null);
      }
   }

   private static LivingEntity getTargetById(ServerLevel level, int id) {
      return level.getEntity(id) instanceof LivingEntity living ? living : null;
   }

   private static boolean canHit(LiShuwenEntity entity, LivingEntity target) {
      return target != entity && target.isAlive() && !target.isAlliedTo(entity) && !EntityUtils.isImmunePlayerTarget(target);
   }

   private static Vec3 horizontalDirection(LivingEntity from, LivingEntity to) {
      Vec3 dir = to.position().subtract(from.position()).multiply(1.0, 0.0, 1.0);
      if (dir.lengthSqr() < 1.0E-4) {
         dir = from.getLookAngle().multiply(1.0, 0.0, 1.0);
      }
      return dir.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : dir.normalize();
   }

   private static void push(LivingEntity target, Vec3 direction, double horizontal, double vertical) {
      Vec3 dir = new Vec3(direction.x, 0.0, direction.z);
      if (dir.lengthSqr() < 1.0E-4) {
         dir = target.getLookAngle().multiply(1.0, 0.0, 1.0).reverse();
      }
      dir = dir.normalize();
      target.push(dir.x * horizontal, vertical, dir.z * horizontal);
      target.hurtMarked = true;
      target.hasImpulse = true;
   }

   private static void spawnCircleDodgeFx(LiShuwenEntity entity) {
      if (!(entity.level() instanceof ServerLevel level)) {
         return;
      }
      level.sendParticles(ParticleTypes.CLOUD, entity.getX(), entity.getY() + entity.getBbHeight() * 0.45, entity.getZ(), 12, 0.18, 0.25, 0.18, 0.035);
      level.sendParticles(INK, entity.getX(), entity.getY() + 0.08, entity.getZ(), 18, 0.45, 0.04, 0.45, 0.01);
      level.playSound(null, entity.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 0.45F, 0.55F);
   }

   private static void spawnWuErDaImpact(ServerLevel level, LivingEntity target, boolean killed) {
      VFXServerEffects.spawn(level, "servant_li_shuwen_wu_er_da_impact", target.position(), 96.0);
      level.sendParticles(killed ? ParticleTypes.FLASH : ParticleTypes.DAMAGE_INDICATOR, target.getX(), target.getY() + target.getBbHeight() * 0.58, target.getZ(), killed ? 2 : 8, 0.1, 0.18, 0.1, 0.0);
      level.sendParticles(INK, target.getX(), target.getY() + target.getBbHeight() * 0.55, target.getZ(), killed ? 46 : 28, 0.45, 0.42, 0.45, 0.05);
      level.playSound(null, target.blockPosition(), killed ? SoundEvents.WITHER_DEATH : SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.HOSTILE, killed ? 0.9F : 0.8F, killed ? 0.85F : 0.55F);
   }

   private static void updateModifier(AttributeInstance attribute, ResourceLocation id, double amount, AttributeModifier.Operation operation) {
      if (attribute == null) {
         return;
      }
      AttributeModifier existing = attribute.getModifier(id);
      if (existing != null) {
         if (Math.abs(existing.amount() - amount) < 1.0E-6 && existing.operation() == operation) {
            return;
         }
         attribute.removeModifier(id);
      }
      attribute.addTransientModifier(new AttributeModifier(id, amount, operation));
   }

   private static void removeModifier(AttributeInstance attribute, ResourceLocation id) {
      if (attribute != null && attribute.getModifier(id) != null) {
         attribute.removeModifier(id);
      }
   }
}
