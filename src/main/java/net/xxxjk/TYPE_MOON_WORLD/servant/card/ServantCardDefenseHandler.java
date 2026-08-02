package net.xxxjk.TYPE_MOON_WORLD.servant.card;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.xxxjk.TYPE_MOON_WORLD.combat.OriginBulletHelper;
import net.xxxjk.TYPE_MOON_WORLD.entity.ArtoriaExcaliburBeamEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.BrokenPhantasmProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.CrimsonHoundProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.GaeBulgArmyProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.PseudoSpiralSwordProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.SwordBarrelProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatFormulas;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.GilgameshDivineShield;
import net.xxxjk.TYPE_MOON_WORLD.servant.data.ServantDataRegistry;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.EmiyaArcherEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ArashCombatRules;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.HeraclesGodHandHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.OdaNobunagaCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.SasakiKojiroCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantDefinition;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantParams;
import net.xxxjk.TYPE_MOON_WORLD.servant.fanatic.FanaticDamageTypes;
import net.xxxjk.TYPE_MOON_WORLD.servant.palerider.PaleRiderDamageTypes;

public final class ServantCardDefenseHandler {
   private static final String TAG_PREFIX = "ServantCardCombat";
   private static final String TAG_STAMINA = TAG_PREFIX + "Stamina";
   private static final String TAG_POISE = TAG_PREFIX + "Poise";
   private static final String TAG_INVULN_UNTIL = TAG_PREFIX + "InvulnUntil";
   private static final String TAG_LAST_DODGE_TICK = TAG_PREFIX + "LastDodgeTick";
   private static final String TAG_LAST_GUARD_TICK = TAG_PREFIX + "LastGuardTick";
   private static final String TAG_GUARD_EXHAUST_UNTIL = TAG_PREFIX + "GuardExhaustUntil";
   private static final String TAG_GUARD_BROKEN_UNTIL = TAG_PREFIX + "GuardBrokenUntil";
   private static final String TAG_LI_PASSIVE_DODGE_COOLDOWN = "ServantCardLiPassiveDodgeCooldown";
   private static final String GOD_HAND_REVIVE_LOCK_TAG = "GodHandReviveLockUntil";
   private static final String BATTLE_CONTINUATION_RECOVERY_ACTIVE_TAG = "BattleContinuationRecoveryActive";
   private static final double BATTLE_CONTINUATION_TRIGGER_HEALTH_RATIO = 0.18;

   private ServantCardDefenseHandler() {
   }

   public static void tick(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      ServantParams params = paramsFor(vars);
      if (params == null) {
         clear(player);
         return;
      }
      CompoundTag data = player.getPersistentData();
      initializeResources(data, params);
      double staminaRegen = ServantCombatFormulas.staminaRegenPerSecond(params);
      double poiseRegen = ServantCombatFormulas.poiseRegenPerSecond(params);
      if ("arash".equals(vars.servant_card_id)) {
         staminaRegen = ArashCombatRules.boostedDefenseRecovery(staminaRegen);
         poiseRegen = ArashCombatRules.boostedPoiseRecovery(poiseRegen);
      }
      data.putDouble(TAG_STAMINA, Math.min(ServantCombatFormulas.staminaMax(params),
         data.getDouble(TAG_STAMINA) + staminaRegen / 20.0));
      data.putDouble(TAG_POISE, Math.min(ServantCombatFormulas.poiseMax(params),
         data.getDouble(TAG_POISE) + poiseRegen / 20.0));
   }

   public static void clear(ServerPlayer player) {
      CompoundTag data = player.getPersistentData();
      data.remove(TAG_STAMINA);
      data.remove(TAG_POISE);
      data.remove(TAG_INVULN_UNTIL);
      data.remove(TAG_LAST_DODGE_TICK);
      data.remove(TAG_LAST_GUARD_TICK);
      data.remove(TAG_GUARD_EXHAUST_UNTIL);
      data.remove(TAG_GUARD_BROKEN_UNTIL);
      data.remove(TAG_LI_PASSIVE_DODGE_COOLDOWN);
   }

