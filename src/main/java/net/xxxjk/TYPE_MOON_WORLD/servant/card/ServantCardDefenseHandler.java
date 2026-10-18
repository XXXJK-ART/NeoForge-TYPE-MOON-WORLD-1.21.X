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
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatDefenseService;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.GilgameshDivineShield;
import net.xxxjk.TYPE_MOON_WORLD.servant.data.ServantDataRegistry;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.EmiyaArcherEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ArashCombatRules;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ArtoriaPendragonCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.HeraclesGodHandHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.OdaNobunagaCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.SasakiKojiroCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.lancelot.LancelotCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantDefinition;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantParams;
import net.xxxjk.TYPE_MOON_WORLD.servant.fanatic.FanaticDamageTypes;
import net.xxxjk.TYPE_MOON_WORLD.servant.palerider.PaleRiderDamageTypes;
import net.xxxjk.TYPE_MOON_WORLD.magic.MuramasaDamageTypes;

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
      ServantCombatDefenseService.tick(player);
      ServantParams params = paramsFor(vars);
      if (params == null) {
         clear(player);
         return;
      }
   }

   public static void clear(ServerPlayer player) {
      CompoundTag data = player.getPersistentData();
      data.remove(ServantCombatDefenseService.TAG_GUARDING);
      data.remove(ServantCombatDefenseService.TAG_GUARD_START);
      data.remove(ServantCombatDefenseService.TAG_LAST_GUARD_PACKET);
      data.remove(ServantCombatDefenseService.TAG_LAST_RESULT);
      data.remove(ServantCombatDefenseService.TAG_RESULT_UNTIL);
      ServantCombatDefenseService.sync(player, true);
      // Persistent resources and cooldowns survive form changes and relogging.

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
      if (event.getSource().is(MuramasaDamageTypes.TSUMUKARI_MURAMASA)) {
         event.setCanceled(false);
         event.setAmount(Float.MAX_VALUE);
         event.setInvulnerabilityTicks(0);
         return false;
      }
      if (OriginBulletHelper.isOriginBulletDamage(event.getSource())) {
         return false;
      }

      boolean infectionDamage = PaleRiderDamageTypes.isInfection(event.getSource());
      boolean guaranteedHit = event.getSource().is(FanaticDamageTypes.GUARANTEED_HITS);
      boolean defensePiercing = event.getSource().is(FanaticDamageTypes.BYPASSES_DEFENSES);

      ServantParams params = paramsFor(vars);
      if (params == null) {
         return false;
      }
      CompoundTag data = player.getPersistentData();
      ServantCombatDefenseService.initialize(player);
      long now = player.level().getGameTime();
      if (!defensePiercing && "artoria_pendragon".equals(vars.servant_card_id)) {
         event.setAmount(ArtoriaPendragonCombatHelper.applyManaBurstDefense(player, event.getSource(), event.getAmount()));
      }
      boolean specialNoblePhantasmDamage = isSpecialNoblePhantasmDamage(event.getSource(), event.getAmount());
      boolean elementalSwordDamage = net.xxxjk.TYPE_MOON_WORLD.servant.combat.NoblePhantasmDamageClassifier
         .isParacelsusElementalSwordDamage(event.getSource());
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
      if (handleHeraclesGodHand(player, vars, event, now, divineDefenseBroken, infectionDamage, defensePiercing)) {
         return true;
      }
      if (!defensePiercing && ("gilgamesh".equals(vars.servant_card_id) || "gilgamesh_caster".equals(vars.servant_card_id))) {
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
      if (!defensePiercing && !divineDefenseBroken && "paracelsus".equals(vars.servant_card_id)) {
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
      if (!defensePiercing && "li_shuwen".equals(vars.servant_card_id) && player.tickCount <= data.getInt("ServantCardLiCounterUntil")) {
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
      if (!defensePiercing && "cu_chulainn".equals(vars.servant_card_id)) {
         float barrier = data.getFloat(ServantCardCuChulainnSkills.CU_RUNE_BARRIER_HP_TAG);
         if (barrier > 0.0F && data.getLong(ServantCardCuChulainnSkills.CU_RUNE_BARRIER_UNTIL_TAG) > now) {
            float absorbed = Math.min(barrier, event.getAmount());
            data.putFloat(ServantCardCuChulainnSkills.CU_RUNE_BARRIER_HP_TAG, barrier - absorbed);
            event.setAmount(Math.max(0.0F, event.getAmount() - absorbed));
            spawnDefenseFx(player, ParticleTypes.END_ROD, SoundEvents.SHIELD_BLOCK, 1.35F);
            if (event.getAmount() <= 0.0F) {
               event.setCanceled(true);
               if (barrier <= absorbed) ServantCardCuChulainnSkills.clearRuneBarrier(player);
               return true;
            }
            if (barrier <= absorbed) ServantCardCuChulainnSkills.clearRuneBarrier(player);
         }
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
      if (!defensePiercing && "ushiwakamaru_rider".equals(vars.servant_card_id)) {
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
         && !LancelotCombatHelper.rollsEternalArmsDodgeBypass(event.getSource())
         && tryLiShuwenPassiveDodge(player, vars, event, now)) {
         if (event.getSource().is(DamageTypeTags.IS_EXPLOSION)) {
            event.setAmount(event.getAmount() * 0.5F);
            return false;
         }
         event.setCanceled(true);
         event.setAmount(0.0F);
         return true;
      }

      if (!guaranteedHit && !infectionDamage && !specialNoblePhantasmDamage && !divineDefenseBroken
         && ServantCombatDefenseService.trySkillAutoDodge(player)) {
         event.setCanceled(true); event.setAmount(0); return true;
      }
      Float reduced = defensePiercing || divineDefenseBroken || specialNoblePhantasmDamage && !elementalSwordDamage
         || LancelotCombatHelper.rollsEternalArmsGuardBypass(event.getSource())
         ? null : ServantCombatDefenseService.applyGuardHit(player, event.getSource(), event.getAmount());
      if (reduced != null) {
         ServantCombatDefenseService.markGuarded(event);
         if (reduced <= 0.0F) {
            event.setCanceled(true);
            event.setAmount(0.0F);
            return true;
         }
         event.setAmount(reduced);
      }

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
      boolean infectionDamage,
      boolean defensePiercing
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
      boolean specialAttack = defensePiercing || divineDefenseBroken || majorBrokenPhantasmExplosion || artoriaExcalibur || gaeBulgArmy || poisonOrWither;

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
         level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + player.getBbHeight() * 0.58, player.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
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
