package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.block.ModBlocks;
import net.xxxjk.TYPE_MOON_WORLD.block.custom.UBWWeaponBlock;
import net.xxxjk.TYPE_MOON_WORLD.block.entity.UBWWeaponBlockEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.UBWProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.TsumukariWaveProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.magic.MagicCircuitColorHelper;
import net.xxxjk.TYPE_MOON_WORLD.magic.MuramasaDissolutionService;
import net.xxxjk.TYPE_MOON_WORLD.magic.MuramasaSlashHandler;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantAiContext;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantCombatTempoService;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantNavigationHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatPhase;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatSystem;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantIdentityHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantTraitTag;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXServerEffects;

public final class MuramasaCombatHelper {
   private static final String LAST_PASSIVE_STATE_TICK = "MuramasaLastPassiveStateTick";
   private static final String PROJECTED_TAG = "MuramasaProjectedWeapon";
   private static final String WEAPON_INDEX = "MuramasaWeaponIndex";
   private static final String LAST_WEAPON_TICK = "MuramasaLastWeaponTick";
   private static final String NP_USED = "MuramasaNoblePhantasmUsed";
   private static final String NP_PENDING = "MuramasaNoblePhantasmPending";
   private static final String NP_LAST = "MuramasaNoblePhantasmLastTick";
   private static final String CHARGE_TICK = "MuramasaTsumukariChargeTick";
   private static final String CHARGE_FORCED_DEATH = "MuramasaTsumukariForcedDeath";
   private static final String TSUMUKARI_RELEASED = "MuramasaTsumukariReleased";
   private static final String TRIAL_UNTIL = "MuramasaTrialUntil";
   private static final String KARMA_UNTIL = "MuramasaKarmaEyeUntil";
   private static final String FLAME_UNTIL = "MuramasaFlameUntil";
   private static final String TEMPER_UNTIL = "MuramasaTemperUntil";
   private static final String TSUMUKARI_UNTIL = "MuramasaTsumukariUntil";
   private static final String ACTION_LOCK_UNTIL = "MuramasaActionLockUntil";
   private static final String CHARGE_TARGET = "MuramasaChargeTarget";
   private static final String CHARGE_LOOK_X = "MuramasaChargeLookX";
   private static final String CHARGE_LOOK_Z = "MuramasaChargeLookZ";
   private static final String LAST_CONTACT = "MuramasaLastContact";
   private static final String LAST_ATTACK = "MuramasaLastAttack";
   private static final String LAST_SKILL = "MuramasaLastSkill";
   private static final String FIELD_ACTIVE = "MuramasaFieldActive";
   private static final String FIELD_RESTORE_TICK = "MuramasaFieldRestoreTick";
   private static final String FIELD_TERRAIN = "MuramasaFieldTerrain";
   private static final String FIELD_CENTER_X = "MuramasaFieldCenterX";
   private static final String FIELD_CENTER_Z = "MuramasaFieldCenterZ";
   private static final String FIELD_COLLAPSING = "MuramasaFieldCollapsing";
   private static final ResourceLocationLike DAMAGE_ID = new ResourceLocationLike("muramasa_damage");
   private static final ResourceLocationLike SPEED_ID = new ResourceLocationLike("muramasa_speed");
   private static final ResourceLocationLike KNOCKBACK_ID = new ResourceLocationLike("muramasa_knockback");
   private static final int WEAPON_ROTATION_TICKS = 1200;
   private static final int CHARGE_TICKS = 30;
   private static final int TSUMUKARI_SPECIAL_CHARGE_PERCENT = 10;
   private static final int NP_EXPAND_TICKS = 200;
   private static final int NP_COLLAPSE_TICKS = 80;

   private MuramasaCombatHelper() {
   }

   public static void tickPassive(SenkoMuramasaEntity entity) {
      if (!(entity.level() instanceof ServerLevel level) || !entity.isAlive()) {
         return;
      }
      long now = level.getGameTime();
      CompoundTag data = entity.getPersistentData();
      if (data.contains(LAST_PASSIVE_STATE_TICK) && data.getLong(LAST_PASSIVE_STATE_TICK) == now) return;
      data.putLong(LAST_PASSIVE_STATE_TICK, now);
      MuramasaDissolutionService.tick(entity);
      if (!entity.isAlive()) {
         return;
      }
      AttributeInstance armor = entity.getAttribute(Attributes.ARMOR);
      if (armor != null) {
         armor.setBaseValue(20.0);
      }
      refreshModifiers(entity, now);
      rotateWeapon(entity, now);
      if (entity.getPersistentData().getBoolean(FIELD_ACTIVE)
         && now >= entity.getPersistentData().getLong(FIELD_RESTORE_TICK)) {
         restoreFieldTerrain(entity);
      }
   }

   public static boolean tick(SenkoMuramasaEntity entity, ServantAiContext context) {
      if (!(entity.level() instanceof ServerLevel level) || !entity.isAlive()) {
         return false;
      }
      tickPassive(entity);
      CompoundTag data = entity.getPersistentData();
      long now = context.gameTick();
      if (data.getInt(CHARGE_TICK) > 0) {
         tickCharge(entity, level, now);
         return true;
      }
      if (data.getBoolean(NP_PENDING)) {
         entity.getNavigation().stop();
         return true;
      }
      LivingEntity target = context.target();
      if (target == null || !target.isAlive() || EntityUtils.isImmunePlayerTarget(target)) {
         return false;
      }
      if (ServantCombatSystem.cannotAct(entity)) {
         return false;
      }

      entity.getLookControl().setLookAt(target, 55.0F, 45.0F);
      entity.faceToward(target.position().add(0.0, target.getBbHeight() * 0.42, 0.0));
      ServantCombatPhase phase = ServantCombatSystem.getPhase(entity);
      boolean decisive = phase == ServantCombatPhase.DECISIVE
         || entity.getHealth() <= entity.getMaxHealth() * 0.30F;
      double distance = entity.distanceTo(target);
      boolean lineOfSight = entity.getSensing().hasLineOfSight(target);

      if (decisive && tryStartTsumukariCharge(entity, target, level, data, now)) {
         return true;
      }
      if (phase != ServantCombatPhase.PROBING && trySummonTsumukari(entity, target, level, data, now)) {
         return true;
      }
      if (tryMuramasaSkillRotation(entity, target, phase, level, data, now, distance, lineOfSight)) {
         return true;
      }
      if (data.getLong(ACTION_LOCK_UNTIL) > now || entity.isPerformingAction()) {
         entity.getNavigation().stop();
         return true;
      }
      if (distance > 4.35) {
         entity.setSprinting(decisive);
         ServantNavigationHelper.moveToTargetThrottled(
            entity, target, decisive ? 1.25 : 1.12, now, 5, 0.2, "MuramasaCombat");
         return true;
      }
      if (!lineOfSight) {
         ServantNavigationHelper.moveToTargetThrottled(entity, target, 1.15, now, 5, 0.2, "MuramasaCombatLos");
         return true;
      }
      entity.getNavigation().stop();
      return false;
   }

   /** Compatibility entry point for older entity integrations. */
   public static void tick(SenkoMuramasaEntity entity) {
      tickPassive(entity);
   }