   public static boolean handleIncomingDamage(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars, LivingIncomingDamageEvent event) {
      if (!vars.servant_card_transformed || event.isCanceled()) {
         return false;
      }
      if (event.getSource().is(DamageTypes.FALL)) {
         event.setCanceled(true);
         event.setAmount(0.0F);
         player.fallDistance = 0.0F;
         return true;
      }
      if (event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
         return false;
      }
      if (OriginBulletHelper.isOriginBulletDamage(event.getSource())) {
         return false;
      }

      boolean infectionDamage = PaleRiderDamageTypes.isInfection(event.getSource());
      boolean guaranteedHit = event.getSource().is(FanaticDamageTypes.GUARANTEED_HITS);

      ServantParams params = paramsFor(vars);
      if (params == null) {
         return false;
      }
      CompoundTag data = player.getPersistentData();
      initializeResources(data, params);
      long now = player.level().getGameTime();
      boolean specialNoblePhantasmDamage = isSpecialNoblePhantasmDamage(event.getSource(), event.getAmount());
      boolean divineDefenseBroken = now < data.getLong(OdaNobunagaCombatHelper.TAG_DIVINE_BREAK_UNTIL);
      if (divineDefenseBroken) {
         player.removeEffect(MobEffects.DAMAGE_RESISTANCE);
         player.removeEffect(MobEffects.ABSORPTION);
         player.setAbsorptionAmount(0.0F);
      }
      if (!guaranteedHit && !infectionDamage && !divineDefenseBroken && !specialNoblePhantasmDamage
         && now < data.getLong(TAG_INVULN_UNTIL)) {
         event.setCanceled(true);
         event.setAmount(0.0F);
         spawnDefenseFx(player, ParticleTypes.END_ROD, SoundEvents.SHIELD_BLOCK, 1.45F);
         return true;
      }
      if (handleHeraclesGodHand(player, vars, event, now, divineDefenseBroken, infectionDamage)) {
         return true;
      }
      if ("gilgamesh".equals(vars.servant_card_id)) {
         GilgameshDivineShield.ShieldHit shieldHit = GilgameshDivineShield.tryAbsorb(
            player, event.getSource(), event.getAmount()
         );
         if (shieldHit != null) {
            event.setAmount(shieldHit.remainingDamage());
            if (shieldHit.remainingDamage() <= 0.0F) {
               event.setCanceled(true);
               return true;
            }
         }
      }
      if (!divineDefenseBroken && "paracelsus".equals(vars.servant_card_id)) {
         float projected = player.getHealth() - event.getAmount();
         if ((projected <= 0.0F || projected <= player.getMaxHealth() * 0.5F) && ServantCardParacelsusSkills.usePhilosopherStone(player)) {
            event.setCanceled(true);
            event.setAmount(0.0F);
            return true;
         }
         if (event.getAmount() >= 18.0F && ServantCardParacelsusSkills.useDiamondShield(player)) {
            event.setCanceled(true);
            event.setAmount(0.0F);
            return true;
         }
      }
      if ("li_shuwen".equals(vars.servant_card_id) && player.tickCount <= data.getInt("ServantCardLiCounterUntil")) {
         data.remove("ServantCardLiCounterUntil");
         event.setCanceled(true);
         event.setAmount(0.0F);
         Entity sourceEntity = event.getSource().getEntity();
         if (sourceEntity instanceof LivingEntity attacker && attacker != player) {
            Vec3 dir = attacker.position().subtract(player.position());
            if (dir.lengthSqr() < 0.001) {
               dir = player.getLookAngle();
            }
            dir = new Vec3(dir.x, 0.0, dir.z).normalize();
            attacker.invulnerableTime = 0;
            attacker.hurt(player.damageSources().playerAttack(player), 28.0F);
            attacker.push(dir.x * 3.4, 0.45, dir.z * 3.4);
            attacker.hurtMarked = true;
         }
         ServantCardLiShuwenSkills.spawnLiHitFx(player, sourceEntity instanceof LivingEntity living ? living : null);
         return true;
      }
      if ("cu_chulainn".equals(vars.servant_card_id)) {
         float shield = data.getFloat(ServantCardCuChulainnSkills.CU_RUNE_ALGIZ_SHIELD_TAG);
         if (shield > 0.0F) {
            float absorbed = Math.min(shield, event.getAmount());
            event.setAmount(Math.max(0.0F, event.getAmount() - absorbed));
            if (shield > absorbed) {
               data.putFloat(ServantCardCuChulainnSkills.CU_RUNE_ALGIZ_SHIELD_TAG, shield - absorbed);
            } else {
               data.remove(ServantCardCuChulainnSkills.CU_RUNE_ALGIZ_SHIELD_TAG);
            }
            spawnDefenseFx(player, ParticleTypes.WAX_OFF, SoundEvents.SHIELD_BLOCK, 1.25F);
            if (event.getAmount() <= 0.0F) {
               event.setCanceled(true);
               return true;
            }
         }
      }
      if ("ushiwakamaru_rider".equals(vars.servant_card_id)) {
         if (ServantCardUshiwakamaruSkills.tryAbsorbShieldDamage(player, event.getSource(), event.getAmount())) {
            event.setCanceled(true);
            event.setAmount(0.0F);
            return true;
         }
         if (!guaranteedHit && ServantCardUshiwakamaruSkills.trySwallowDodge(player, event.getSource())) {
            event.setCanceled(true);
            event.setAmount(0.0F);
            return true;
         }
      }

      if (!guaranteedHit && !infectionDamage && !specialNoblePhantasmDamage && !divineDefenseBroken
         && (tryLiShuwenPassiveDodge(player, vars, event, now) || tryAutoDodge(player, vars, event, params, now))) {
         if ("zhao_yun_rider".equals(vars.servant_card_id)) {
            ServantCardZhaoYunSkills.recordBreakthroughDefense(player);
         }
         if (event.getSource().is(DamageTypeTags.IS_EXPLOSION)) {
            event.setAmount(event.getAmount() * 0.5F);
            return false;
         }
         event.setCanceled(true);
         event.setAmount(0.0F);
         return true;
      }

      Float reduced = divineDefenseBroken || specialNoblePhantasmDamage ? null : tryAutoGuard(player, event.getSource(), event.getAmount(), params, now);
      if (reduced != null) {
         if ("zhao_yun_rider".equals(vars.servant_card_id)) {
            ServantCardZhaoYunSkills.recordBreakthroughDefense(player);
         }
         if (reduced <= 0.0F) {
            event.setCanceled(true);
            event.setAmount(0.0F);
            return true;
         }
         event.setAmount(reduced);
      }
      consumePoise(player, params, event.getAmount(), now);
      if (!specialNoblePhantasmDamage && tryBattleContinuation(player, event, data)) {
         return true;
      }
      return false;
   }

