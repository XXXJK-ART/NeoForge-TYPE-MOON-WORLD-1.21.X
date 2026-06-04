package net.xxxjk.TYPE_MOON_WORLD.servant.combat;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.CuChulainnCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantClassType;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantDefinition;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantParams;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantSpecialization;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;

public final class ServantCombatSystem {
   private static final String TAG_PREFIX = "TypeMoonCombat";
   private static final String TAG_PHASE = TAG_PREFIX + "Phase";
   private static final String TAG_LAST_COMBAT_TICK = TAG_PREFIX + "LastCombatTick";
   private static final String TAG_STAMINA = TAG_PREFIX + "Stamina";
   private static final String TAG_POISE = TAG_PREFIX + "Poise";
   private static final String TAG_STUN_UNTIL = TAG_PREFIX + "StunUntil";
   private static final String TAG_INVULN_UNTIL = TAG_PREFIX + "InvulnUntil";
   private static final String TAG_SUPPRESSED_UNTIL = TAG_PREFIX + "SuppressedUntil";
   private static final String TAG_DAMAGE_SECOND = TAG_PREFIX + "DamageSecond";
   private static final String TAG_DAMAGE_THIS_SECOND = TAG_PREFIX + "DamageThisSecond";
   private static final String TAG_COMBO_OWNER = TAG_PREFIX + "ComboOwner";
   private static final String TAG_COMBO_COUNT = TAG_PREFIX + "ComboCount";
   private static final String TAG_COMBO_DAMAGE = TAG_PREFIX + "ComboDamage";
   private static final String TAG_COMBO_LAST_TICK = TAG_PREFIX + "ComboLastTick";
   private static final String TAG_LAST_DODGE_TICK = TAG_PREFIX + "LastDodgeTick";
   private static final String TAG_GUARD_EXHAUST_UNTIL = TAG_PREFIX + "GuardExhaustUntil";
   private static final String TAG_LAST_GUARD_TICK = TAG_PREFIX + "LastGuardTick";
   private static final String TAG_NEXT_COMBO_TICK = TAG_PREFIX + "NextComboTick";
   private static final String TAG_UNTARGETABLE_UNTIL = TAG_PREFIX + "UntargetableUntil";
   private static final String TAG_RECOVERY_UNTIL = TAG_PREFIX + "RecoveryUntil";
   private static final String TAG_DAMAGE_BOOST_UNTIL = TAG_PREFIX + "DamageBoostUntil";
   private static final ResourceLocation SPEED_ID = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "servant_combat_speed");
   private static final ResourceLocation HARD_TANK_ATTACK_ID = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "servant_np_hardtank_attack");
   private static final int OUT_OF_COMBAT_RESET_TICKS = 100;

   private ServantCombatSystem() {
   }

   public static boolean tickBeforeAi(ServantEntity entity) {
      if (entity.level().isClientSide()) {
         return false;
      }

      long now = entity.level().getGameTime();
      ServantDefinition definition = entity.getDefinition();
      ServantParams params = definition != null ? definition.parameters() : null;
      CompoundTag data = entity.getPersistentData();
      initializeResources(data, params);
      tickResourceRegen(entity, data, params, now);
      tickRecovery(entity, data, params, now);
      updateCombatState(entity, definition, data, now);
      updateMovementSpeed(entity, definition, data, now);
      cleanupAttackBoost(entity, data, now);

      if (isUntargetable(entity)) {
         entity.setTarget(null);
      }

      if (cannotAct(entity)) {
         entity.getNavigation().stop();
         entity.setDeltaMovement(entity.getDeltaMovement().multiply(0.25, 1.0, 0.25));
         return true;
      }
      return false;
   }

   public static boolean tryRunComboAction(ServantEntity attacker, LivingEntity target) {
      if (attacker.level().isClientSide()
         || target == null
         || !target.isAlive()
         || cannotAct(attacker)
         || skillsSuppressed(attacker)
         || isUntargetable(target)) {
         return false;
      }

      ServantDefinition definition = attacker.getDefinition();
      if (definition == null) {
         return false;
      }

      CompoundTag data = attacker.getPersistentData();
      long now = attacker.level().getGameTime();
      ServantCombatPhase phase = getPhase(attacker);
      if (phase == ServantCombatPhase.PROBING || now < data.getLong(TAG_NEXT_COMBO_TICK)) {
         return false;
      }

      double distance = attacker.distanceTo(target);
      boolean decisive = phase == ServantCombatPhase.DECISIVE || isBerserker(definition);
      double maxStartDistance = decisive ? 10.0 : 7.0;
      if (distance > maxStartDistance) {
         return false;
      }
      attacker.faceToward(target.position());

      ServantSpecialization specialization = definition.specialization();
      if (!hasLauncher(specialization)) {
         return false;
      }

      data.putLong(TAG_NEXT_COMBO_TICK, now + (decisive ? 45L : 80L));
      performLauncher(attacker, target, decisive ? 1.0F : 0.8F);
      return true;
   }

   public static void handleIncomingDamage(ServantEntity servant, LivingIncomingDamageEvent event) {
      if (servant.level().isClientSide() || event.isCanceled()) {
         return;
      }
      DamageSource source = event.getSource();
      if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
         recordIncomingDamage(servant, event.getAmount());
         return;
      }

      long now = servant.level().getGameTime();
      CompoundTag data = servant.getPersistentData();
      if (now < data.getLong(TAG_INVULN_UNTIL) || isUntargetable(servant)) {
         event.setCanceled(true);
         spawnGuardFx(servant, ParticleTypes.END_ROD, SoundEvents.SHIELD_BLOCK, 1.45F);
         return;
      }

      ServantDefinition definition = servant.getDefinition();
      ServantParams params = definition != null ? definition.parameters() : null;
      if (now < data.getLong(TAG_STUN_UNTIL) && data.getBoolean(TAG_PREFIX + "GuardBroken")) {
         event.setAmount((float)(event.getAmount() * (1.0 + ServantCombatFormulas.guardBreakDamageBonus(params))));
      }

      if (!skillsSuppressed(servant)) {
         if (tryAutoDodge(servant, source, params, now)) {
            event.setCanceled(true);
            return;
         }

         Float reduced = tryAutoBlock(servant, source, event.getAmount(), params, now);
         if (reduced != null) {
            if (reduced <= 0.0F) {
               event.setCanceled(true);
            } else {
               event.setAmount(reduced);
            }
         }
      }

      if (!event.isCanceled()) {
         recordIncomingDamage(servant, event.getAmount());
         consumePoiseFromControl(servant, event.getAmount() >= servant.getMaxHealth() * 0.08F ? 10.0 : 0.0);
      }
   }

   public static void broadcastNoblePhantasmWindup(ServantEntity caster, LivingEntity target, int windupTicks, boolean ranged) {
      if (!(caster.level() instanceof ServerLevel level) || windupTicks <= 0) {
         return;
      }
      AABB box = caster.getBoundingBox().inflate(20.0);
      List<ServantEntity> responders = level.getEntitiesOfClass(
         ServantEntity.class,
         box,
         servant -> servant != caster && servant.isAlive() && !servant.isAlliedTo(caster) && !isUntargetable(servant)
      );
      for (ServantEntity responder : responders) {
         respondToNoblePhantasm(responder, caster, target, ranged);
      }
   }

   public static boolean cannotAct(LivingEntity entity) {
      return entity instanceof ServantEntity servant
         && servant.getPersistentData().getLong(TAG_STUN_UNTIL) > servant.level().getGameTime();
   }

   public static boolean skillsSuppressed(LivingEntity entity) {
      return entity instanceof ServantEntity servant
         && servant.getPersistentData().getLong(TAG_SUPPRESSED_UNTIL) > servant.level().getGameTime();
   }

   public static boolean isUntargetable(LivingEntity entity) {
      return entity instanceof ServantEntity servant
         && servant.getPersistentData().getLong(TAG_UNTARGETABLE_UNTIL) > servant.level().getGameTime();
   }

   public static ServantCombatPhase getPhase(ServantEntity entity) {
      ServantDefinition definition = entity.getDefinition();
      if (definition != null && isBerserker(definition)) {
         return ServantCombatPhase.DECISIVE;
      }
      return ServantCombatPhase.fromId(entity.getPersistentData().getInt(TAG_PHASE));
   }

   public static boolean canUseNoblePhantasm(ServantEntity entity) {
      ServantDefinition definition = entity.getDefinition();
      return definition != null && (isBerserker(definition) || getPhase(entity) == ServantCombatPhase.DECISIVE);
   }

   public static boolean isDamageBoosted(ServantEntity entity) {
      return entity.getPersistentData().getLong(TAG_DAMAGE_BOOST_UNTIL) > entity.level().getGameTime();
   }

   private static void initializeResources(CompoundTag data, ServantParams params) {
      double staminaMax = ServantCombatFormulas.staminaMax(params);
      if (!data.contains(TAG_STAMINA)) {
         data.putDouble(TAG_STAMINA, staminaMax);
      }
      if (!data.contains(TAG_POISE)) {
         data.putDouble(TAG_POISE, ServantCombatFormulas.poiseMax(params));
      }
   }

   private static void tickResourceRegen(ServantEntity entity, CompoundTag data, ServantParams params, long now) {
      if (entity.tickCount % 5 != 0) {
         return;
      }
      double staminaMax = ServantCombatFormulas.staminaMax(params);
      if (now >= data.getLong(TAG_GUARD_EXHAUST_UNTIL)) {
         double stamina = Math.min(staminaMax, data.getDouble(TAG_STAMINA) + ServantCombatFormulas.staminaRegenPerSecond(params) / 4.0);
         data.putDouble(TAG_STAMINA, stamina);
      }

      double poiseMax = ServantCombatFormulas.poiseMax(params);
      double poise = Math.min(poiseMax, data.getDouble(TAG_POISE) + ServantCombatFormulas.poiseRegenPerSecond(params) / 4.0);
      data.putDouble(TAG_POISE, poise);
   }

   private static void tickRecovery(ServantEntity entity, CompoundTag data, ServantParams params, long now) {
      long recoveryUntil = data.getLong(TAG_RECOVERY_UNTIL);
      if (recoveryUntil <= now || entity.tickCount % 20 != 0) {
         return;
      }
      entity.heal((float)(entity.getMaxHealth() * ServantCombatFormulas.comboProtectionHealPercentPerSecond(params)));
      if (entity.level() instanceof ServerLevel level) {
         level.sendParticles(ParticleTypes.HAPPY_VILLAGER, entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ(), 4, 0.25, 0.25, 0.25, 0.0);
      }
   }

   private static void updateCombatState(ServantEntity entity, ServantDefinition definition, CompoundTag data, long now) {
      LivingEntity target = entity.getTarget();
      boolean inCombat = target != null && target.isAlive() && !EntityUtils.isImmunePlayerTarget(target) && entity.distanceToSqr(target) <= 24.0 * 24.0;
      if (inCombat) {
         data.putLong(TAG_LAST_COMBAT_TICK, now);
         if (isBerserker(definition)) {
            data.putInt(TAG_PHASE, ServantCombatPhase.DECISIVE.id());
            return;
         }

         double healthRatio = entity.getHealth() / Math.max(1.0, entity.getMaxHealth());
         ServantCombatPhase current = ServantCombatPhase.fromId(data.getInt(TAG_PHASE));
         if (healthRatio <= 0.60 && current.id() < ServantCombatPhase.DECISIVE.id()) {
            data.putInt(TAG_PHASE, ServantCombatPhase.DECISIVE.id());
         } else if (healthRatio <= 0.80 && current.id() < ServantCombatPhase.NORMAL.id()) {
            data.putInt(TAG_PHASE, ServantCombatPhase.NORMAL.id());
         }
         return;
      }

      long lastCombat = data.getLong(TAG_LAST_COMBAT_TICK);
      if (lastCombat <= 0 || now - lastCombat >= OUT_OF_COMBAT_RESET_TICKS) {
         resetCombatState(entity, data);
      }
   }

   private static void updateMovementSpeed(ServantEntity entity, ServantDefinition definition, CompoundTag data, long now) {
      AttributeInstance speed = entity.getAttribute(Attributes.MOVEMENT_SPEED);
      if (speed == null) {
         return;
      }
      speed.removeModifier(SPEED_ID);
      double targetSpeed = data.getLong(TAG_LAST_COMBAT_TICK) > 0 && now - data.getLong(TAG_LAST_COMBAT_TICK) < OUT_OF_COMBAT_RESET_TICKS
         ? ServantCombatFormulas.combatMovementSpeed(definition != null ? definition.parameters() : null)
         : ServantCombatFormulas.OUT_OF_COMBAT_SPEED;
      double base = speed.getBaseValue();
      speed.addTransientModifier(new AttributeModifier(SPEED_ID, targetSpeed - base, AttributeModifier.Operation.ADD_VALUE));
   }

   private static void resetCombatState(ServantEntity entity, CompoundTag data) {
      data.putInt(TAG_PHASE, ServantCombatPhase.PROBING.id());
      data.putDouble(TAG_STAMINA, ServantCombatFormulas.staminaMax(entity.getDefinition() != null ? entity.getDefinition().parameters() : null));
      data.putDouble(TAG_POISE, ServantCombatFormulas.poiseMax(entity.getDefinition() != null ? entity.getDefinition().parameters() : null));
      data.remove(TAG_LAST_COMBAT_TICK);
      data.remove(TAG_STUN_UNTIL);
      data.remove(TAG_INVULN_UNTIL);
      data.remove(TAG_SUPPRESSED_UNTIL);
      data.remove(TAG_COMBO_OWNER);
      data.remove(TAG_COMBO_COUNT);
      data.remove(TAG_COMBO_DAMAGE);
      data.remove(TAG_UNTARGETABLE_UNTIL);
      data.remove(TAG_RECOVERY_UNTIL);
      AttributeInstance speed = entity.getAttribute(Attributes.MOVEMENT_SPEED);
      if (speed != null) {
         speed.removeModifier(SPEED_ID);
         double base = speed.getBaseValue();
         speed.addTransientModifier(new AttributeModifier(SPEED_ID, ServantCombatFormulas.OUT_OF_COMBAT_SPEED - base, AttributeModifier.Operation.ADD_VALUE));
      }
   }

   private static void performLauncher(ServantEntity attacker, LivingEntity target, float damageScale) {
      ServantParams params = attacker.getDefinition().parameters();
      attacker.getNavigation().stop();
      triggerLauncherAnimation(attacker);
      float damage = (float)(attacker.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.8F * damageScale);
      if (target.hurt(attacker.damageSources().mobAttack(attacker), damage)) {
         addComboDamage(attacker, target, damage);
      }

      Vec3 dir = target.position().subtract(attacker.position());
      Vec3 horizontal = new Vec3(dir.x, 0.0, dir.z);
      if (horizontal.lengthSqr() < 1.0E-4) {
         horizontal = attacker.getLookAngle().multiply(1.0, 0.0, 1.0);
      }
      horizontal = horizontal.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : horizontal.normalize();
      double distance = ServantCombatFormulas.launcherDistance(params);
      attacker.faceVector(horizontal);
      double horizontalPower = 0.78 + distance * 0.13;
      double verticalPower = 0.72 + distance * 0.045;
      target.setDeltaMovement(horizontal.x * horizontalPower, verticalPower, horizontal.z * horizontalPower);
      target.hurtMarked = true;
      if (target instanceof ServantEntity servantTarget) {
         servantTarget.faceVector(horizontal.scale(-1.0));
         applyStun(servantTarget, ServantCombatFormulas.launcherHitstunTicks(params));
         consumePoiseFromControl(servantTarget, ServantCombatFormulas.launcherPoiseCost(params));
      }
      breakSoftBlocksAlongPath(attacker, target.position(), horizontal, distance);
      schedulePursuit(attacker, target);
   }

   private static void schedulePursuit(ServantEntity attacker, LivingEntity target) {
      CompoundTag targetData = target.getPersistentData();
      int count = getComboCount(attacker, target);
      if (count >= 2) {
         targetData.putLong(TAG_INVULN_UNTIL, target.level().getGameTime() + 10L);
         target.setDeltaMovement(target.getDeltaMovement().x, -0.25, target.getDeltaMovement().z);
         return;
      }

      TYPE_MOON_WORLD.queueServerWork(8, () -> {
         if (!attacker.isAlive() || !target.isAlive() || cannotAct(attacker)) {
            return;
         }
         if (tryInterruptPursuit(attacker, target)) {
            return;
         }
         Vec3 dir = target.position().subtract(attacker.position());
         Vec3 horizontal = new Vec3(dir.x, 0.0, dir.z);
         if (horizontal.lengthSqr() > 1.0E-4) {
            horizontal = horizontal.normalize();
            Vec3 arrive = target.position().subtract(horizontal.scale(1.6));
            attacker.teleportTo(arrive.x, arrive.y, arrive.z);
            attacker.faceVector(horizontal);
            attacker.setDeltaMovement(horizontal.scale(1.15).add(0.0, 0.05, 0.0));
         }
         triggerPursuitAnimation(attacker);
         float damage = (float)(attacker.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.9F);
         target.invulnerableTime = 0;
         if (target.hurt(attacker.damageSources().mobAttack(attacker), damage)) {
            addComboDamage(attacker, target, damage);
         }
         target.invulnerableTime = 0;
      });
   }

   private static boolean tryInterruptPursuit(ServantEntity attacker, LivingEntity target) {
      if (!(target instanceof ServantEntity defender) || skillsSuppressed(defender) || cannotAct(defender)) {
         return false;
      }
      long now = defender.level().getGameTime();
      ServantParams params = defender.getDefinition() != null ? defender.getDefinition().parameters() : null;
      if (tryAutoDodge(defender, attacker.damageSources().mobAttack(attacker), params, now)
         || tryAutoBlock(defender, attacker.damageSources().mobAttack(attacker), 1.0F, params, now) != null) {
         applyStun(attacker, 10);
         spawnGuardFx(attacker, ParticleTypes.CRIT, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 0.8F);
         return true;
      }
      return false;
   }

   private static boolean tryAutoDodge(ServantEntity servant, DamageSource source, ServantParams params, long now) {
      if (!canReactTo(source) || now < servant.getPersistentData().getLong(TAG_LAST_DODGE_TICK) + ServantCombatFormulas.dodgeCooldownTicks(params)) {
         return false;
      }
      if (servant.getCurrentMp() < ServantCombatFormulas.dodgeMpCost(params)) {
         return false;
      }
      int agility = ServantCombatFormulas.agilityStep(params);
      boolean urgent = servant.getHealth() <= servant.getMaxHealth() * 0.55F || source.getDirectEntity() instanceof Projectile || getPhase(servant) == ServantCombatPhase.DECISIVE;
      if (agility < 3 && !urgent) {
         return false;
      }
      servant.setCurrentMp(servant.getCurrentMp() - ServantCombatFormulas.dodgeMpCost(params));
      servant.getPersistentData().putLong(TAG_LAST_DODGE_TICK, now);
      servant.getPersistentData().putLong(TAG_INVULN_UNTIL, now + ServantCombatFormulas.dodgeInvulnerabilityTicks(params));
      Vec3 away = dodgeDirection(servant, source);
      servant.faceVector(away);
      servant.setDeltaMovement(away.x * 0.9, Math.max(servant.getDeltaMovement().y, 0.08), away.z * 0.9);
      servant.hurtMarked = true;
      spawnGuardFx(servant, ParticleTypes.CLOUD, SoundEvents.PLAYER_ATTACK_SWEEP, 1.35F);
      return true;
   }

   private static Float tryAutoBlock(ServantEntity servant, DamageSource source, float amount, ServantParams params, long now) {
      if (!canReactTo(source) || now < servant.getPersistentData().getLong(TAG_GUARD_EXHAUST_UNTIL)) {
         return null;
      }
      CompoundTag data = servant.getPersistentData();
      double cost = ServantCombatFormulas.blockStaminaCost(params);
      double stamina = data.getDouble(TAG_STAMINA);
      if (stamina < cost) {
         data.putLong(TAG_GUARD_EXHAUST_UNTIL, now + 60L);
         return null;
      }

      boolean shouldBlock = amount >= servant.getMaxHealth() * 0.04F || getPhase(servant) != ServantCombatPhase.PROBING;
      if (!shouldBlock) {
         return null;
      }

      boolean parry = now - data.getLong(TAG_LAST_GUARD_TICK) <= ServantCombatFormulas.parryWindowTicks(params);
      data.putLong(TAG_LAST_GUARD_TICK, now);
      data.putDouble(TAG_STAMINA, Math.max(0.0, stamina - (parry ? ServantCombatFormulas.parryStaminaCost(params) : cost)));
      if (data.getDouble(TAG_STAMINA) <= 0.0) {
         data.putLong(TAG_GUARD_EXHAUST_UNTIL, now + 60L);
      }
      spawnGuardFx(servant, parry ? ParticleTypes.CRIT : ParticleTypes.ENCHANT, SoundEvents.SHIELD_BLOCK, parry ? 1.65F : 1.1F);
      if (parry && source.getEntity() instanceof ServantEntity attacker) {
         applyStun(attacker, 10);
         return 0.0F;
      }
      return (float)(amount * (1.0 - ServantCombatFormulas.blockReduction(params)));
   }

   private static void respondToNoblePhantasm(ServantEntity responder, ServantEntity caster, LivingEntity target, boolean ranged) {
      ServantDefinition definition = responder.getDefinition();
      ServantSpecialization specialization = definition != null ? definition.specialization() : ServantSpecialization.empty();
      ServantParams params = definition != null ? definition.parameters() : null;
      if (skillsSuppressed(responder) || cannotAct(responder)) {
         return;
      }
      if (ranged && specialization.hasCombatAction("ranged_np") && canUseNoblePhantasm(responder) && responder.getCurrentMp() >= responder.getMaxMp() * 0.35) {
         responder.getPersistentData().putLong(TAG_DAMAGE_BOOST_UNTIL, responder.level().getGameTime() + 60L);
         responder.setCurrentMp(Math.max(0.0, responder.getCurrentMp() - responder.getMaxMp() * 0.25));
         spawnGuardFx(responder, ParticleTypes.FLASH, SoundEvents.BEACON_ACTIVATE, 1.2F);
         return;
      }
      if (hasInterrupt(specialization) && responder.distanceToSqr(caster) <= 14.0 * 14.0) {
         caster.getPersistentData().remove(CuChulainnCombatHelper.GAE_BOLG_WINDUP_UNTIL_TAG);
         applyStun(caster, 10);
         spawnGuardFx(responder, ParticleTypes.CRIT, SoundEvents.TRIDENT_THROW.value(), 1.4F);
         return;
      }
      if (ServantCombatFormulas.agilityStep(params) >= 3 && tryAutoDodge(responder, caster.damageSources().mobAttack(caster), params, responder.level().getGameTime())) {
         return;
      }
      if (tryAutoBlock(responder, caster.damageSources().mobAttack(caster), 20.0F, params, responder.level().getGameTime()) != null) {
         return;
      }
      if (isBerserker(definition) || responder.getPersistentData().getBoolean("BattleContinuationActive")) {
         responder.getPersistentData().putLong(TAG_DAMAGE_BOOST_UNTIL, responder.level().getGameTime() + 100L);
         applyAttackBoost(responder, 0.20);
      }
   }

   private static void recordIncomingDamage(ServantEntity servant, float amount) {
      if (amount <= 0.0F) {
         return;
      }
      CompoundTag data = servant.getPersistentData();
      long now = servant.level().getGameTime();
      long second = now / 20L;
      if (data.getLong(TAG_DAMAGE_SECOND) != second) {
         data.putLong(TAG_DAMAGE_SECOND, second);
         data.putFloat(TAG_DAMAGE_THIS_SECOND, 0.0F);
      }
      float total = data.getFloat(TAG_DAMAGE_THIS_SECOND) + amount;
      data.putFloat(TAG_DAMAGE_THIS_SECOND, total);
      ServantParams params = servant.getDefinition() != null ? servant.getDefinition().parameters() : null;
      if (total > ServantCombatFormulas.damageSuppressionThreshold(params)) {
         data.putLong(TAG_SUPPRESSED_UNTIL, second * 20L + 20L);
      }
   }

   private static void consumePoiseFromControl(ServantEntity servant, double amount) {
      if (amount <= 0.0) {
         return;
      }
      CompoundTag data = servant.getPersistentData();
      long now = servant.level().getGameTime();
      if (data.getLong(TAG_PREFIX + "SuperArmorUntil") > now) {
         return;
      }
      double poise = Math.max(0.0, data.getDouble(TAG_POISE) - amount);
      data.putDouble(TAG_POISE, poise);
      if (poise <= 0.0) {
         ServantParams params = servant.getDefinition() != null ? servant.getDefinition().parameters() : null;
         data.putBoolean(TAG_PREFIX + "GuardBroken", true);
         data.putLong(TAG_STUN_UNTIL, now + ServantCombatFormulas.guardBreakTicks(params));
         data.putDouble(TAG_POISE, ServantCombatFormulas.poiseMax(params) * 0.5);
         TYPE_MOON_WORLD.queueServerWork(ServantCombatFormulas.guardBreakTicks(params), () -> {
            if (servant.isAlive()) {
               servant.getPersistentData().remove(TAG_PREFIX + "GuardBroken");
               servant.getPersistentData().putLong(TAG_PREFIX + "SuperArmorUntil", servant.level().getGameTime() + 40L);
            }
         });
      }
   }

   private static void addComboDamage(ServantEntity attacker, LivingEntity target, float damage) {
      CompoundTag data = target.getPersistentData();
      long now = target.level().getGameTime();
      String owner = attacker.getUUID().toString();
      if (!owner.equals(data.getString(TAG_COMBO_OWNER)) || now - data.getLong(TAG_COMBO_LAST_TICK) > 60L) {
         data.putString(TAG_COMBO_OWNER, owner);
         data.putInt(TAG_COMBO_COUNT, 0);
         data.putFloat(TAG_COMBO_DAMAGE, 0.0F);
      }
      data.putInt(TAG_COMBO_COUNT, data.getInt(TAG_COMBO_COUNT) + 1);
      data.putFloat(TAG_COMBO_DAMAGE, data.getFloat(TAG_COMBO_DAMAGE) + damage);
      data.putLong(TAG_COMBO_LAST_TICK, now);
      if (data.getFloat(TAG_COMBO_DAMAGE) > target.getMaxHealth() * 0.40F) {
         triggerComboProtection(attacker, target);
      }
   }

   private static int getComboCount(ServantEntity attacker, LivingEntity target) {
      CompoundTag data = target.getPersistentData();
      return attacker.getUUID().toString().equals(data.getString(TAG_COMBO_OWNER)) ? data.getInt(TAG_COMBO_COUNT) : 0;
   }

   private static void triggerComboProtection(ServantEntity attacker, LivingEntity target) {
      long now = target.level().getGameTime();
      CompoundTag data = target.getPersistentData();
      data.putLong(TAG_INVULN_UNTIL, now + 20L);
      data.putLong(TAG_UNTARGETABLE_UNTIL, now + 40L);
      data.putLong(TAG_RECOVERY_UNTIL, now + 40L);
      data.remove(TAG_COMBO_OWNER);
      Vec3 away = attacker.position().subtract(target.position());
      Vec3 horizontal = new Vec3(away.x, 0.0, away.z);
      if (horizontal.lengthSqr() > 1.0E-4) {
         horizontal = horizontal.normalize().scale(1.2);
         attacker.faceVector(horizontal);
         attacker.setDeltaMovement(horizontal.x, Math.max(attacker.getDeltaMovement().y, 0.25), horizontal.z);
         attacker.hurtMarked = true;
      }
      spawnGuardFx(target, ParticleTypes.TOTEM_OF_UNDYING, SoundEvents.TOTEM_USE, 1.0F);
   }

   private static void applyStun(ServantEntity entity, int ticks) {
      entity.getPersistentData().putLong(TAG_STUN_UNTIL, entity.level().getGameTime() + Math.max(1, ticks));
   }

   private static boolean canReactTo(DamageSource source) {
      if (source == null || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
         return false;
      }
      if (source.is(DamageTypes.FELL_OUT_OF_WORLD)
         || source.is(DamageTypes.GENERIC_KILL)
         || source.is(DamageTypes.FALL)
         || source.is(DamageTypes.DROWN)
         || source.is(DamageTypes.FREEZE)
         || source.is(DamageTypes.IN_FIRE)
         || source.is(DamageTypes.ON_FIRE)
         || source.is(DamageTypes.LAVA)
         || source.is(DamageTypes.IN_WALL)) {
         return false;
      }
      return source.getEntity() != null || source.getDirectEntity() != null;
   }

   private static Vec3 dodgeDirection(ServantEntity servant, DamageSource source) {
      Entity attacker = source.getEntity() != null ? source.getEntity() : source.getDirectEntity();
      Vec3 away = attacker != null ? servant.position().subtract(attacker.position()) : servant.getLookAngle().scale(-1.0);
      Vec3 horizontal = new Vec3(away.x, 0.0, away.z);
      if (horizontal.lengthSqr() < 1.0E-4) {
         horizontal = servant.getLookAngle().yRot((float)Math.PI / 2.0F).multiply(1.0, 0.0, 1.0);
      }
      return horizontal.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : horizontal.normalize();
   }

   private static void triggerLauncherAnimation(ServantEntity attacker) {
      if (attacker.hasActionAnimation("launcher")) {
         attacker.triggerNamedActionAnimation("launcher");
      } else if (attacker.hasActionAnimation("uppercut")) {
         attacker.triggerUppercutAnimation();
      } else if (attacker.hasActionAnimation("slam")) {
         attacker.triggerGroundSlam();
      } else {
         attacker.triggerAttackSwing();
      }
   }

   private static void triggerPursuitAnimation(ServantEntity attacker) {
      if (attacker.hasActionAnimation("pursuit")) {
         attacker.triggerNamedActionAnimation("pursuit");
      } else if (attacker.hasActionAnimation("shadow_step")) {
         attacker.triggerNamedActionAnimation("shadow_step");
      } else if (attacker.hasActionAnimation("charge")) {
         attacker.triggerChargeAnimation();
      } else {
         attacker.triggerAttackSwing();
      }
   }

   private static boolean hasLauncher(ServantSpecialization specialization) {
      return specialization.hasCombatAction("launcher")
         || specialization.hasCombatAction("uppercut")
         || specialization.hasCombatAction("slam")
         || specialization.hasCombatAction("stomp")
         || specialization.hasCombatAction("chain_snare");
   }

   private static boolean hasPursuit(ServantSpecialization specialization) {
      return specialization.hasCombatAction("pursuit")
         || specialization.hasCombatAction("shadow_step")
         || specialization.hasCombatAction("charge")
         || specialization.hasCombatAction("viper_rush")
         || specialization.hasCombatAction("lunging_thrust")
         || specialization.hasCombatAction("serpent_step");
   }

   private static boolean hasInterrupt(ServantSpecialization specialization) {
      return specialization.hasCombatAction("interrupt")
         || specialization.hasCombatAction("dirk_throw")
         || specialization.hasCombatAction("rune_cast")
         || specialization.hasCombatAction("gandr")
         || specialization.hasCombatAction("magic_bolt");
   }

   private static void breakSoftBlocksAlongPath(ServantEntity attacker, Vec3 start, Vec3 horizontal, double distance) {
      if (!(attacker.level() instanceof ServerLevel level)) {
         return;
      }
      boolean heavy = isBerserker(attacker.getDefinition()) || attacker.getAttributeValue(Attributes.ATTACK_DAMAGE) >= 20.0;
      int radius = heavy ? 2 : 1;
      int height = heavy ? 3 : 2;
      float hardnessLimit = heavy ? 80.0F : 35.0F;
      for (double step = 1.0; step <= distance + (heavy ? 3.0 : 1.0); step += 0.75) {
         BlockPos center = BlockPos.containing(start.add(horizontal.scale(step)));
         for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -1, -radius), center.offset(radius, height, radius))) {
            BlockState state = level.getBlockState(pos);
            float hardness = state.getDestroySpeed(level, pos);
            if (!state.isAir() && hardness >= 0.0F && hardness < hardnessLimit && !state.is(Blocks.BEDROCK)) {
               level.removeBlock(pos, false);
            }
         }
      }
      level.sendParticles(
         heavy ? ParticleTypes.EXPLOSION : ParticleTypes.CLOUD,
         start.x + horizontal.x * distance * 0.5,
         start.y + 0.45,
         start.z + horizontal.z * distance * 0.5,
         heavy ? 10 : 6,
         distance * 0.18,
         0.35,
         distance * 0.18,
         0.02
      );
      level.playSound(null, BlockPos.containing(start), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, heavy ? 1.8F : 1.0F, heavy ? 0.45F : 0.7F);
   }

   private static void spawnGuardFx(LivingEntity entity, net.minecraft.core.particles.ParticleOptions particle, net.minecraft.sounds.SoundEvent sound, float pitch) {
      if (entity.level() instanceof ServerLevel level) {
         level.sendParticles(particle, entity.getX(), entity.getY() + entity.getBbHeight() * 0.55, entity.getZ(), 10, 0.25, 0.25, 0.25, 0.04);
         level.playSound(null, entity.blockPosition(), sound, SoundSource.HOSTILE, 0.75F, pitch);
      }
   }

   private static void applyAttackBoost(ServantEntity entity, double amount) {
      AttributeInstance attack = entity.getAttribute(Attributes.ATTACK_DAMAGE);
      if (attack != null) {
         attack.removeModifier(HARD_TANK_ATTACK_ID);
         attack.addTransientModifier(new AttributeModifier(HARD_TANK_ATTACK_ID, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
      }
   }

   private static void cleanupAttackBoost(ServantEntity entity, CompoundTag data, long now) {
      if (data.getLong(TAG_DAMAGE_BOOST_UNTIL) <= now) {
         AttributeInstance attack = entity.getAttribute(Attributes.ATTACK_DAMAGE);
         if (attack != null) {
            attack.removeModifier(HARD_TANK_ATTACK_ID);
         }
      }
   }

   private static boolean isBerserker(ServantDefinition definition) {
      return definition != null && definition.classType() == ServantClassType.BERSERKER;
   }
}