   public static void tickCharge(SenkoMuramasaEntity entity) {
      if (!(entity.level() instanceof ServerLevel level) || !entity.isAlive()) {
         return;
      }
      tickCharge(entity, level, level.getGameTime());
   }

   public static boolean isCharging(SenkoMuramasaEntity entity) {
      return entity.getPersistentData().getInt(CHARGE_TICK) > 0;
   }

   public static void prepareAttack(SenkoMuramasaEntity entity) {
      if (entity.level().isClientSide()) return;
      long now = entity.level().getGameTime();
      AttributeInstance attack = entity.getAttribute(Attributes.ATTACK_DAMAGE);
      if (attack != null) {
         removeModifier(attack, DAMAGE_ID.id());
      }
   }

   public static void prepareAttack(SenkoMuramasaEntity entity, Entity target) {
      prepareAttack(entity);
      AttributeInstance attack = entity.getAttribute(Attributes.ATTACK_DAMAGE);
      if (attack == null || !(target instanceof LivingEntity living)) return;
      double multiplier = 0.0;
      long now = entity.level().getGameTime();
      if (now < entity.getPersistentData().getLong(TRIAL_UNTIL)) {
         multiplier += 0.30;
      }
      if (isSword(entity.getMainHandItem())) {
         multiplier += 0.20;
      }
      if (isRulerOrKing(living)) {
         multiplier += 0.30;
      }
      if (now < entity.getPersistentData().getLong(KARMA_UNTIL)) {
         multiplier += living.isBlocking() ? 0.50 : 0.0;
      }
      if (multiplier > 0.0) {
         attack.addTransientModifier(new AttributeModifier(DAMAGE_ID.id(), multiplier, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
      }
   }

   public static boolean shouldBypassDefense(SenkoMuramasaEntity entity, LivingEntity target) {
      long now = entity.level().getGameTime();
      CompoundTag data = entity.getPersistentData();
      if (now < data.getLong(KARMA_UNTIL)) {
         return true;
      }
      return now < data.getLong(TRIAL_UNTIL) && entity.getRandom().nextFloat() < 0.30F;
   }

   public static boolean performPrecisionStrike(SenkoMuramasaEntity entity, LivingEntity target) {
      long now = entity.level().getGameTime();
      // The normal attack path installs temporary attribute modifiers before
      // choosing between vanilla damage and the defense-bypassing strike.
      // Clear them here so Karma Eye's explicit multipliers are not doubled.
      prepareAttack(entity);
      double damage = precisionStrikeDamage(
         entity,
         target,
         now < entity.getPersistentData().getLong(TRIAL_UNTIL),
         isSword(entity.getMainHandItem()),
         now < entity.getPersistentData().getLong(KARMA_UNTIL)
      );
      applyNoDefenseDamage(entity, target, (float)damage);
      entity.getPersistentData().putLong(LAST_CONTACT, now);
      entity.getPersistentData().putLong(LAST_ATTACK, now);
      ServantCombatTempoService.recordContact(entity, target, ServantCombatTempoService.ContactType.DAMAGE, now);
      return true;
   }

   public static void afterAttack(SenkoMuramasaEntity entity, Entity target, boolean hit) {
      if (hit && target instanceof LivingEntity living) {
         long now = entity.level().getGameTime();
         if (isTsumukariActive(entity, now)) {
            applyNoDefenseDamage(entity, living, 30.0F);
            entity.setCurrentMp(Math.min(entity.getMaxMp(), entity.getCurrentMp() + 5.0));
         }
         if (now < entity.getPersistentData().getLong(FLAME_UNTIL)) {
            entity.setCurrentMp(Math.min(entity.getMaxMp(), entity.getCurrentMp() + 10.0));
         }
         if (now < entity.getPersistentData().getLong(TEMPER_UNTIL)) {
            living.igniteForSeconds(4.0F);
            levelTemperHitFx(entity, living);
         }
         entity.getPersistentData().putLong(LAST_CONTACT, now);
         entity.getPersistentData().putLong(LAST_ATTACK, now);
         prepareAttack(entity);
      }
   }

   private static void refreshModifiers(SenkoMuramasaEntity entity, long now) {
      AttributeInstance attack = entity.getAttribute(Attributes.ATTACK_DAMAGE);
      if (attack != null) {
         removeModifier(attack, DAMAGE_ID.id());
         if (now < entity.getPersistentData().getLong(TRIAL_UNTIL)) {
            attack.addTransientModifier(new AttributeModifier(DAMAGE_ID.id(), 0.30, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
         }
      }
      AttributeInstance speed = entity.getAttribute(Attributes.ATTACK_SPEED);
      if (speed != null) {
         removeModifier(speed, SPEED_ID.id());
         if (now < entity.getPersistentData().getLong(TSUMUKARI_UNTIL)) {
            speed.addTransientModifier(new AttributeModifier(SPEED_ID.id(), 0.30, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
         }
      }
      AttributeInstance knockback = entity.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
      if (knockback != null) {
         removeModifier(knockback, KNOCKBACK_ID.id());
         if (now < entity.getPersistentData().getLong(TEMPER_UNTIL)) {
            knockback.addTransientModifier(new AttributeModifier(KNOCKBACK_ID.id(), 1.0, AttributeModifier.Operation.ADD_VALUE));
         }
      }
   }

   private static boolean tryMuramasaSkillRotation(
      SenkoMuramasaEntity entity, LivingEntity target, ServantCombatPhase phase,
      ServerLevel level, CompoundTag data, long now, double distance, boolean lineOfSight) {
      if (data.getLong(ACTION_LOCK_UNTIL) > now || entity.isPerformingAction()) {
         return true;
      }
      boolean defensiveTarget = target.isBlocking() || target.getArmorValue() >= 12.0F
         || target.getMaxHealth() >= 180.0F;

      // Muramasa first tests the blade and only then commits to reading the
      // opponent.  This preserves his probing -> analysis -> execution rhythm.
      if (phase == ServantCombatPhase.PROBING
         && now >= data.getLong("MuramasaTrialCooldown")
         && entity.getCurrentMp() >= 10.0) {
         data.putLong(TRIAL_UNTIL, now + 400L);
         data.putLong("MuramasaTrialCooldown", now + 500L);
         entity.setCurrentMp(entity.getCurrentMp() - 10.0);
         lockAction(entity, data, now, 10L);
         entity.triggerNamedActionAnimation("charge");
         spawnSkillFx(level, entity, net.minecraft.core.particles.ParticleTypes.CRIT, 18);
         return true;
      }
      if (phase != ServantCombatPhase.PROBING && defensiveTarget
         && now >= data.getLong("MuramasaKarmaCooldown")
         && entity.getCurrentMp() >= 15.0
         && lineOfSight) {
         data.putLong(KARMA_UNTIL, now + 267L);
         data.putLong("MuramasaKarmaCooldown", now + 600L);
         entity.setCurrentMp(entity.getCurrentMp() - 15.0);
         lockAction(entity, data, now, 12L);
         entity.triggerNamedActionAnimation("charge");
         spawnSkillFx(level, entity, net.minecraft.core.particles.ParticleTypes.ENCHANT, 24);
         return true;
      }
      if (phase != ServantCombatPhase.PROBING
         && distance >= 7.0
         && lineOfSight
         && now >= data.getLong("MuramasaProjectionVolleyCooldown")
         && entity.getCurrentMp() >= 25.0) {
         data.putLong("MuramasaProjectionVolleyCooldown", now + 400L);
         entity.setCurrentMp(entity.getCurrentMp() - 25.0);
         lockAction(entity, data, now, 14L);
         entity.triggerNamedActionAnimation("charge");
         performProjectionVolley(entity, target, level);
         return true;
      }
      if (phase != ServantCombatPhase.PROBING
         && now >= data.getLong("MuramasaTemperCooldown")
         && entity.getCurrentMp() >= 20.0
         && (entity.getHealth() <= entity.getMaxHealth() * 0.72F || distance <= 4.5)) {
         data.putLong(TEMPER_UNTIL, now + 320L);
         data.putLong("MuramasaTemperCooldown", now + 600L);
         entity.setCurrentMp(entity.getCurrentMp() - 20.0);
         lockAction(entity, data, now, 10L);
         entity.triggerNamedActionAnimation("charge");
         entity.addEffect(new net.minecraft.world.effect.MobEffectInstance(
            net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE, 320, 0, false, true, true));
         entity.addEffect(new net.minecraft.world.effect.MobEffectInstance(
            net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 320, 0, false, true, true));
         spawnSkillFx(level, entity, net.minecraft.core.particles.ParticleTypes.FLAME, 32);
         return true;
      }
      if (phase != ServantCombatPhase.PROBING
         && distance <= 10.0
         && now >= data.getLong("MuramasaKarmaSlashCooldown")
         && entity.getCurrentMp() >= 35.0
         && lineOfSight) {
         data.putLong("MuramasaKarmaSlashCooldown", now + 500L);
         entity.setCurrentMp(entity.getCurrentMp() - 35.0);
         lockAction(entity, data, now, 12L);
         entity.triggerNamedActionAnimation("attack");
         performKarmaSlash(entity, target, level);
         return true;
      }
      if (phase != ServantCombatPhase.PROBING
         && now >= data.getLong("MuramasaSwordFieldCooldown")
         && entity.getCurrentMp() >= 40.0
         && (distance <= 8.0 || nearbyEnemyCount(entity, level, 8.0) >= 2)) {
         data.putLong("MuramasaSwordFieldCooldown", now + 600L);
         entity.setCurrentMp(entity.getCurrentMp() - 40.0);
         lockAction(entity, data, now, 20L);
         entity.triggerNamedActionAnimation("charge");
         performSwordField(entity, level);
         return true;
      }
      if (now >= data.getLong("MuramasaFlameCooldown")
         && entity.getCurrentMp() <= entity.getMaxMp() * 0.55) {
         data.putLong(FLAME_UNTIL, now + 400L);
         data.putLong("MuramasaFlameCooldown", now + 600L);
         entity.setCurrentMp(Math.min(entity.getMaxMp(), entity.getCurrentMp() + 100.0));
         lockAction(entity, data, now, 8L);
         entity.triggerNamedActionAnimation("charge");
         spawnSkillFx(level, entity, net.minecraft.core.particles.ParticleTypes.FLAME, 28);
         return true;
      }
      if (phase != ServantCombatPhase.PROBING
         && data.getBoolean(NP_USED)
         && !data.getBoolean(NP_PENDING)
         && !data.getBoolean(TSUMUKARI_RELEASED)
         && entity.getMainHandItem().is(ModItems.TSUMUKARI_MURAMASA.get())
         && now >= data.getLong("MuramasaTsumukariCooldown")
         && entity.getCurrentMp() >= 25.0
         && distance <= 8.0) {
         data.putLong(TSUMUKARI_UNTIL, now + 533L);
         data.putLong("MuramasaTsumukariCooldown", now + 600L);
         entity.setCurrentMp(entity.getCurrentMp() - 25.0);
         lockAction(entity, data, now, 12L);
         entity.triggerNamedActionAnimation("charge");
         spawnSkillFx(level, entity, net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME, 20);
         return true;
      }
      return false;
   }

   private static boolean isTsumukariActive(SenkoMuramasaEntity entity, long now) {
      CompoundTag data = entity.getPersistentData();
      return data.getBoolean(NP_USED)
         && !data.getBoolean(NP_PENDING)
         && !data.getBoolean(TSUMUKARI_RELEASED)
         && now < data.getLong(TSUMUKARI_UNTIL)
         && entity.getMainHandItem().is(ModItems.TSUMUKARI_MURAMASA.get());
   }

   private static void lockAction(SenkoMuramasaEntity entity, CompoundTag data, long now, long duration) {
      data.putLong(ACTION_LOCK_UNTIL, now + Math.max(1L, duration));
      data.putLong(LAST_SKILL, now);
      entity.getNavigation().stop();
   }

   private static void spawnSkillFx(ServerLevel level, SenkoMuramasaEntity entity,
                                    net.minecraft.core.particles.ParticleOptions particle, int count) {
      level.sendParticles(particle, entity.getX(), entity.getY() + entity.getBbHeight() * 0.58,
         entity.getZ(), count, 0.42, 0.55, 0.42, 0.04);
      level.playSound(null, entity.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE,
         SoundSource.HOSTILE, 0.65F, 1.15F);
   }

   public static boolean performProjectionVolley(
      SenkoMuramasaEntity entity, LivingEntity target, ServerLevel level) {
      if (entity == null || target == null || level == null || !entity.isAlive() || !target.isAlive()) {
         return false;
      }
      Vec3 forward = target.position().add(0.0, target.getBbHeight() * 0.45, 0.0)
         .subtract(entity.getEyePosition());
      if (forward.lengthSqr() < 1.0E-4) {
         forward = entity.getLookAngle();
      }
      forward = forward.normalize();
      for (int i = 0; i < 5; i++) {
         Vec3 side = new Vec3(-forward.z, 0.0, forward.x);
         double sideOffset = (i - 2) * 0.42;
         Vec3 spawn = entity.getEyePosition().add(forward.scale(0.45)).add(side.scale(sideOffset));
         UBWProjectileEntity sword = new UBWProjectileEntity(
            level, entity, projectedStack(Items.IRON_SWORD));
         sword.setPos(spawn.x, spawn.y, spawn.z);
         Vec3 aim = target.position().add(0.0, target.getBbHeight() * 0.45, 0.0)
            .add(side.scale((i - 2) * 0.35)).subtract(spawn).normalize();
         sword.setDeltaMovement(aim.scale(2.65 + i * 0.06));
         level.addFreshEntity(sword);
      }
      level.sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT,
         entity.getX(), entity.getY() + entity.getBbHeight() * 0.62, entity.getZ(),
         45, 0.55, 0.65, 0.55, 0.1);
      level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME,
         entity.getX(), entity.getY() + entity.getBbHeight() * 0.55, entity.getZ(),
         18, 0.35, 0.45, 0.35, 0.04);
      level.playSound(null, entity.blockPosition(), SoundEvents.TRIDENT_THROW.value(),
         SoundSource.HOSTILE, 0.8F, 1.35F);
      return true;
   }

   public static boolean performKarmaSlash(
      SenkoMuramasaEntity entity, LivingEntity target, ServerLevel level) {
      if (entity == null || target == null || level == null || !entity.isAlive() || !target.isAlive()) {
         return false;
      }
      removeBeneficialEffects(target);
      applyNoDefenseDamage(entity, target, 42.0F);
      Vec3 origin = entity.position().add(0.0, entity.getBbHeight() * 0.55, 0.0);
      Vec3 end = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
      Vec3 delta = end.subtract(origin);
      for (double distance = 0.0; distance <= delta.length(); distance += 0.45) {
         Vec3 point = origin.add(delta.normalize().scale(distance));
         level.sendParticles(net.minecraft.core.particles.ParticleTypes.SWEEP_ATTACK,
            point.x, point.y, point.z, 1, 0.0, 0.0, 0.0, 0.0);
         level.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME,
            point.x, point.y, point.z, 2, 0.08, 0.08, 0.08, 0.03);
      }
      level.playSound(null, entity.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
         SoundSource.HOSTILE, 1.1F, 0.65F);
      return true;
   }

   public static boolean performSwordField(SenkoMuramasaEntity entity, ServerLevel level) {
      if (entity == null || level == null || !entity.isAlive()) {
         return false;
      }
      Vec3 center = entity.position();
      for (int i = 0; i < 28; i++) {
         double angle = Math.PI * 2.0 * i / 28.0;
         double radius = 2.0 + (i % 4) * 1.9;
         double x = center.x + Math.cos(angle) * radius;
         double z = center.z + Math.sin(angle) * radius;
         level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
            x, center.y + 0.8 + (i % 3) * 0.35, z, 3, 0.12, 0.35, 0.12, 0.06);
         level.sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT,
            x, center.y + 0.9, z, 3, 0.16, 0.3, 0.16, 0.08);
      }
      level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME,
         center.x, center.y + 0.5, center.z, 100, 8.0, 0.65, 8.0, 0.08);
      TYPE_MOON_WORLD.queueServerWork(15, () -> {
         if (!entity.isAlive() || entity.level() != level) {
            return;
         }
         AABB area = entity.getBoundingBox().inflate(8.0);
         for (LivingEntity living : level.getEntitiesOfClass(
            LivingEntity.class, area,
            candidate -> candidate != entity && candidate.isAlive()
               && !entity.isAlliedTo(candidate) && !EntityUtils.isImmunePlayerTarget(candidate))) {
            applyNoDefenseDamage(entity, living, 28.0F);
            living.igniteForSeconds(4.0F);
         }
         level.sendParticles(net.minecraft.core.particles.ParticleTypes.SNOWFLAKE,
            entity.getX(), entity.getY() + 1.0, entity.getZ(), 140, 8.0, 1.1, 8.0, 0.1);
         level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME,
            entity.getX(), entity.getY() + 0.8, entity.getZ(), 90, 7.0, 0.7, 7.0, 0.08);
         level.playSound(null, entity.blockPosition(), SoundEvents.GLASS_BREAK,
            SoundSource.HOSTILE, 1.3F, 0.7F);
      });
      return true;
   }

   public static boolean performTemper(SenkoMuramasaEntity entity) {
      if (entity == null || !(entity.level() instanceof ServerLevel level) || !entity.isAlive()) {
         return false;
      }
      long now = level.getGameTime();
      entity.getPersistentData().putLong(TEMPER_UNTIL, now + 320L);
      entity.addEffect(new net.minecraft.world.effect.MobEffectInstance(
         net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE, 320, 0, false, true, true));
      entity.addEffect(new net.minecraft.world.effect.MobEffectInstance(
         net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 320, 0, false, true, true));
      level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME,
         entity.getX(), entity.getY() + entity.getBbHeight() * 0.6, entity.getZ(),
         32, 0.42, 0.55, 0.42, 0.06);
      return true;
   }

   private static int nearbyEnemyCount(SenkoMuramasaEntity entity, ServerLevel level, double radius) {
      return level.getEntitiesOfClass(
         LivingEntity.class, entity.getBoundingBox().inflate(radius),
         candidate -> candidate != entity && candidate.isAlive()
            && !entity.isAlliedTo(candidate) && !EntityUtils.isImmunePlayerTarget(candidate)
      ).size();
   }

   private static void levelTemperHitFx(SenkoMuramasaEntity entity, LivingEntity target) {
      if (!(entity.level() instanceof ServerLevel level)) {
         return;
      }
      level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME,
         target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
         10, 0.25, 0.35, 0.25, 0.04);
      level.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME,
         target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
         5, 0.18, 0.25, 0.18, 0.03);
   }

   private static void rotateWeapon(SenkoMuramasaEntity entity, long now) {
      CompoundTag data = entity.getPersistentData();
      if (data.getBoolean(NP_USED)) {
         return;
      }
      if (entity.isPerformingAction() || entity.isAttackSwinging()
         || data.getLong(ACTION_LOCK_UNTIL) > now) {
         return;
      }
      if (!isProjected(entity.getMainHandItem())) {
         setProjectedWeapon(entity, ModItems.MURAMASA.get());
      }
      if (data.getLong(LAST_WEAPON_TICK) == 0L) {
         data.putLong(LAST_WEAPON_TICK, now);
         data.putInt(WEAPON_INDEX, -1);
         return;
      }
      if (now - data.getLong(LAST_WEAPON_TICK) < WEAPON_ROTATION_TICKS) {
         return;
      }
      data.putLong(LAST_WEAPON_TICK, now);
      int index = (data.getInt(WEAPON_INDEX) + 1) % 4;
      data.putInt(WEAPON_INDEX, index);
      ItemStack[] weapons = {
         new ItemStack(ModItems.WAKIZASHI.get()),
         new ItemStack(ModItems.KATANA.get()),
         new ItemStack(ModItems.NODACHI.get()),
         new ItemStack(ModItems.MURAMASA.get())
      };
      ItemStack stack = weapons[index];
      project(stack);
      entity.setItemInHand(InteractionHand.MAIN_HAND, stack);
   }

   private static boolean trySummonTsumukari(
      SenkoMuramasaEntity entity, LivingEntity target, ServerLevel level, CompoundTag data, long now) {
      if (data.getBoolean(NP_USED)
         || now - data.getLong(NP_LAST) < 600L
         || entity.getCurrentMp() < 50.0
         || !entity.hasMasterNoblePhantasmPermission()
         || !entity.getSensing().hasLineOfSight(target)
         || entity.distanceTo(target) > 25.0) {
         return false;
      }
      summonTsumukari(entity, level);
      lockAction(entity, data, now, 18L);
      return true;
   }

   private static boolean tryStartTsumukariCharge(
      SenkoMuramasaEntity entity, LivingEntity target, ServerLevel level, CompoundTag data, long now) {
      if (!data.getBoolean(NP_USED)
         || data.getBoolean(TSUMUKARI_RELEASED)
         || data.getBoolean(NP_PENDING)
         || data.getInt(CHARGE_TICK) > 0
         || entity.getHealth() >= entity.getMaxHealth() * 0.30F
         || now - data.getLong(NP_LAST) < 20L
         || entity.isPerformingAction()) {
         return false;
      }
      startCharge(entity, target, level);
      return true;
   }

   private static void summonTsumukari(SenkoMuramasaEntity entity, ServerLevel level) {
      CompoundTag data = entity.getPersistentData();
      data.putBoolean(NP_USED, true);
      data.putBoolean(NP_PENDING, true);
      data.putLong(NP_LAST, level.getGameTime());
      entity.setCurrentMp(Math.max(0.0, entity.getCurrentMp() - 50.0));
      entity.triggerNamedActionAnimation("np");
      ServantVoiceHelper.tryPlayMuramasaNp(entity);
      entity.getNavigation().stop();
      // The field starts at the same moment as the Noble Phantasm voice.
      finishTsumukariSummon(entity);
   }

   private static void finishTsumukariSummon(SenkoMuramasaEntity entity) {
      if (!entity.isAlive() || !(entity.level() instanceof ServerLevel level)) {
         return;
      }
      CompoundTag data = entity.getPersistentData();
      if (!data.getBoolean(NP_PENDING)) {
         return;
      }
      VFXServerEffects.spawnOriented(level, "muramasa_no_gen_kensai",
         entity.position().add(0.0, 0.1, 0.0), entity.getLookAngle(), 128.0);
      spawnMuramasaFieldEffects(level, entity);
      level.playSound(null, entity.blockPosition(), SoundEvents.END_PORTAL_SPAWN, SoundSource.HOSTILE, 0.8F, 0.7F);
      data.putLong(ACTION_LOCK_UNTIL, level.getGameTime() + NP_EXPAND_TICKS + NP_COLLAPSE_TICKS + 2L);
      TYPE_MOON_WORLD.queueServerWork(NP_EXPAND_TICKS, () -> revealTsumukari(entity));
   }

   private static void revealTsumukari(SenkoMuramasaEntity entity) {
      if (!entity.isAlive() || !(entity.level() instanceof ServerLevel level)) {
         return;
      }
      CompoundTag data = entity.getPersistentData();
      if (!data.getBoolean(NP_PENDING)) {
         return;
      }
      expandMuramasaTerrain(level, entity, 25);
      data.putBoolean(FIELD_COLLAPSING, true);
      setProjectedWeapon(entity, ModItems.TSUMUKARI_MURAMASA.get());
      shatterMuramasaFieldSwords(level, entity);
      scheduleMuramasaCollapse(level, entity, 0);
      scheduleTsumukariHandEffects(level, entity, 0);
      TYPE_MOON_WORLD.queueServerWork(NP_COLLAPSE_TICKS, () -> completeTsumukariSummon(entity));
      level.playSound(null, entity.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.HOSTILE, 0.75F, 0.55F);
   }

   private static void completeTsumukariSummon(SenkoMuramasaEntity entity) {
      if (!entity.isAlive() || !(entity.level() instanceof ServerLevel level)) {
         return;
      }
      CompoundTag data = entity.getPersistentData();
      if (!data.getBoolean(NP_PENDING)) {
         return;
      }
      restoreFieldTerrain(entity);
      data.putBoolean(NP_PENDING, false);
      level.playSound(null, entity.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.HOSTILE, 0.75F, 0.55F);
   }

   private static void startCharge(SenkoMuramasaEntity entity, LivingEntity target, ServerLevel level) {
      CompoundTag data = entity.getPersistentData();
      data.putInt(CHARGE_TICK, 1);
      data.putBoolean(CHARGE_FORCED_DEATH, false);
      if (target != null) {
         data.putUUID(CHARGE_TARGET, target.getUUID());
      } else {
         data.remove(CHARGE_TARGET);
      }
      Vec3 look = entity.getLookAngle().multiply(1.0, 0.0, 1.0);
      if (look.lengthSqr() < 1.0E-4) {
         look = new Vec3(0.0, 0.0, 1.0);
      } else {
         look = look.normalize();
      }
      data.putDouble(CHARGE_LOOK_X, look.x);
      data.putDouble(CHARGE_LOOK_Z, look.z);
      data.putLong(ACTION_LOCK_UNTIL, level.getGameTime() + CHARGE_TICKS + 2L);
      entity.getNavigation().stop();
      entity.triggerNamedActionAnimation("charge");
      ServantVoiceHelper.tryPlayMuramasaTsumukari(entity);
      level.playSound(null, entity.blockPosition(), SoundEvents.FLINTANDSTEEL_USE, SoundSource.HOSTILE, 0.8F, 0.6F);
   }

   private static void tickCharge(SenkoMuramasaEntity entity, ServerLevel level, long now) {
      CompoundTag data = entity.getPersistentData();
      int tick = data.getInt(CHARGE_TICK);
      if (tick >= CHARGE_TICKS) {
         int percent = 100;
         releaseTsumukari(entity, level, percent);
         data.remove(CHARGE_TICK);
         data.remove(CHARGE_TARGET);
         data.remove(CHARGE_LOOK_X);
         data.remove(CHARGE_LOOK_Z);
         return;
      }
      entity.getNavigation().stop();
      entity.setDeltaMovement(entity.getDeltaMovement().multiply(0.18, 1.0, 0.18));
      if (data.hasUUID(CHARGE_TARGET)) {
         Entity locked = level.getEntity(data.getUUID(CHARGE_TARGET));
         if (locked instanceof LivingEntity living && living.isAlive()) {
            entity.faceToward(living.position().add(0.0, living.getBbHeight() * 0.45, 0.0));
            entity.getLookControl().setLookAt(living, 180.0F, 180.0F);
         }
      } else {
         entity.faceVector(new Vec3(data.getDouble(CHARGE_LOOK_X), 0.0, data.getDouble(CHARGE_LOOK_Z)));
      }
      data.putInt(CHARGE_TICK, tick + 1);
      if (tick % 5 == 0) {
         level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME, entity.getX(), entity.getY() + 1.0, entity.getZ(), 10, 0.5, 0.6, 0.5, 0.04);
      }
   }

   private static void releaseTsumukari(SenkoMuramasaEntity entity, ServerLevel level, int percent) {
      CompoundTag data = entity.getPersistentData();
      data.putBoolean(TSUMUKARI_RELEASED, true);
      boolean delayedDissolution = !hasDivinity(entity) && percent >= TSUMUKARI_SPECIAL_CHARGE_PERCENT;
      double remaining = 0.0;
      if (!delayedDissolution) {
         double cost = 1000.0 * Math.max(0.0, Math.min(1.0, percent / 100.0));
         double servantCost = Math.min(entity.getCurrentMp(), cost);
         entity.setCurrentMp(entity.getCurrentMp() - servantCost);
         remaining = cost - servantCost;
         ServerPlayer master = entity.getEntityMaster();
         if (remaining > 0.0 && master != null) {
            TypeMoonWorldModVariables.PlayerVariables vars = master.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
            double masterCost = Math.min(vars.player_mana, remaining);
            vars.player_mana -= masterCost;
            vars.syncMana(master);
            remaining -= masterCost;
         }
      }
      if (remaining > 0.0 && !delayedDissolution) {
         data.putBoolean(CHARGE_FORCED_DEATH, true);
      }
      entity.triggerSlashAnimation();
      if (level.getServer() != null) {
         MuramasaSlashHandler.initiateTsumukari(level, entity, percent, 300, MuramasaSlashHandler.TSUMUKARI_DAMAGE_SLASH_WIDTH, 100);
         TsumukariWaveProjectileEntity wave = new TsumukariWaveProjectileEntity(
            level, entity, percent, MagicCircuitColorHelper.ensureColor(entity)
         );
         level.addFreshEntity(wave);
      }
      for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, entity.getBoundingBox().inflate(25.0), living -> living != entity && !entity.isAlliedTo(living))) {
         Vec3 delta = target.position().subtract(entity.position());
         if (delta.lengthSqr() <= 625.0 && delta.normalize().dot(entity.getLookAngle()) > -0.15) {
            removeBeneficialEffects(target);
         }
      }
      spawnMuramasaReleaseEffects(level, entity, percent);
      if (delayedDissolution) {
         forceTsumukariDeath(entity, level);
         return;
      }
      if (data.getBoolean(CHARGE_FORCED_DEATH)) {
         TYPE_MOON_WORLD.queueServerWork(2, () -> {
            if (entity.isAlive()) {
               forceTsumukariDeath(entity, level);
            }
         });
      }
   }

   private static void forceTsumukariDeath(SenkoMuramasaEntity entity, ServerLevel level) {
      if (entity == null || !entity.isAlive()) {
         return;
      }
      if (ArtoriaPendragonCombatHelper.tryProtectWithAvalon(entity)) {
         return;
      }
      level.sendParticles(ParticleTypes.EXPLOSION, entity.getX(), entity.getY() + 1.0, entity.getZ(),
         4, 0.6, 0.8, 0.6, 0.0);
      level.sendParticles(ParticleTypes.LAVA, entity.getX(), entity.getY() + 0.8, entity.getZ(),
         40, 1.0, 0.8, 1.0, 0.04);
      entity.setInvulnerable(false);
      entity.invulnerableTime = 0;
      entity.hurt(entity.damageSources().genericKill(), Float.MAX_VALUE);
      if (entity.isAlive()) {
         entity.setHealth(0.0F);
         entity.die(entity.damageSources().genericKill());
      }
   }

   private static void spawnMuramasaFieldEffects(ServerLevel level, SenkoMuramasaEntity entity) {
      Vec3 origin = entity.position().add(0.0, 0.15, 0.0);

      level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLASH,
         origin.x, origin.y + 0.8, origin.z, 2, 0.0, 0.0, 0.0, 0.0);
      level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME,
         origin.x, origin.y + 0.85, origin.z, 90, 0.9, 0.75, 0.9, 0.08);
      level.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME,
         origin.x, origin.y + 0.9, origin.z, 36, 0.75, 0.65, 0.75, 0.05);
      level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
         origin.x, origin.y + 1.0, origin.z, 24, 0.8, 0.9, 0.8, 0.12);

      placeMuramasaField(level, entity);
      scheduleMuramasaBarrier(level, entity, 0);
      VFXServerEffects.screenFlash(level, origin, 48.0, 5, 0.24F);
   }

   private static void placeMuramasaField(ServerLevel level, SenkoMuramasaEntity entity) {
      CompoundTag data = entity.getPersistentData();
      if (data.getBoolean(FIELD_ACTIVE)) {
         return;
      }

      ListTag terrain = new ListTag();
      data.put(FIELD_TERRAIN, terrain);
      data.putBoolean(FIELD_ACTIVE, true);
      data.putBoolean(FIELD_COLLAPSING, false);
      data.putInt(FIELD_CENTER_X, entity.blockPosition().getX());
      data.putInt(FIELD_CENTER_Z, entity.blockPosition().getZ());
      int restoreDelay = NP_EXPAND_TICKS + NP_COLLAPSE_TICKS + 2;
      data.putLong(FIELD_RESTORE_TICK, level.getGameTime() + restoreDelay);
      TYPE_MOON_WORLD.queueServerWork(1, () -> expandMuramasaTerrain(level, entity, 0));
   }

   private static void expandMuramasaTerrain(ServerLevel level, SenkoMuramasaEntity entity, int radius) {
      if (!entity.isAlive() || entity.level() != level
         || !entity.getPersistentData().getBoolean(FIELD_ACTIVE)
         || entity.getPersistentData().getBoolean(FIELD_COLLAPSING)) {
         return;
      }
      CompoundTag data = entity.getPersistentData();
      ListTag terrain = data.getList(FIELD_TERRAIN, Tag.TAG_COMPOUND);
      int centerX = data.getInt(FIELD_CENTER_X);
      int centerZ = data.getInt(FIELD_CENTER_Z);
      int outerSquared = radius * radius;
      int innerSquared = Math.max(0, radius - 1) * Math.max(0, radius - 1);

      for (int dx = -radius; dx <= radius; dx++) {
         for (int dz = -radius; dz <= radius; dz++) {
            int distanceSquared = dx * dx + dz * dz;
            if (distanceSquared > outerSquared || (radius > 0 && distanceSquared <= innerSquared)) {
               continue;
            }
            int x = centerX + dx;
            int z = centerZ + dz;
            int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
            BlockPos groundPos = new BlockPos(x, y, z);
            if (!level.hasChunkAt(groundPos) || level.getBlockEntity(groundPos) != null) {
               continue;
            }
            BlockState original = level.getBlockState(groundPos);
            if (!original.is(Blocks.BEDROCK) && original.getDestroySpeed(level, groundPos) >= 0.0F) {
               BlockState replacement = Blocks.RED_SANDSTONE.defaultBlockState();
               if (!original.equals(replacement) && level.setBlock(groundPos, replacement, 3)) {
                  rememberFieldBlock(terrain, groundPos, original, replacement);
               }
            }

            long seed = mix64(groundPos.asLong() ^ entity.getUUID().getMostSignificantBits());
            if ((seed & 15L) != 0L) {
               continue;
            }
            BlockPos swordPos = groundPos.above();
            if (!level.hasChunkAt(swordPos) || level.getBlockEntity(swordPos) != null
               || !level.getBlockState(swordPos).canBeReplaced()) {
               continue;
            }
            Direction facing = Direction.fromYRot((seed & 0xFFFFL) * 360.0 / 65536.0);
            BlockState swordState = ((UBWWeaponBlock)ModBlocks.UBW_WEAPON_BLOCK.get()).defaultBlockState()
               .setValue(UBWWeaponBlock.FACING, facing.getAxis().isHorizontal() ? facing : Direction.NORTH)
               .setValue(UBWWeaponBlock.ROTATION_A, (seed & 1L) != 0L)
               .setValue(UBWWeaponBlock.ROTATION_B, (seed & 2L) != 0L)
               .setValue(UBWWeaponBlock.ROTATION_C, (seed & 4L) != 0L);
            BlockState swordOriginal = level.getBlockState(swordPos);
            if (!level.setBlock(swordPos, swordState, 3)) {
               continue;
            }
            rememberFieldBlock(terrain, swordPos, swordOriginal, swordState);
            if (level.getBlockEntity(swordPos) instanceof UBWWeaponBlockEntity tile) {
               ItemStack sword = new ItemStack(Items.IRON_SWORD);
               project(sword);
               tile.setStoredItem(sword);
            }
         }
      }

      if (radius < 25) {
         TYPE_MOON_WORLD.queueServerWork(8, () -> expandMuramasaTerrain(level, entity, radius + 1));
      }
   }

   private static void rememberFieldBlock(ListTag terrain, BlockPos pos, BlockState original, BlockState replacement) {
      CompoundTag entry = new CompoundTag();
      entry.putLong("pos", pos.asLong());
      entry.putInt("original", Block.getId(original));
      entry.putInt("replacement", Block.getId(replacement));
      terrain.add(entry);
   }

   private static void restoreFieldTerrain(SenkoMuramasaEntity entity) {
      if (!(entity.level() instanceof ServerLevel level)) {
         return;
      }
      CompoundTag data = entity.getPersistentData();
      ListTag terrain = data.getList(FIELD_TERRAIN, Tag.TAG_COMPOUND);
      for (int i = terrain.size() - 1; i >= 0; i--) {
         CompoundTag entry = terrain.getCompound(i);
         BlockPos pos = BlockPos.of(entry.getLong("pos"));
         if (!level.hasChunkAt(pos)) {
            continue;
         }
         BlockState current = level.getBlockState(pos);
         BlockState replacement = Block.stateById(entry.getInt("replacement"));
         if (current.equals(replacement)) {
            level.setBlock(pos, Block.stateById(entry.getInt("original")), 3);
         }
      }
      data.remove(FIELD_TERRAIN);
      data.putBoolean(FIELD_ACTIVE, false);
      data.remove(FIELD_RESTORE_TICK);
      data.remove(FIELD_CENTER_X);
      data.remove(FIELD_CENTER_Z);
      data.remove(FIELD_COLLAPSING);
   }

   private static void scheduleMuramasaBarrier(ServerLevel level, SenkoMuramasaEntity entity, int step) {
      if (step > 50) {
         return;
      }
      TYPE_MOON_WORLD.queueServerWork(step == 0 ? 1 : 4, () -> {
         if (!entity.isAlive() || entity.level() != level) {
            return;
         }
         double radius = 0.5 + 24.5 * step / 50.0;
         spawnMuramasaBarrierRing(level, entity, radius);
         scheduleMuramasaBarrier(level, entity, step + 1);
      });
   }

   private static void scheduleMuramasaCollapse(ServerLevel level, SenkoMuramasaEntity entity, int tick) {
      if (tick > NP_COLLAPSE_TICKS) {
         return;
      }
      TYPE_MOON_WORLD.queueServerWork(tick == 0 ? 1 : 4, () -> {
         if (!entity.isAlive() || entity.level() != level) {
            return;
         }
         double progress = Math.min(1.0, tick / (double)NP_COLLAPSE_TICKS);
         double radius = 25.0 * (1.0 - progress);
         if (radius > 0.25) {
            spawnMuramasaBarrierRing(level, entity, radius);
         }
         scheduleMuramasaCollapse(level, entity, tick + 4);
      });
   }

   private static void shatterMuramasaFieldSwords(ServerLevel level, SenkoMuramasaEntity entity) {
      ListTag terrain = entity.getPersistentData().getList(FIELD_TERRAIN, Tag.TAG_COMPOUND);
      int shattered = 0;
      for (int i = 0; i < terrain.size(); i++) {
         CompoundTag entry = terrain.getCompound(i);
         if (!Block.stateById(entry.getInt("replacement")).is(ModBlocks.UBW_WEAPON_BLOCK.get())) {
            continue;
         }
         BlockPos pos = BlockPos.of(entry.getLong("pos"));
         if (!level.hasChunkAt(pos) || !level.getBlockState(pos).is(ModBlocks.UBW_WEAPON_BLOCK.get())) {
            continue;
         }
         level.removeBlock(pos, false);
         level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
            pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5, 16, 0.18, 0.4, 0.18, 0.08);
         level.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT,
            pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5, 12, 0.32, 0.42, 0.32, 0.1);
         level.sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT,
            pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5, 8, 0.22, 0.35, 0.22, 0.12);
         shattered++;
      }
      if (shattered > 0) {
         level.playSound(null, entity.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 1.5F, 0.55F);
         level.sendParticles(net.minecraft.core.particles.ParticleTypes.SNOWFLAKE,
            entity.getX(), entity.getY() + 1.0, entity.getZ(), shattered * 3, 8.0, 3.0, 8.0, 0.12);
      }
   }

   private static void scheduleTsumukariHandEffects(ServerLevel level, SenkoMuramasaEntity entity, int tick) {
      if (tick > NP_COLLAPSE_TICKS) {
         return;
      }
      TYPE_MOON_WORLD.queueServerWork(tick == 0 ? 1 : 4, () -> {
         if (!entity.isAlive() || entity.level() != level) {
            return;
         }
         spawnTsumukariHandEffects(level, entity, tick);
         scheduleTsumukariHandEffects(level, entity, tick + 4);
      });
   }

   private static void spawnTsumukariHandEffects(ServerLevel level, SenkoMuramasaEntity entity, int tick) {
      Vec3 hand = entity.position().add(entity.getLookAngle().scale(0.35)).add(0.0, entity.getBbHeight() * 0.68, 0.0);
      int count = 12 + Math.min(24, tick / 3);
      level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME,
         hand.x, hand.y, hand.z, count, 0.28, 0.3, 0.28, 0.08);
      level.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME,
         hand.x, hand.y, hand.z, count / 2, 0.22, 0.24, 0.22, 0.05);
      level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
         hand.x, hand.y, hand.z, count / 2, 0.2, 0.25, 0.2, 0.1);
      if ((tick & 7) == 0) {
         level.sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT,
            hand.x, hand.y, hand.z, 32, 0.5, 0.45, 0.5, 0.15);
      }
   }

   private static void spawnMuramasaBarrierRing(ServerLevel level, SenkoMuramasaEntity entity, double radius) {
      double y = entity.getY() + 0.08;
      int points = Math.max(24, Math.min(72, (int)(radius * 3.0)));
      for (int i = 0; i < points; i++) {
         double angle = Math.PI * 2.0 * i / points;
         double x = entity.getX() + Math.cos(angle) * radius;
         double z = entity.getZ() + Math.sin(angle) * radius;
         level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME,
            x, y, z, 1, 0.0, 0.12, 0.0, 0.025);
         if ((i & 3) == 0) {
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME,
               x, y + 0.12, z, 1, 0.0, 0.08, 0.0, 0.018);
         }
         if ((i & 7) == 0) {
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
               x, y + 0.45, z, 1, 0.0, 0.1, 0.0, 0.04);
         }
      }
      spawnMuramasaHemisphereShell(level, entity, radius);
   }

   private static void spawnMuramasaHemisphereShell(ServerLevel level, SenkoMuramasaEntity entity, double radius) {
      if (radius < 1.0) {
         return;
      }
      int latitudeBands = Math.max(3, Math.min(6, (int)(radius / 4.0)));
      int longitudePoints = Math.max(24, Math.min(72, (int)(radius * 2.5)));
      for (int latitude = 1; latitude <= latitudeBands; latitude++) {
         double polar = (Math.PI * 0.5) * latitude / latitudeBands;
         double horizontalRadius = Math.sin(polar) * radius;
         double height = Math.cos(polar) * radius;
         for (int longitude = 0; longitude < longitudePoints; longitude++) {
            double angle = Math.PI * 2.0 * longitude / longitudePoints;
            double x = entity.getX() + Math.cos(angle) * horizontalRadius;
            double z = entity.getZ() + Math.sin(angle) * horizontalRadius;
            double y = entity.getY() + 0.1 + height;
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME,
               x, y, z, 1, 0.0, 0.03, 0.0, 0.015);
            if ((longitude + latitude) % 3 == 0) {
               level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                  x, y, z, 1, 0.0, 0.04, 0.0, 0.025);
            }
         }
      }
   }

   private static void spawnMuramasaReleaseEffects(ServerLevel level, SenkoMuramasaEntity entity, int percent) {
      Vec3 origin = entity.position().add(0.0, entity.getBbHeight() * 0.55, 0.0);
      Vec3 forward = horizontalLook(entity);
      int length = Math.max(8, Math.min(25, 6 + percent / 5));

      level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLASH,
         origin.x, origin.y, origin.z, 3, 0.0, 0.0, 0.0, 0.0);
      level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME,
         origin.x, origin.y, origin.z, 120, 0.7, 0.65, 0.7, 0.12);
      level.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME,
         origin.x, origin.y, origin.z, 42, 0.65, 0.55, 0.65, 0.08);

      for (int i = 1; i <= length; i++) {
         Vec3 point = origin.add(forward.scale(i));
         level.sendParticles(net.minecraft.core.particles.ParticleTypes.SWEEP_ATTACK,
            point.x, point.y, point.z, 1, 0.0, 0.0, 0.0, 0.0);
         level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME,
            point.x, point.y, point.z, 12, 0.32, 0.45, 0.32, 0.1);
         if (i % 2 == 0) {
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.LAVA,
               point.x, point.y - 0.35, point.z, 2, 0.18, 0.08, 0.18, 0.0);
         }
      }
      level.sendParticles(net.minecraft.core.particles.ParticleTypes.ASH,
         origin.x + forward.x * 3.0, origin.y, origin.z + forward.z * 3.0,
         45, length * 0.35, 0.7, length * 0.35, 0.04);
      level.playSound(null, entity.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.6F, 0.55F);
      VFXServerEffects.screenFlash(level, origin, 64.0, 4, 0.35F);
   }

   private static Vec3 horizontalLook(SenkoMuramasaEntity entity) {
      Vec3 look = entity.getLookAngle();
      Vec3 horizontal = new Vec3(look.x, 0.0, look.z);
      return horizontal.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : horizontal.normalize();
   }

   private static long mix64(long value) {
      value ^= value >>> 30;
      value *= 0xBF58476D1CE4E5B9L;
      value ^= value >>> 27;
      value *= 0x94D049BB133111EBL;
      return value ^ (value >>> 31);
   }

   public static void applyNoDefenseDamage(LivingEntity attacker, LivingEntity target, float amount) {
      if (attacker == null || target == null || amount <= 0.0F || !target.isAlive() || EntityUtils.isImmunePlayerTarget(target)) {
         return;
      }
      float before = target.getHealth();
      var source = attacker.damageSources().magic();
      target.invulnerableTime = 0;
      target.hurt(source, amount);
      target.invulnerableTime = 0;
      if (!target.isAlive()) {
         return;
      }
      float expected = Math.max(0.0F, before - amount);
      if (target.getHealth() > expected) {
         target.setHealth(expected);
         if (expected <= 0.0F && target.isAlive()) {
            target.die(source);
         }
      }
   }

   private static void applyNoDefenseDamage(SenkoMuramasaEntity entity, LivingEntity target, float amount) {
      applyNoDefenseDamage((LivingEntity)entity, target, amount);
   }

   public static double precisionStrikeDamage(LivingEntity attacker, LivingEntity target, boolean trialActive, boolean swordEquipped, boolean karmaActive) {
      if (attacker == null || target == null) {
         return 0.0;
      }
      double damage = Math.max(1.0, attacker.getAttributeValue(Attributes.ATTACK_DAMAGE) + 8.0);
      if (trialActive) {
         damage *= 1.30;
      }
      if (swordEquipped) {
         damage *= 1.20;
      }
      if (karmaActive) {
         damage *= 1.50;
      }
      if (isRulerOrKing(target)) {
         damage *= 1.30;
      }
      return damage * 1.50; // Karma Eye / precision strike guarantees a critical, maximum-damage hit.
   }

   private static boolean hasDivinity(SenkoMuramasaEntity entity) {
      return ServantIdentityHelper.hasTrait(entity, ServantTraitTag.DIVINE)
         || ServantIdentityHelper.hasTrait(entity, ServantTraitTag.CELESTIAL);
   }

   private static boolean isSword(ItemStack stack) {
      return stack != null && !stack.isEmpty() && stack.getItem() instanceof SwordItem;
   }

   private static boolean isRulerOrKing(LivingEntity target) {
      if (ServantIdentityHelper.hasTrait(target, ServantTraitTag.RULER)) return true;
      var definition = ServantIdentityHelper.definitionOf(target);
      if (definition != null && definition.classType() == net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantClassType.RULER) return true;
      if (target instanceof ServerPlayer player) {
         var advancement = player.server.getAdvancements().get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("typemoonworld", "king_qualification"));
         return advancement != null && player.getAdvancements().getOrStartProgress(advancement).isDone();
      }
      return false;
   }

   private static void removeBeneficialEffects(LivingEntity target) {
      for (var effect : List.copyOf(target.getActiveEffects())) {
         if (effect.getEffect().value().getCategory() == net.minecraft.world.effect.MobEffectCategory.BENEFICIAL) {
            target.removeEffect(effect.getEffect());
         }
      }
   }

   private static void project(ItemStack stack) {
      CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
      // Use the shared UBW projection markers so ItemRendererMixin selects
      // the mod's projection glint texture instead of vanilla enchantment foil.
      tag.putBoolean(PROJECTED_TAG, true);
      tag.putBoolean("is_projected", true);
      tag.putBoolean("is_infinite_projection", true);
      stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
      stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
   }

   public static ItemStack projectedStack(net.minecraft.world.item.Item item) {
      ItemStack stack = new ItemStack(item);
      project(stack);
      return stack;
   }

   private static void setProjectedWeapon(SenkoMuramasaEntity entity, net.minecraft.world.item.Item item) {
      ItemStack stack = new ItemStack(item);
      project(stack);
      entity.setItemInHand(InteractionHand.MAIN_HAND, stack);
   }

   private static boolean isProjected(ItemStack stack) {
      return stack != null && stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getBoolean(PROJECTED_TAG);
   }

   public static void cleanup(SenkoMuramasaEntity entity) {
      restoreFieldTerrain(entity);
      AttributeInstance attack = entity.getAttribute(Attributes.ATTACK_DAMAGE);
      AttributeInstance speed = entity.getAttribute(Attributes.ATTACK_SPEED);
      AttributeInstance knockback = entity.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
      if (attack != null) removeModifier(attack, DAMAGE_ID.id());
      if (speed != null) removeModifier(speed, SPEED_ID.id());
      if (knockback != null) removeModifier(knockback, KNOCKBACK_ID.id());
      if (isProjected(entity.getMainHandItem())) {
         entity.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
      }
   }

   private static void removeModifier(AttributeInstance attribute, net.minecraft.resources.ResourceLocation id) {
      if (attribute.getModifier(id) != null) {
         attribute.removeModifier(id);
      }
   }

   private record ResourceLocationLike(String value) {
      net.minecraft.resources.ResourceLocation id() {
         return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, value);
      }
   }
}