   private static boolean handleHeraclesGodHand(
      ServerPlayer player,
      TypeMoonWorldModVariables.PlayerVariables vars,
      LivingIncomingDamageEvent event,
      long now,
      boolean divineDefenseBroken,
      boolean infectionDamage
   ) {
      if (!"heracles".equals(vars.servant_card_id) || !HeraclesGodHandHelper.hasGodHand(player)) {
         return false;
      }

      CompoundTag data = player.getPersistentData();
      if (now < data.getLong(GOD_HAND_REVIVE_LOCK_TAG)) {
         event.setCanceled(true);
         event.setAmount(0.0F);
         return true;
      }

      float damage = event.getAmount();
      boolean majorBrokenPhantasmExplosion = isMajorBrokenPhantasmExplosion(event.getSource(), damage);
      boolean artoriaExcalibur = isArtoriaExcaliburDamage(event.getSource());
      boolean gaeBulgArmy = isGaeBulgArmyDamage(event.getSource());
      boolean poisonOrWither = isPoisonOrWitherDamage(event.getSource());
      boolean specialAttack = divineDefenseBroken || majorBrokenPhantasmExplosion || artoriaExcalibur || gaeBulgArmy || poisonOrWither;

      if (!infectionDamage && !specialAttack && damage < data.getFloat("GodHandThreshold")) {
         event.setCanceled(true);
         event.setAmount(0.0F);
         spawnDefenseFx(player, ParticleTypes.ENCHANT, SoundEvents.SHIELD_BLOCK, 1.4F);
         return true;
      }

      if (!infectionDamage && !specialAttack) {
         float reduction = data.getFloat("GodHandAdaptiveReduction");
         float maxReduction = data.getFloat("GodHandAdaptiveMax");
         float currentResistance = data.getFloat("GodHandCurrentResistance");
         if (currentResistance < maxReduction) {
            currentResistance = Math.min(currentResistance + reduction, maxReduction);
            data.putFloat("GodHandCurrentResistance", currentResistance);
         }
         if (currentResistance > 0.0F) {
            event.setAmount(damage * (1.0F - currentResistance));
         }
      } else if (player.level() instanceof ServerLevel level) {
         level.sendParticles(ParticleTypes.FLASH, player.getX(), player.getY() + player.getBbHeight() * 0.58, player.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
         level.sendParticles(ParticleTypes.FLAME, player.getX(), player.getY() + player.getBbHeight() * 0.45, player.getZ(), 18, 0.52, 0.45, 0.52, 0.045);
      }

      if (!data.getBoolean("CausalSevered") && player.getHealth() - event.getAmount() <= 0.0F) {
         int livesLeft = data.getInt("GodHandLives");
         if (livesLeft > 0) {
            event.setCanceled(true);
            event.setAmount(0.0F);
            reviveGodHandPlayer(player, data, livesLeft - 1, now);
            return true;
         }
      }
      return false;
   }

   private static void reviveGodHandPlayer(ServerPlayer player, CompoundTag data, int remainingLives, long now) {
      data.putInt("GodHandLives", remainingLives);
      data.remove("CausalSevered");
      data.putLong(GOD_HAND_REVIVE_LOCK_TAG, now + 20L);
      player.clearFire();
      player.invulnerableTime = 0;
      player.hurtTime = 0;
      player.hurtDuration = 0;
      player.setHealth(player.getMaxHealth());
      player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20, 4, false, false, true));
      player.setDeltaMovement(player.getDeltaMovement().multiply(0.55, 1.0, 0.55));
      player.hurtMarked = true;
      if (player.level() instanceof ServerLevel level) {
         level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + player.getBbHeight() * 0.72, player.getZ(), 32, 0.62, 0.7, 0.62, 0.12);
         level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, player.getX(), player.getY() + player.getBbHeight() * 0.6, player.getZ(), 20, 0.55, 0.55, 0.55, 0.025);
         level.sendParticles(ParticleTypes.POOF, player.getX(), player.getY() + player.getBbHeight() * 0.45, player.getZ(), 16, 0.5, 0.45, 0.5, 0.06);
         level.playSound(null, player.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.9F, 0.78F);
      }
   }

   private static boolean isMajorBrokenPhantasmExplosion(DamageSource source, float originalDamage) {
      if (source == null || !source.is(DamageTypeTags.IS_EXPLOSION) || originalDamage < 300.0F) {
         return false;
      }
      Entity direct = source.getDirectEntity();
      if (direct instanceof PseudoSpiralSwordProjectileEntity
         || direct instanceof CrimsonHoundProjectileEntity
         || direct instanceof BrokenPhantasmProjectileEntity) {
         return true;
      }
      if (direct instanceof SwordBarrelProjectileEntity swordBarrel) {
         return swordBarrel.isBrokenPhantasm();
      }
      return direct instanceof EmiyaArcherEntity;
   }

   private static boolean isArtoriaExcaliburDamage(DamageSource source) {
      return source != null && source.getDirectEntity() instanceof ArtoriaExcaliburBeamEntity;
   }

   private static boolean isGaeBulgArmyDamage(DamageSource source) {
      return source != null && source.getDirectEntity() instanceof GaeBulgArmyProjectileEntity;
   }

   public static boolean isSpecialNoblePhantasmDamage(DamageSource source, float originalDamage) {
      return net.xxxjk.TYPE_MOON_WORLD.servant.combat.NoblePhantasmDamageClassifier.isNoblePhantasmDamage(source, originalDamage);
   }

   private static boolean isPoisonOrWitherDamage(DamageSource source) {
      return source != null && (source.is(NeoForgeMod.POISON_DAMAGE) || source.is(DamageTypes.WITHER));
   }

   private static boolean canReactTo(ServerPlayer player, DamageSource source) {
      if (source == null || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
         return false;
      }
      if (source.getEntity() == player || source.getDirectEntity() == player) {
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

   private static ServantParams paramsFor(TypeMoonWorldModVariables.PlayerVariables vars) {
      ServantDefinition definition = ServantDataRegistry.get(vars.servant_card_id);
      return definition == null ? null : definition.parameters();
   }

   private static boolean tryBattleContinuation(ServerPlayer player, LivingIncomingDamageEvent event, CompoundTag data) {
      if (!data.getBoolean("BattleContinuationActive")
         || data.getBoolean("CausalSevered")
         || (data.getBoolean("GodHandActive") && data.getInt("GodHandLives") > 0)
         || data.getInt("BattleContinuationCooldown") > 0
         || player.getHealth() - event.getAmount() > player.getMaxHealth() * BATTLE_CONTINUATION_TRIGGER_HEALTH_RATIO) {
         return false;
      }
      event.setCanceled(true);
      event.setAmount(0.0F);
      player.setHealth(Math.max(1.0F, player.getMaxHealth() * (float)BATTLE_CONTINUATION_TRIGGER_HEALTH_RATIO));
      player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 3, false, false, true));
      data.putBoolean(BATTLE_CONTINUATION_RECOVERY_ACTIVE_TAG, true);
      data.remove("BattleContinuationLastHealTick");
      data.putInt("BattleContinuationCooldown", Math.max(1, data.getInt("BattleContinuationMaxCooldown")));
      spawnDefenseFx(player, ParticleTypes.CRIT, SoundEvents.PLAYER_ATTACK_STRONG, 1.5F);
      return true;
   }

   private static void initializeResources(CompoundTag data, ServantParams params) {
      if (!data.contains(TAG_STAMINA)) {
         data.putDouble(TAG_STAMINA, ServantCombatFormulas.staminaMax(params));
      }
      if (!data.contains(TAG_POISE)) {
         data.putDouble(TAG_POISE, ServantCombatFormulas.poiseMax(params));
      }
   }

   private static boolean tryLiShuwenPassiveDodge(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars, LivingIncomingDamageEvent event, long now) {
      if (!"li_shuwen".equals(vars.servant_card_id)
         || player.getPersistentData().getLong(TAG_LI_PASSIVE_DODGE_COOLDOWN) > now
         || player.getRandom().nextFloat() >= 0.16F) {
         return false;
      }
      player.getPersistentData().putLong(TAG_LI_PASSIVE_DODGE_COOLDOWN, now + 80L);
      player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 60, 0, false, false, false));
      player.getPersistentData().putLong(TAG_INVULN_UNTIL, now + 8L);
      Vec3 away = dodgeDirection(player, event.getSource());
      player.setDeltaMovement(away.x * 1.15, Math.max(player.getDeltaMovement().y, 0.08), away.z * 1.15);
      player.hurtMarked = true;
      player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.8F, 1.8F);
      return true;
   }

   private static boolean tryAutoDodge(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars, LivingIncomingDamageEvent event, ServantParams params, long now) {
      if (!canReactTo(player, event.getSource())) {
         return false;
      }
      int agility = ServantCombatFormulas.agilityStep(params);
      int cooldown = ServantCombatFormulas.dodgeCooldownTicks(params);
      if ("emiya_archer".equals(vars.servant_card_id) || "li_shuwen".equals(vars.servant_card_id)) {
         cooldown = Math.max(4, cooldown / 2);
      }
      CompoundTag data = player.getPersistentData();
      if (now < data.getLong(TAG_LAST_DODGE_TICK) + cooldown) {
         return false;
      }
      boolean urgent = event.getAmount() >= player.getMaxHealth() * 0.08F
         || event.getSource().getDirectEntity() instanceof Projectile
         || event.getSource().is(DamageTypeTags.IS_EXPLOSION)
         || player.getHealth() <= player.getMaxHealth() * 0.55F;
      if (agility < 3 && !urgent) {
         return false;
      }
      double chance = Math.min(0.82, 0.18 + agility * 0.12 + (urgent ? 0.18 : 0.0));
      if ("emiya_archer".equals(vars.servant_card_id)) {
         chance += 0.12;
      }
      if (data.getBoolean(SasakiKojiroCombatHelper.MINDSEYE_ACTIVE_TAG)) {
         chance = Math.max(chance, Math.max(0.0F, data.getFloat(SasakiKojiroCombatHelper.MINDSEYE_DODGE_CHANCE_TAG)));
      }
      if (data.getBoolean("ArtoriaInstinctAActive") && event.getSource().getDirectEntity() instanceof Projectile) {
         chance = Math.max(chance, 0.90);
      }
      chance = Math.min(0.92, chance);
      if (player.getRandom().nextDouble() > chance) {
         return false;
      }
      double cost = Math.max(1.0, ServantCombatFormulas.dodgeMpCost(params) * ("emiya_archer".equals(vars.servant_card_id) ? 0.55 : 1.0));
      if (!ServantCardManaService.consume(player, vars, cost)) {
         return false;
      }
      data.putLong(TAG_LAST_DODGE_TICK, now);
      int invuln = ServantCombatFormulas.dodgeInvulnerabilityTicks(params);
      if ("li_shuwen".equals(vars.servant_card_id)) {
         invuln *= 2;
      } else if ("emiya_archer".equals(vars.servant_card_id)) {
         invuln += 5;
      }
      data.putLong(TAG_INVULN_UNTIL, now + invuln);
      Vec3 away = dodgeDirection(player, event.getSource());
      double distance = "emiya_archer".equals(vars.servant_card_id) ? 1.35 : 0.95;
      player.setDeltaMovement(away.x * distance, Math.max(player.getDeltaMovement().y, "emiya_archer".equals(vars.servant_card_id) ? 0.14 : 0.08), away.z * distance);
      player.hurtMarked = true;
      spawnDefenseFx(player, ParticleTypes.CLOUD, SoundEvents.PLAYER_ATTACK_SWEEP, 1.35F);
      return true;
   }

   private static Float tryAutoGuard(ServerPlayer player, DamageSource source, float amount, ServantParams params, long now) {
      if (!canReactTo(player, source)) {
         return null;
      }
      CompoundTag data = player.getPersistentData();
      if (now < data.getLong(TAG_GUARD_EXHAUST_UNTIL)) {
         return null;
      }
      double stamina = data.getDouble(TAG_STAMINA);
      double cost = ServantCombatFormulas.blockStaminaCost(params);
      if (stamina < cost) {
         data.putLong(TAG_GUARD_EXHAUST_UNTIL, now + 60L);
         return null;
      }
      boolean shouldGuard = player.isBlocking()
         || amount >= player.getMaxHealth() * 0.04F
         || (data.getBoolean(SasakiKojiroCombatHelper.MINDSEYE_ACTIVE_TAG)
            && player.getRandom().nextFloat() < Math.max(0.0F, data.getFloat(SasakiKojiroCombatHelper.MINDSEYE_BLOCK_CHANCE_TAG)));
      if (!shouldGuard) {
         return null;
      }
      boolean parry = player.isBlocking() && now - data.getLong(TAG_LAST_GUARD_TICK) <= ServantCombatFormulas.parryWindowTicks(params);
      data.putLong(TAG_LAST_GUARD_TICK, now);
      data.putDouble(TAG_STAMINA, Math.max(0.0, stamina - (parry ? ServantCombatFormulas.parryStaminaCost(params) : cost)));
      if (data.getDouble(TAG_STAMINA) <= 0.0) {
         data.putLong(TAG_GUARD_EXHAUST_UNTIL, now + 60L);
      }
      spawnDefenseFx(player, parry ? ParticleTypes.CRIT : ParticleTypes.ENCHANT, SoundEvents.SHIELD_BLOCK, parry ? 1.65F : 1.1F);
      if (parry && !source.is(DamageTypeTags.IS_EXPLOSION)) {
         return 0.0F;
      }
      float reduction = (float)ServantCombatFormulas.blockReduction(params);
      float reduced = amount * (1.0F - reduction);
      return source.is(DamageTypeTags.IS_EXPLOSION) ? Math.max(reduced, amount * 0.5F) : reduced;
   }

   private static void consumePoise(ServerPlayer player, ServantParams params, float amount, long now) {
      if (amount < player.getMaxHealth() * 0.08F || now < player.getPersistentData().getLong(TAG_GUARD_BROKEN_UNTIL)) {
         return;
      }
      CompoundTag data = player.getPersistentData();
      double poise = Math.max(0.0, data.getDouble(TAG_POISE) - 10.0);
      data.putDouble(TAG_POISE, poise);
      if (poise <= 0.0) {
         int breakTicks = ServantCombatFormulas.guardBreakTicks(params);
         data.putLong(TAG_GUARD_BROKEN_UNTIL, now + breakTicks);
         data.putDouble(TAG_POISE, ServantCombatFormulas.poiseMax(params) * 0.5);
         player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, breakTicks, 0, false, true, true));
         spawnDefenseFx(player, ParticleTypes.CRIT, SoundEvents.SHIELD_BREAK, 0.9F);
      }
   }

   private static Vec3 dodgeDirection(ServerPlayer player, DamageSource source) {
      Entity sourceEntity = source.getDirectEntity() != null ? source.getDirectEntity() : source.getEntity();
      Vec3 away;
      if (sourceEntity instanceof Projectile projectile && projectile.getDeltaMovement().horizontalDistanceSqr() > 1.0E-4) {
         away = projectile.getDeltaMovement().multiply(-1.0, 0.0, -1.0);
      } else if (sourceEntity != null) {
         away = player.position().subtract(sourceEntity.position()).multiply(1.0, 0.0, 1.0);
      } else {
         Vec3 look = player.getLookAngle();
         away = new Vec3(-look.x, 0.0, -look.z);
      }
      if (away.lengthSqr() < 1.0E-4) {
         double yaw = Math.toRadians(player.getYRot() + 90.0F);
         away = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
      }
      return away.normalize();
   }

   private static void spawnDefenseFx(ServerPlayer player, net.minecraft.core.particles.ParticleOptions particle, net.minecraft.sounds.SoundEvent sound, float pitch) {
      if (player.level() instanceof ServerLevel level) {
         level.sendParticles(particle, player.getX(), player.getY() + player.getBbHeight() * 0.55, player.getZ(), 14, 0.32, 0.38, 0.32, 0.04);
         level.playSound(null, player.blockPosition(), sound, SoundSource.PLAYERS, 0.75F, pitch);
      }
   }
}
