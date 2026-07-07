package net.xxxjk.TYPE_MOON_WORLD.item.custom;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.entity.ArtoriaExcaliburBeamEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.DirkProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.EmiyaThrownWeaponEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.GaeBulgArmyProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.GaeBulgProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.RubyProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.init.ModSounds;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardManaService;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ArtoriaPendragonCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.CuChulainnCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.EnkiduEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.EmiyaArcherEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.MedeaCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.utils.ManaHelper;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXServerEffects;

public final class PlayerNoblePhantasmHelper {
   public static final String ONE_SHOT_TSUBAME_TAG = "TypeMoonOneShotTsubame";
   private static final String OVEREDGE_USE_COUNT_TAG = "TypeMoonOveredgeUseCount";
   private static final String GAE_DEATH_FLIGHT_TAG = "TypeMoonGaeBulgDeathFlight";
   private static final String GAE_DEATH_FLIGHT_PAID_TAG = "TypeMoonGaeBulgDeathFlightPaid";
   private static final String EXCALIBUR_CHARGE_TAG = "TypeMoonExcaliburCharge";
   private static final String EXCALIBUR_LAST_CHARGE_VFX_TAG = "TypeMoonExcaliburLastChargeVfx";
   private static final String EXCALIBUR_MIN_CHARGE_PAID_TAG = "TypeMoonExcaliburMinChargePaid";
   private static final String ARTORIA_WIND_REVEAL_UNTIL_TAG = "ServantCardArtoriaWindRevealUntil";
   private static final String ARTORIA_EXCALIBUR_WIND_LOCK_UNTIL_TAG = "ServantCardArtoriaExcaliburWindLockUntil";
   private static final String GALLATIN_CHARGE_TAG = "TypeMoonGallatinCharge";
   private static final String GALLATIN_LAST_CHARGE_VFX_TAG = "TypeMoonGallatinLastChargeVfx";
   private static final String GALLATIN_MIN_CHARGE_PAID_TAG = "TypeMoonGallatinMinChargePaid";
   private static final String SERVANT_CARD_NP_CHARGE_VOICE_TAG = "TypeMoonServantCardNpChargeVoice";
   private static final int SERVANT_CARD_CHARGE_SHORT_VOICE_TICKS = 60;
   private static final double SERVANT_CARD_CHARGE_VOICE_STOP_RADIUS = 96.0;
   private static final int GAE_DEATH_FLIGHT_CHARGE_TICKS = 30;
   private static final int MIN_CHARGE_NP_RELEASE_TICKS = 30;
   private static final int EXCALIBUR_MAX_CHARGE_TICKS = 100;
   private static final int EXCALIBUR_RELEASE_TICKS = 150;
   private static final int EXCALIBUR_DAMAGE_START_TICK = 58;
   private static final int EXCALIBUR_PLAYER_COOLDOWN = 1200;
   private static final int GALLATIN_MAX_CHARGE_TICKS = 100;
   private static final int GALLATIN_PLAYER_COOLDOWN = 1200;
   private static final int GAE_BULG_SINGLE_PLAYER_COOLDOWN = 600;
   private static final int GAE_BULG_ARMY_PLAYER_COOLDOWN = 2400;
   private static final double GALLATIN_RANGE = 100.0;
   private static final double GALLATIN_HALF_ANGLE_COS = Math.cos(Math.toRadians(35.0));
   private static final double CHARGE_MANA_PER_TICK = 10.0;

   private PlayerNoblePhantasmHelper() {
   }

   public static boolean consumeStrict(ServerPlayer player, double amount) {
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (vars.servant_card_transformed) {
         if (ServantCardManaService.consume(player, vars, amount)) {
            return true;
         }
         player.displayClientMessage(Component.translatable("message.typemoonworld.not_enough_mana"), true);
         return false;
      }
      if (ManaHelper.consumeManaStrict(player, amount, false)) {
         return true;
      }
      player.displayClientMessage(Component.translatable("message.typemoonworld.not_enough_mana"), true);
      return false;
   }

   public static boolean hasOneShotTsubame(ItemStack stack) {
      CompoundTag tag = customTag(stack);
      return tag != null && tag.getBoolean(ONE_SHOT_TSUBAME_TAG);
   }

   public static boolean isInfiniteProjectedBizen(ItemStack stack) {
      CompoundTag tag = customTag(stack);
      return !stack.isEmpty()
         && stack.getItem() instanceof BizenNagamitsuItem
         && tag != null
         && tag.getBoolean("is_projected")
         && tag.getBoolean("is_infinite_projection");
   }

   public static void armTsubameAfterAnalysis(ServerPlayer player, ItemStack stack) {
      if (stack.isEmpty() || !(stack.getItem() instanceof BizenNagamitsuItem)) {
         return;
      }
      updateCustomData(stack, tag -> tag.putBoolean(ONE_SHOT_TSUBAME_TAG, true));
      player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 600, 2, false, true));
      player.displayClientMessage(Component.translatable("message.typemoonworld.tsubame_ready"), true);
   }

   public static void clearOneShotTsubame(ItemStack stack) {
      updateCustomData(stack, tag -> tag.remove(ONE_SHOT_TSUBAME_TAG));
   }

   public static boolean triggerTsubameOnHit(ServerPlayer player, ItemStack stack, LivingEntity target) {
      if (!hasOneShotTsubame(stack) || target == null || !target.isAlive() || player.distanceToSqr(target) > 16.0) {
         return false;
      }
      clearOneShotTsubame(stack);
      performTsubame(player, target);
      return true;
   }

   public static boolean hasTsubameGaeshiTarget(ServerPlayer player) {
      return findLookTarget(player, 5.5, 1.25) != null;
   }

   public static boolean useTsubameGaeshi(ServerPlayer player) {
      LivingEntity target = findLookTarget(player, 5.5, 1.25);
      if (target == null) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.no_target"), true);
         return false;
      }
      performTsubame(player, target);
      return true;
   }

   public static boolean useRuleBreaker(ServerPlayer player) {
      if (!consumeStrict(player, 20.0)) {
         return false;
      }
      LivingEntity target = findLookTarget(player, 5.0, 1.0);
      if (target == null) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.no_target"), true);
         return true;
      }
      MedeaCombatHelper.applyRuleBreakerHit(target, player);
      target.invulnerableTime = 0;
      target.hurt(player.damageSources().magic(), 8.0F);
      target.invulnerableTime = 0;
      if (player.level() instanceof ServerLevel level) {
         level.sendParticles(ParticleTypes.ENCHANT, target.getX(), target.getY() + target.getBbHeight() * 0.55, target.getZ(), 28, 0.35, 0.45, 0.35, 0.08);
         level.playSound(null, target.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0F, 1.4F);
      }
      return true;
   }

   public static boolean useDirk(ServerPlayer player, InteractionHand hand) {
      if (!consumeStrict(player, 16.0)) {
         return false;
      }
      DirkProjectileEntity projectile = new DirkProjectileEntity(player.level(), player);
      projectile.setDamage(20.0F);
      projectile.setPos(player.getX(), player.getEyeY() - 0.1, player.getZ());
      Vec3 look = player.getLookAngle();
      projectile.shoot(look.x, look.y, look.z, 1.9F, 0.0F);
      player.level().addFreshEntity(projectile);
      player.level().playSound(null, player.blockPosition(), SoundEvents.TRIDENT_THROW.value(), SoundSource.PLAYERS, 0.75F, 1.45F);
      if (!player.isCreative()) {
         player.getItemInHand(hand).hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
      }
      return true;
   }

   public static boolean useGaeBulgMelee(ServerPlayer player) {
      LivingEntity target = findLookTarget(player, 4.0, 1.15);
      if (target == null) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.no_target"), true);
         return true;
      }
      if (!consumeStrict(player, 20.0)) {
         return false;
      }
      resolveGaeBulgHit(player, target);
      addGaeBulgCooldown(player, GAE_BULG_SINGLE_PLAYER_COOLDOWN);
      return true;
   }

   public static void startGaeBulgDeathFlight(ServerPlayer player) {
      player.getPersistentData().putBoolean(GAE_DEATH_FLIGHT_TAG, true);
      player.getPersistentData().putInt(GAE_DEATH_FLIGHT_TAG + "Ticks", 0);
      player.getPersistentData().remove(GAE_DEATH_FLIGHT_PAID_TAG);
      startServantCardChargeVoice(player, "cu_chulainn", ModSounds.CU_CHULAINN_VOICE_GAE_BOLG.get());
   }

   public static void tickGaeBulgUse(Level level, LivingEntity living, int useTicks) {
      if (!(living instanceof ServerPlayer player) || !player.getPersistentData().getBoolean(GAE_DEATH_FLIGHT_TAG)) {
         return;
      }
      int charged = Math.min(GAE_DEATH_FLIGHT_CHARGE_TICKS, useTicks);
      player.getPersistentData().putInt(GAE_DEATH_FLIGHT_TAG + "Ticks", charged);
      tickServantCardChargeVoice(player, "cu_chulainn", ModSounds.CU_CHULAINN_VOICE_GAE_BOLG.get(), null);
      if (useTicks >= GAE_DEATH_FLIGHT_CHARGE_TICKS && !player.getPersistentData().getBoolean(GAE_DEATH_FLIGHT_PAID_TAG)) {
         if (!consumeStrict(player, CHARGE_MANA_PER_TICK * GAE_DEATH_FLIGHT_CHARGE_TICKS)) {
            stopServantCardChargeVoice(player, "cu_chulainn", ModSounds.CU_CHULAINN_VOICE_GAE_BOLG.get(), null);
            player.releaseUsingItem();
         } else {
            player.getPersistentData().putBoolean(GAE_DEATH_FLIGHT_PAID_TAG, true);
         }
      }
      if (charged < GAE_DEATH_FLIGHT_CHARGE_TICKS && level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CRIT, player.getX(), player.getY() + 1.0, player.getZ(), 6, 0.5, 0.55, 0.5, 0.04);
      }
   }

   public static boolean releaseGaeBulg(ServerPlayer player, boolean crouchingRelease) {
      boolean deathFlight = player.getPersistentData().getBoolean(GAE_DEATH_FLIGHT_TAG);
      int charged = player.getPersistentData().getInt(GAE_DEATH_FLIGHT_TAG + "Ticks");
      boolean deathFlightPaid = player.getPersistentData().getBoolean(GAE_DEATH_FLIGHT_PAID_TAG);
      stopServantCardChargeVoice(player, "cu_chulainn", ModSounds.CU_CHULAINN_VOICE_GAE_BOLG.get(), null);
      player.getPersistentData().remove(GAE_DEATH_FLIGHT_TAG);
      player.getPersistentData().remove(GAE_DEATH_FLIGHT_TAG + "Ticks");
      player.getPersistentData().remove(GAE_DEATH_FLIGHT_PAID_TAG);
      if (deathFlight && charged >= GAE_DEATH_FLIGHT_CHARGE_TICKS && deathFlightPaid) {
         throwGaeBulgArmy(player);
         addGaeBulgCooldown(player, GAE_BULG_ARMY_PLAYER_COOLDOWN);
         return true;
      }
      if (crouchingRelease || deathFlight) {
         if (consumeStrict(player, 20.0)) {
            throwGaeBulgSingle(player);
            addGaeBulgCooldown(player, GAE_BULG_SINGLE_PLAYER_COOLDOWN);
         }
         return true;
      }
      return false;
   }

   public static void startExcaliburCharge(ServerPlayer player) {
      player.getPersistentData().putInt(EXCALIBUR_CHARGE_TAG, 0);
      player.getPersistentData().remove(EXCALIBUR_LAST_CHARGE_VFX_TAG);
      player.getPersistentData().remove(EXCALIBUR_MIN_CHARGE_PAID_TAG);
      startServantCardChargeVoice(player, "artoria_pendragon", ModSounds.ARTORIA_VOICE_EXCALIBUR.get());
   }

   public static void tickExcaliburCharge(Level level, LivingEntity living, int useTicks) {
      if (!(living instanceof ServerPlayer player)) {
         return;
      }
      int charged = Math.min(EXCALIBUR_MAX_CHARGE_TICKS, useTicks);
      player.getPersistentData().putInt(EXCALIBUR_CHARGE_TAG, charged);
      applyNoblePhantasmChargeSlow(player);
      tickServantCardChargeVoice(player, "artoria_pendragon", ModSounds.ARTORIA_VOICE_EXCALIBUR.get(), ModSounds.ARTORIA_VOICE_EXCALIBUR_SHORT.get());
      if (useTicks >= SERVANT_CARD_CHARGE_SHORT_VOICE_TICKS) {
         revealArtoriaWindVeiledExcaliburForNp(player, 12);
      }
      if (useTicks >= MIN_CHARGE_NP_RELEASE_TICKS && useTicks <= EXCALIBUR_MAX_CHARGE_TICKS) {
         double cost = player.getPersistentData().getBoolean(EXCALIBUR_MIN_CHARGE_PAID_TAG)
            ? CHARGE_MANA_PER_TICK
            : CHARGE_MANA_PER_TICK * MIN_CHARGE_NP_RELEASE_TICKS;
         if (!consumeStrict(player, cost)) {
            stopServantCardChargeVoice(player, "artoria_pendragon", ModSounds.ARTORIA_VOICE_EXCALIBUR.get(), ModSounds.ARTORIA_VOICE_EXCALIBUR_SHORT.get());
            player.releaseUsingItem();
         } else {
            player.getPersistentData().putBoolean(EXCALIBUR_MIN_CHARGE_PAID_TAG, true);
            if (level instanceof ServerLevel serverLevel) {
            long now = serverLevel.getGameTime();
            if (now - player.getPersistentData().getLong(EXCALIBUR_LAST_CHARGE_VFX_TAG) >= 32L) {
               player.getPersistentData().putLong(EXCALIBUR_LAST_CHARGE_VFX_TAG, now);
               VFXServerEffects.spawn(serverLevel, "artoria_excalibur_charge", player, 128.0);
            }
            }
         }
      }
   }

   public static void releaseExcalibur(ServerPlayer player) {
      int charged = player.getPersistentData().getInt(EXCALIBUR_CHARGE_TAG);
      stopServantCardChargeVoice(player, "artoria_pendragon", ModSounds.ARTORIA_VOICE_EXCALIBUR.get(), ModSounds.ARTORIA_VOICE_EXCALIBUR_SHORT.get());
      player.getPersistentData().remove(EXCALIBUR_CHARGE_TAG);
      player.getPersistentData().remove(EXCALIBUR_LAST_CHARGE_VFX_TAG);
      player.getPersistentData().remove(EXCALIBUR_MIN_CHARGE_PAID_TAG);
      if (!(player.level() instanceof ServerLevel level)) {
         return;
      }
      if (charged < MIN_CHARGE_NP_RELEASE_TICKS) {
         level.playSound(null, player.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.45F, 1.65F);
         return;
      }
      float powerScale = chargePower(charged, EXCALIBUR_MAX_CHARGE_TICKS);
      Vec3 start = player.position().add(0.0, player.getBbHeight() * 0.66, 0.0).add(player.getLookAngle().normalize().scale(1.2));
      ArtoriaExcaliburBeamEntity beam = new ArtoriaExcaliburBeamEntity(level, player, start, EXCALIBUR_RELEASE_TICKS, EXCALIBUR_DAMAGE_START_TICK, powerScale);
      level.addFreshEntity(beam);
      revealArtoriaWindVeiledExcaliburForNp(player, EXCALIBUR_RELEASE_TICKS + 100);
      VFXServerEffects.spawn(level, "artoria_excalibur_beam", player, 192.0);
      addExcaliburCooldown(player, scaledCooldown(EXCALIBUR_PLAYER_COOLDOWN, powerScale));
      level.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F + powerScale * 1.5F, 0.85F);
      level.playSound(null, player.blockPosition(), SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 0.45F + powerScale * 0.65F, 1.65F);
   }

   public static void startGallatinCharge(ServerPlayer player) {
      player.getPersistentData().putInt(GALLATIN_CHARGE_TAG, 0);
      player.getPersistentData().remove(GALLATIN_LAST_CHARGE_VFX_TAG);
      player.getPersistentData().remove(GALLATIN_MIN_CHARGE_PAID_TAG);
      startServantCardChargeVoice(player, "gawain", ModSounds.GAWAIN_VOICE_NP.get());
   }

   public static void tickGallatinCharge(Level level, LivingEntity living, int useTicks) {
      if (!(living instanceof ServerPlayer player)) {
         return;
      }
      int charged = Math.min(GALLATIN_MAX_CHARGE_TICKS, useTicks);
      player.getPersistentData().putInt(GALLATIN_CHARGE_TAG, charged);
      applyNoblePhantasmChargeSlow(player);
      tickServantCardChargeVoice(player, "gawain", ModSounds.GAWAIN_VOICE_NP.get(), ModSounds.GAWAIN_VOICE_GALLATIN_SHORT.get());
      if (useTicks >= MIN_CHARGE_NP_RELEASE_TICKS && useTicks <= GALLATIN_MAX_CHARGE_TICKS) {
         double cost = player.getPersistentData().getBoolean(GALLATIN_MIN_CHARGE_PAID_TAG)
            ? CHARGE_MANA_PER_TICK
            : CHARGE_MANA_PER_TICK * MIN_CHARGE_NP_RELEASE_TICKS;
         if (!consumeStrict(player, cost)) {
            stopServantCardChargeVoice(player, "gawain", ModSounds.GAWAIN_VOICE_NP.get(), ModSounds.GAWAIN_VOICE_GALLATIN_SHORT.get());
            player.releaseUsingItem();
         } else {
            player.getPersistentData().putBoolean(GALLATIN_MIN_CHARGE_PAID_TAG, true);
            if (level instanceof ServerLevel serverLevel) {
            long now = serverLevel.getGameTime();
            if (now - player.getPersistentData().getLong(GALLATIN_LAST_CHARGE_VFX_TAG) >= 28L) {
               player.getPersistentData().putLong(GALLATIN_LAST_CHARGE_VFX_TAG, now);
               VFXServerEffects.spawn(serverLevel, "servant_gawain_gallatin_charge", player, 128.0);
            }
            if (useTicks % 2 == 0) {
               Vec3 front = player.position().add(horizontalLook(player).scale(1.2)).add(0.0, player.getBbHeight() * 0.72, 0.0);
               serverLevel.sendParticles(ParticleTypes.FLAME, front.x, front.y, front.z, 4, 0.24, 0.2, 0.24, 0.03);
               serverLevel.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + player.getBbHeight() + 2.2, player.getZ(), 3, 0.7, 0.2, 0.7, 0.015);
            }
            }
         }
      }
   }

   public static void releaseGallatin(ServerPlayer player) {
      int charged = player.getPersistentData().getInt(GALLATIN_CHARGE_TAG);
      stopServantCardChargeVoice(player, "gawain", ModSounds.GAWAIN_VOICE_NP.get(), ModSounds.GAWAIN_VOICE_GALLATIN_SHORT.get());
      player.getPersistentData().remove(GALLATIN_CHARGE_TAG);
      player.getPersistentData().remove(GALLATIN_LAST_CHARGE_VFX_TAG);
      player.getPersistentData().remove(GALLATIN_MIN_CHARGE_PAID_TAG);
      if (!(player.level() instanceof ServerLevel level)) {
         return;
      }
      if (charged < MIN_CHARGE_NP_RELEASE_TICKS) {
         level.playSound(null, player.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.45F, 1.65F);
         return;
      }
      float powerScale = chargePower(charged, GALLATIN_MAX_CHARGE_TICKS);
      Vec3 look = horizontalLook(player);
      boolean sunlight = isUnderGallatinSun(level, player.blockPosition());
      VFXServerEffects.spawnReplayable(level, "servant_gawain_gallatin", player, 3.0F);
      level.playSound(null, player.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.8F + powerScale * 1.4F, 0.62F);
      level.playSound(null, player.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.6F + powerScale * 0.9F, 0.78F);
      performGallatinCone(player, level, look, (sunlight ? 3000.0F : 1000.0F) * powerScale, powerScale);
      addGallatinCooldown(player, scaledCooldown(GALLATIN_PLAYER_COOLDOWN, powerScale));
   }

   public static boolean isChargingMovementLocked(ServerPlayer player) {
      CompoundTag data = player.getPersistentData();
      return data.getInt(EXCALIBUR_CHARGE_TAG) > 0 || data.getInt(GALLATIN_CHARGE_TAG) > 0;
   }

   private static void applyNoblePhantasmChargeSlow(ServerPlayer player) {
      player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 6, 1, false, false, true));
   }

   public static boolean usePseudoSpiralDash(ServerPlayer player) {
      if (!consumeStrict(player, 50.0)) {
         return false;
      }
      if (!(player.level() instanceof ServerLevel level)) {
         return true;
      }
      Vec3 dir = horizontalLook(player);
      player.setDeltaMovement(player.getDeltaMovement().add(dir.x * 1.55, 0.18, dir.z * 1.55));
      player.hurtMarked = true;
      Vec3 start = player.position().add(0.0, player.getBbHeight() * 0.45, 0.0);
      Vec3 end = start.add(dir.scale(5.0));
      AABB box = new AABB(start, end).inflate(1.1, 1.0, 1.1);
      boolean hit = false;
      for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && e != player && !EntityUtils.isImmunePlayerTarget(e))) {
         hit = true;
         target.invulnerableTime = 0;
         target.hurt(player.damageSources().mobAttack(player), 10F);
         target.invulnerableTime = 0;
         triggerPseudoSpiralExplosion(level, player, target.position().add(0.0, target.getBbHeight() * 0.45, 0.0));
         break;
      }
      if (!hit) {
         triggerPseudoSpiralExplosion(level, player, end);
      }
      level.sendParticles(ParticleTypes.END_ROD, start.x, start.y, start.z, 24, 0.35, 0.35, 0.35, 0.12);
      level.playSound(null, player.blockPosition(), SoundEvents.TRIDENT_RIPTIDE_2.value(), SoundSource.PLAYERS, 1.1F, 1.2F);
      return true;
   }

   public static boolean isUbwProjection(ItemStack stack) {
      CompoundTag tag = customTag(stack);
      return tag != null && tag.getBoolean("is_infinite_projection");
   }

   public static void markUbwProjection(ItemStack stack) {
      updateCustomData(stack, tag -> {
         tag.putBoolean("is_projected", true);
         tag.putBoolean("is_infinite_projection", true);
      });
      stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
   }

   public static void tryCompleteProjectedKanshouBakuyaPair(ServerPlayer player, ItemStack stack, InteractionHand hand) {
      String projectionId = kanshouBakuyaId(stack);
      if (projectionId == null || !isUbwProjection(stack)) {
         return;
      }
      InteractionHand otherHand = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
      if (!player.getItemInHand(otherHand).isEmpty()) {
         return;
      }
      ItemStack paired = new ItemStack(otherKanshouBakuyaItem(projectionId));
      markUbwProjection(paired);
      player.setItemInHand(otherHand, paired);
      if (player.level() instanceof ServerLevel level) {
         level.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + player.getBbHeight() * 0.65, player.getZ(), 18, 0.28, 0.42, 0.28, 0.03);
         level.playSound(null, player.blockPosition(), SoundEvents.ILLUSIONER_PREPARE_MIRROR, SoundSource.PLAYERS, 0.65F, 1.45F);
      }
   }

   public static boolean useKanshouBakuya(ServerPlayer player, InteractionHand hand, String projectionId) {
      ItemStack stack = player.getItemInHand(hand);
      if (!isKanshouBakuyaId(projectionId) || !isUbwProjection(stack)) {
         return false;
      }
      if (player.isCrouching() && !isOveredgeKanshouBakuyaId(projectionId)) {
         evolveKanshouBakuyaPair(player);
      } else if (isOveredgeKanshouBakuyaId(projectionId)) {
         dashSlashOveredge(player, hand);
      } else {
         throwKanshouBakuya(player, hand);
      }
      player.getCooldowns().addCooldown(stack.getItem(), 10);
      return true;
   }

   private static void throwGaeBulgSingle(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level)) {
         return;
      }
      LivingEntity target = findLookTarget(player, 32.0, 1.5);
      GaeBulgProjectileEntity projectile = new GaeBulgProjectileEntity(level, player);
      projectile.setMode(GaeBulgProjectileEntity.Mode.SINGLE);
      projectile.setTrackedTarget(target);
      projectile.setPos(player.getX(), player.getEyeY() - 0.1, player.getZ());
      Vec3 aim = target != null ? target.position().add(0.0, target.getBbHeight() * 0.45, 0.0) : player.getEyePosition().add(player.getLookAngle().scale(32.0));
      Vec3 dir = aim.subtract(projectile.position()).normalize();
      projectile.shoot(dir.x, dir.y + 0.06, dir.z, 2.4F, 0.0F);
      level.addFreshEntity(projectile);
      level.playSound(null, player.blockPosition(), SoundEvents.TRIDENT_THROW.value(), SoundSource.PLAYERS, 1.0F, 0.75F);
   }

   private static void addGaeBulgCooldown(ServerPlayer player, int cooldownTicks) {
      if (player.getMainHandItem().is(ModItems.GAE_BULG.get())) {
         player.getCooldowns().addCooldown(player.getMainHandItem().getItem(), cooldownTicks);
      }
      if (player.getOffhandItem().is(ModItems.GAE_BULG.get())) {
         player.getCooldowns().addCooldown(player.getOffhandItem().getItem(), cooldownTicks);
      }
   }

   private static float chargePower(int chargedTicks, int maxTicks) {
      float ratio = Mth.clamp(chargedTicks / (float)Math.max(1, maxTicks), 0.0F, 1.0F);
      return 0.2F + ratio * 0.8F;
   }

   private static int scaledCooldown(int fullCooldown, float powerScale) {
      return Math.max(200, Mth.floor(fullCooldown * (0.25F + powerScale * 0.75F)));
   }

   private static void addExcaliburCooldown(ServerPlayer player, int cooldownTicks) {
      if (player.getMainHandItem().is(ModItems.EXCALIBUR.get())) {
         player.getCooldowns().addCooldown(player.getMainHandItem().getItem(), cooldownTicks);
      }
      if (player.getOffhandItem().is(ModItems.EXCALIBUR.get())) {
         player.getCooldowns().addCooldown(player.getOffhandItem().getItem(), cooldownTicks);
      }
   }

   private static void addGallatinCooldown(ServerPlayer player, int cooldownTicks) {
      if (player.getMainHandItem().is(ModItems.EXCALIBUR_GALLATIN.get())) {
         player.getCooldowns().addCooldown(player.getMainHandItem().getItem(), cooldownTicks);
      }
      if (player.getOffhandItem().is(ModItems.EXCALIBUR_GALLATIN.get())) {
         player.getCooldowns().addCooldown(player.getOffhandItem().getItem(), cooldownTicks);
      }
   }

   private static boolean isKanshouBakuyaId(String projectionId) {
      return "gan_jiang".equals(projectionId)
         || "mo_ye".equals(projectionId)
         || "gan_jiang_overedge".equals(projectionId)
         || "mo_ye_overedge".equals(projectionId);
   }

   private static boolean isOveredgeKanshouBakuyaId(String projectionId) {
      return "gan_jiang_overedge".equals(projectionId) || "mo_ye_overedge".equals(projectionId);
   }

   private static String kanshouBakuyaId(ItemStack stack) {
      if (stack.is(ModItems.GAN_JIANG.get())) {
         return "gan_jiang";
      } else if (stack.is(ModItems.MO_YE.get())) {
         return "mo_ye";
      } else if (stack.is(ModItems.GAN_JIANG_OVEREDGE.get())) {
         return "gan_jiang_overedge";
      } else {
         return stack.is(ModItems.MO_YE_OVEREDGE.get()) ? "mo_ye_overedge" : null;
      }
   }

   private static net.minecraft.world.item.Item otherKanshouBakuyaItem(String projectionId) {
      return switch (projectionId) {
         case "gan_jiang" -> ModItems.MO_YE.get();
         case "mo_ye" -> ModItems.GAN_JIANG.get();
         case "gan_jiang_overedge" -> ModItems.MO_YE_OVEREDGE.get();
         case "mo_ye_overedge" -> ModItems.GAN_JIANG_OVEREDGE.get();
         default -> ModItems.MO_YE.get();
      };
   }

   private static void evolveKanshouBakuyaPair(ServerPlayer player) {
      setKanshouBakuyaHand(player, InteractionHand.MAIN_HAND, true);
      setKanshouBakuyaHand(player, InteractionHand.OFF_HAND, true);
      if (player.level() instanceof ServerLevel level) {
         level.sendParticles(ParticleTypes.FLASH, player.getX(), player.getY() + player.getBbHeight() * 0.6, player.getZ(), 2, 0.1, 0.1, 0.1, 0.0);
         level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + player.getBbHeight() * 0.62, player.getZ(), 36, 0.45, 0.45, 0.45, 0.08);
         level.playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0F, 0.75F);
      }
   }

   private static void setKanshouBakuyaHand(ServerPlayer player, InteractionHand hand, boolean overedge) {
      ItemStack current = player.getItemInHand(hand);
      boolean ganJiang = current.is(ModItems.GAN_JIANG.get()) || current.is(ModItems.GAN_JIANG_OVEREDGE.get());
      boolean moYe = current.is(ModItems.MO_YE.get()) || current.is(ModItems.MO_YE_OVEREDGE.get());
      if (!ganJiang && !moYe) {
         return;
      }
      ItemStack evolved = new ItemStack(ganJiang
         ? overedge ? ModItems.GAN_JIANG_OVEREDGE.get() : ModItems.GAN_JIANG.get()
         : overedge ? ModItems.MO_YE_OVEREDGE.get() : ModItems.MO_YE.get());
      markUbwProjection(evolved);
      player.setItemInHand(hand, evolved);
   }

   private static void throwKanshouBakuya(ServerPlayer player, InteractionHand hand) {
      if (!(player.level() instanceof ServerLevel level)) {
         return;
      }
      ItemStack thrownStack = player.getItemInHand(hand).copy();
      thrownStack.setCount(1);
      EmiyaThrownWeaponEntity thrown = new EmiyaThrownWeaponEntity(level, player, thrownStack);
      Vec3 look = player.getLookAngle();
      Vec3 horizontal = horizontalLook(player);
      Vec3 side = new Vec3(-horizontal.z, 0.0, horizontal.x);
      double sideOffset = (hand == InteractionHand.MAIN_HAND ? 1.45 : -1.45);
      Vec3 spawn = player.getEyePosition().add(look.scale(0.65)).add(side.scale(sideOffset));
      thrown.setPos(spawn.x, spawn.y - 0.12, spawn.z);
      thrown.setFixedDamage(32.0F);
      thrown.setBreakLowHardnessBlocks(true);
      thrown.setNoGravity(true);
      thrown.setArcingFlight(horizontal, sideOffset, 7.0);
      thrown.shoot(look.x, look.y + 0.04, look.z, 2.6F, 0.0F);
      level.addFreshEntity(thrown);
      if (!player.isCreative()) {
         player.getItemInHand(hand).shrink(1);
      }
      level.playSound(null, player.blockPosition(), SoundEvents.TRIDENT_THROW.value(), SoundSource.PLAYERS, 0.8F, 1.55F);
      level.sendParticles(ParticleTypes.CRIT, spawn.x, spawn.y, spawn.z, 10, 0.12, 0.12, 0.12, 0.06);
   }

   private static void dashSlashOveredge(ServerPlayer player, InteractionHand hand) {
      if (!(player.level() instanceof ServerLevel level)) {
         return;
      }
      Vec3 dir = horizontalLook(player);
      player.setDeltaMovement(player.getDeltaMovement().add(dir.x * 1.85, 0.16, dir.z * 1.85));
      player.hurtMarked = true;
      Vec3 start = player.position().add(0.0, player.getBbHeight() * 0.48, 0.0);
      Vec3 end = start.add(dir.scale(5.5));
      AABB box = new AABB(start, end).inflate(1.25, 0.9, 1.25);
      Set<Integer> hit = new HashSet<>();
      for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && e != player && !EntityUtils.isImmunePlayerTarget(e))) {
         if (hit.add(target.getId())) {
            target.invulnerableTime = 0;
            target.hurt(player.damageSources().mobAttack(player), 144.0F);
            target.invulnerableTime = 0;
         }
      }
      for (double t = 0.0; t <= 1.0; t += 0.12) {
         Vec3 pos = start.lerp(end, t);
         level.sendParticles(ParticleTypes.SWEEP_ATTACK, pos.x, pos.y, pos.z, 2, 0.0, 0.0, 0.0, 0.0);
         level.sendParticles(ParticleTypes.CRIT, pos.x, pos.y, pos.z, 2, 0.12, 0.12, 0.12, 0.03);
      }
      level.playSound(null, player.blockPosition(), SoundEvents.TRIDENT_RIPTIDE_1.value(), SoundSource.PLAYERS, 1.0F, 1.25F);
      level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 0.85F);
      consumeProjectedOveredgeUse(player, hand);
   }

   private static void consumeProjectedOveredgeUse(ServerPlayer player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      CompoundTag tag = customTag(stack);
      if (tag == null || !tag.getBoolean("is_projected")) {
         return;
      }
      int uses = tag.getInt(OVEREDGE_USE_COUNT_TAG) + 1;
      if (uses >= 2 && !player.isCreative()) {
         player.setItemInHand(hand, ItemStack.EMPTY);
         player.level().playSound(null, player.blockPosition(), SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 1.0F, 0.75F);
         if (player.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.POOF, player.getX(), player.getY() + player.getBbHeight() * 0.55, player.getZ(), 12, 0.28, 0.34, 0.28, 0.04);
         }
      } else {
         updateCustomData(stack, data -> data.putInt(OVEREDGE_USE_COUNT_TAG, uses));
      }
   }

   private static void throwGaeBulgArmy(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level)) {
         return;
      }
      LivingEntity target = findLookTarget(player, 48.0, 2.0);
      GaeBulgArmyProjectileEntity projectile = new GaeBulgArmyProjectileEntity(level, player);
      projectile.setArmyDamage(500.0F);
      projectile.setPos(player.getX(), player.getEyeY() - 0.1, player.getZ());
      Vec3 aim = target != null ? target.position().add(0.0, target.getBbHeight() * 0.3, 0.0) : player.getEyePosition().add(player.getLookAngle().scale(48.0));
      Vec3 dir = aim.subtract(projectile.position()).normalize();
      projectile.shoot(dir.x, dir.y + 0.14, dir.z, 2.0F, 0.0F);
      level.addFreshEntity(projectile);
      level.playSound(null, player.blockPosition(), SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 1.5F, 0.65F);
   }

   private static void resolveGaeBulgHit(ServerPlayer player, LivingEntity target) {
      if (ArtoriaPendragonCombatHelper.tryNegateCertainHitOrDeath(target, "gae_bolg_player")) {
         return;
      }
      boolean deathThorn = target.isAlive()
         && !(target instanceof EmiyaArcherEntity)
         && !(target instanceof EnkiduEntity)
         && player.getRandom().nextFloat() < CuChulainnCombatHelper.getDeathThornChance(target);
      DamageSource source = player.damageSources().mobAttack(player);
      target.invulnerableTime = 0;
      target.hurt(source, 250.0F);
      target.invulnerableTime = 0;
      if (target.isAlive() && deathThorn) {
         target.hurt(source, Math.max(target.getMaxHealth() * 2.0F, 500.0F));
         if (target.isAlive()) {
            target.setHealth(0.0F);
            target.die(player.damageSources().genericKill());
         }
      }
      if (player.level() instanceof ServerLevel level) {
         level.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), 4, 0.0, 0.0, 0.0, 0.0);
         level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + target.getBbHeight() * 0.55, target.getZ(), 18, 0.25, 0.25, 0.25, 0.12);
         level.playSound(null, target.blockPosition(), SoundEvents.TRIDENT_HIT, SoundSource.PLAYERS, 1.1F, 0.7F);
      }
   }

   private static void performTsubame(ServerPlayer player, LivingEntity target) {
      if (!(player.level() instanceof ServerLevel level)) {
         return;
      }
      VFXServerEffects.spawn(level, "servant_sasaki_tsubame", player, 96.0);
      target.invulnerableTime = 0;
      target.hurt(player.damageSources().mobAttack(player), 300.0F);
      target.invulnerableTime = 0;
      Vec3 from = player.position().add(0.0, player.getBbHeight() * 0.5, 0.0);
      Vec3 to = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
      Vec3 slashDir = to.subtract(from);
      if (slashDir.lengthSqr() < 1.0E-4) {
         slashDir = player.getLookAngle();
      }
      slashDir = slashDir.normalize();
      Vec3 perp = new Vec3(-slashDir.z, 0.0, slashDir.x);
      for (int arc = 0; arc < 3; arc++) {
         double arcOffset = (arc - 1) * 0.4;
         for (double t = 0.0; t <= 1.0; t += 0.1) {
            Vec3 pos = from.lerp(to, t);
            double wave = Math.sin(t * Math.PI) * 0.3 * (arc + 1);
            pos = pos.add(perp.scale(wave + arcOffset));
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, pos.x, pos.y, pos.z, 2, 0.0, 0.0, 0.0, 0.0);
         }
      }
      level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, target.getX(), target.getY() + target.getBbHeight() * 0.3, target.getZ(), 25, 0.5, 0.6, 0.5, 0.08);
      level.sendParticles(ParticleTypes.REVERSE_PORTAL, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), 15, 0.4, 0.5, 0.4, 0.05);
      level.sendParticles(ParticleTypes.SOUL, target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(), 12, 0.3, 0.4, 0.3, 0.04);
      level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), 8, 0.3, 0.3, 0.3, 0.15);
      level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, player.getX(), player.getY() + player.getBbHeight() * 0.5, player.getZ(), 20, 0.3, 0.5, 0.3, 0.04);
      level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.8F, 0.5F);
      level.playSound(null, player.blockPosition(), SoundEvents.ENDER_EYE_DEATH, SoundSource.PLAYERS, 1.2F, 0.7F);
      level.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.5F, 0.6F);
   }

   private static LivingEntity findLookTarget(ServerPlayer player, double range, double inflate) {
      Vec3 eye = player.getEyePosition();
      Vec3 look = player.getLookAngle();
      Vec3 end = eye.add(look.scale(range));
      AABB box = player.getBoundingBox().expandTowards(look.scale(range)).inflate(inflate);
      EntityHitResult hit = ProjectileUtil.getEntityHitResult(
         player,
         eye,
         end,
         box,
         e -> e instanceof LivingEntity living && living.isAlive() && e != player && !EntityUtils.isImmunePlayerTarget(e),
         range * range
      );
      return hit != null && hit.getEntity() instanceof LivingEntity living ? living : null;
   }

   private static void triggerPseudoSpiralExplosion(ServerLevel level, LivingEntity owner, Vec3 center) {
      double radius = 4.5;
      Set<Integer> damaged = new HashSet<>();
      for (LivingEntity living : level.getEntitiesOfClass(
         LivingEntity.class,
         new AABB(center, center).inflate(radius),
         e -> e.isAlive() && e != owner && !EntityUtils.isImmunePlayerTarget(e)
      )) {
         if (damaged.add(living.getId())) {
            living.invulnerableTime = 0;
            living.hurt(owner.damageSources().explosion(null, owner), 10.0F);
            living.invulnerableTime = 0;
            Vec3 push = living.position().subtract(center).multiply(1.0, 0.0, 1.0);
            if (push.lengthSqr() > 1.0E-4) {
               push = push.normalize();
               living.push(push.x * 1.2, 0.42, push.z * 1.2);
               living.hurtMarked = true;
            }
         }
      }
      level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, center.x, center.y, center.z, 2, 0.2, 0.2, 0.2, 0.0);
      level.sendParticles(ParticleTypes.FLASH, center.x, center.y, center.z, 2, 0.0, 0.0, 0.0, 0.0);
      breakSmallExplosionTerrain(level, owner, center);
   }

   private static void performGallatinCone(ServerPlayer player, ServerLevel level, Vec3 look, float damage, float powerScale) {
      Vec3 origin = player.position().add(0.0, player.getBbHeight() * 0.55, 0.0);
      double range = GALLATIN_RANGE * (0.28 + powerScale * 0.72);
      double halfAngleCos = Math.cos(Math.toRadians(12.0 + 23.0 * powerScale));
      Set<Integer> hit = new HashSet<>();
      for (LivingEntity living : level.getEntitiesOfClass(
         LivingEntity.class,
         player.getBoundingBox().inflate(range + 3.0),
         e -> e.isAlive() && e != player && !EntityUtils.isImmunePlayerTarget(e)
      )) {
         Vec3 to = living.position().add(0.0, living.getBbHeight() * 0.45, 0.0).subtract(origin);
         Vec3 horizontal = new Vec3(to.x, 0.0, to.z);
         double distance = horizontal.length();
         if (distance > range || distance < 0.2) {
            continue;
         }
         Vec3 dir = horizontal.normalize();
         if (dir.dot(look) < halfAngleCos || !hit.add(living.getId())) {
            continue;
         }
         applyFixedDamageOverTicks(player, living, damage, 20);
         living.igniteForSeconds(5.0F);
         living.push(look.x * 5.0, 0.32, look.z * 5.0);
         living.hurtMarked = true;
      }
      spawnGallatinReleaseParticles(level, origin, look, range, powerScale);
      breakGallatinPath(level, origin, look, range, powerScale);
   }

   private static void applyFixedDamageOverTicks(ServerPlayer player, LivingEntity target, float totalDamage, int ticks) {
      int duration = Math.max(1, ticks);
      float perTick = totalDamage / duration;
      for (int delay = 0; delay < duration; delay++) {
         TYPE_MOON_WORLD.queueServerWork(delay, () -> {
            if (player.isAlive() && target.isAlive()) {
               applyFixedDamage(player, target, perTick);
            }
         });
      }
   }

   private static void applyFixedDamage(ServerPlayer player, LivingEntity target, float damage) {
      float before = target.getHealth();
      target.invulnerableTime = 0;
      target.hurt(player.damageSources().playerAttack(player), damage);
      target.invulnerableTime = 0;
      if (target.getPersistentData().getBoolean("GodHandActive")) {
         return;
      }
      float desired = Math.max(0.0F, before - damage);
      if (target.getHealth() > desired && target.getHealth() <= before) {
         target.setHealth(desired);
      }
   }

   private static void spawnGallatinReleaseParticles(ServerLevel level, Vec3 origin, Vec3 look, double range, float powerScale) {
      Vec3 right = new Vec3(-look.z, 0.0, look.x);
      for (double dist = 1.0; dist <= range; dist += 3.0) {
         double halfWidth = Math.min(20.0 * powerScale, dist * (0.18 + powerScale * 0.52));
         Vec3 center = origin.add(look.scale(dist));
         level.sendParticles(ParticleTypes.FLAME, center.x, center.y, center.z, 9, halfWidth * 0.3, 0.3, halfWidth * 0.3, 0.09);
         if (((int)dist) % 6 == 0) {
            level.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y - 0.2, center.z, 1, halfWidth * 0.16, 0.08, halfWidth * 0.16, 0.0);
         }
         if (((int)dist) % 9 == 0) {
            Vec3 edge = center.add(right.scale(level.random.nextBoolean() ? halfWidth : -halfWidth));
            level.sendParticles(ParticleTypes.FLAME, edge.x, edge.y, edge.z, 5, 0.2, 0.35, 0.2, 0.08);
         }
      }
   }

   private static void breakGallatinPath(ServerLevel level, Vec3 origin, Vec3 look, double range, float powerScale) {
      int broken = 0;
      int limit = Math.max(24, Mth.floor(220.0F * powerScale));
      Vec3 right = new Vec3(-look.z, 0.0, look.x);
      for (double dist = 2.0; dist <= range && broken < limit; dist += 2.0) {
         double halfWidth = Math.min(20.0 * powerScale, dist * (0.18 + powerScale * 0.52));
         for (double side = -halfWidth; side <= halfWidth && broken < limit; side += 2.0) {
            BlockPos center = BlockPos.containing(origin.add(look.scale(dist)).add(right.scale(side)));
            for (BlockPos pos : BlockPos.betweenClosed(center.offset(0, -1, 0), center.offset(0, 2, 0))) {
               BlockState state = level.getBlockState(pos);
               float hardness = state.getDestroySpeed(level, pos);
               if (!state.isAir()
                  && hardness >= 0.0F
                  && hardness < 55.0F
                  && !state.is(Blocks.BEDROCK)
                  && state.getExplosionResistance(level, pos, null) < 1200.0F
                  && level.removeBlock(pos, false)) {
                  broken++;
                  if (broken >= limit) {
                     break;
                  }
               }
            }
         }
      }
   }

   private static boolean isUnderGallatinSun(ServerLevel level, BlockPos pos) {
      long dayTime = level.getDayTime() % 24000L;
      return level.dimensionType().hasSkyLight()
         && dayTime >= 0L && dayTime < 12000L
         && !level.isRaining()
         && !level.isThundering()
         && level.canSeeSky(pos.above());
   }

   private static void breakSmallExplosionTerrain(ServerLevel level, LivingEntity owner, Vec3 center) {
      int broken = 0;
      for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(center).offset(-3, -3, -3), BlockPos.containing(center).offset(3, 3, 3))) {
         if (broken >= 48 || !pos.closerToCenterThan(center, 3.5)) {
            continue;
         }
         BlockState state = level.getBlockState(pos);
         float hardness = state.getDestroySpeed(level, pos);
         if (state.isAir() || state.is(Blocks.BEDROCK) || hardness < 0.0F || hardness > 50.0F || state.getExplosionResistance(level, pos, null) >= 1200.0F) {
            continue;
         }
         if (level.removeBlock(pos, false)) {
            broken++;
         }
      }
   }

   public static Vec3 horizontalLook(LivingEntity entity) {
      Vec3 look = entity.getLookAngle();
      Vec3 horizontal = new Vec3(look.x, 0.0, look.z);
      return horizontal.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : horizontal.normalize();
   }

   public static void startServantCardVoiceSession(ServerPlayer player, String servantId, SoundEvent sound) {
      startServantCardChargeVoice(player, servantId, sound);
   }

   public static boolean finishServantCardVoiceSession(ServerPlayer player, String servantId, SoundEvent longSound, SoundEvent shortSound) {
      return stopServantCardChargeVoice(player, servantId, longSound, shortSound);
   }

   private static void startServantCardChargeVoice(ServerPlayer player, String servantId, SoundEvent sound) {
      if (sound == null || !isServantCard(player, servantId) || !(player.level() instanceof ServerLevel level)) {
         return;
      }
      CompoundTag data = player.getPersistentData();
      String baseKey = chargeVoiceKey(servantId);
      long now = level.getGameTime();
      if (data.getBoolean(baseKey + "_active")) {
         return;
      }
      data.putBoolean(baseKey + "_active", true);
      data.putBoolean(baseKey + "_short", false);
      data.putBoolean(baseKey + "_ready_short", false);
      data.putLong(baseKey + "_start", now);
      level.playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.VOICE, 1.0F, 1.0F);
   }

   private static void tickServantCardChargeVoice(ServerPlayer player, String servantId, SoundEvent longSound, SoundEvent shortSound) {
      if (!isServantCard(player, servantId) || !(player.level() instanceof ServerLevel level)) {
         return;
      }
      CompoundTag data = player.getPersistentData();
      String baseKey = chargeVoiceKey(servantId);
      if (!data.getBoolean(baseKey + "_active")) {
         return;
      }
      long start = data.getLong(baseKey + "_start");
      if (level.getGameTime() - start >= SERVANT_CARD_CHARGE_SHORT_VOICE_TICKS) {
         data.putBoolean(baseKey + "_ready_short", true);
         data.putLong(baseKey + "_short_until", level.getGameTime() + 40L);
      }
   }

   private static boolean stopServantCardChargeVoice(ServerPlayer player, String servantId, SoundEvent longSound, SoundEvent shortSound) {
      CompoundTag data = player.getPersistentData();
      String baseKey = chargeVoiceKey(servantId);
      boolean active = data.getBoolean(baseKey + "_active");
      boolean playShort = false;
      long now = player.level() instanceof ServerLevel level ? level.getGameTime() : 0L;
      if (active) {
         stopSound(player, longSound);
         if (isServantCard(player, servantId) && shortSound != null && !data.getBoolean(baseKey + "_short") && player.level() instanceof ServerLevel level) {
            long start = data.getLong(baseKey + "_start");
            playShort = data.getBoolean(baseKey + "_ready_short") || level.getGameTime() - start >= SERVANT_CARD_CHARGE_SHORT_VOICE_TICKS;
            if (playShort) {
               data.putBoolean(baseKey + "_short", true);
               level.playSound(null, player.getX(), player.getY(), player.getZ(), shortSound, SoundSource.VOICE, 1.0F, 1.0F);
            }
         }
      } else if (isServantCard(player, servantId)
         && shortSound != null
         && !data.getBoolean(baseKey + "_short")
         && data.getLong(baseKey + "_short_until") >= now
         && player.level() instanceof ServerLevel level) {
         playShort = true;
         data.putBoolean(baseKey + "_short", true);
         level.playSound(null, player.getX(), player.getY(), player.getZ(), shortSound, SoundSource.VOICE, 1.0F, 1.0F);
      }
      long shortUntil = data.getLong(baseKey + "_short_until");
      data.remove(baseKey + "_active");
      data.remove(baseKey + "_short");
      data.remove(baseKey + "_ready_short");
      data.remove(baseKey + "_start");
      if (!playShort && shortUntil >= now) {
         data.putLong(baseKey + "_short_until", shortUntil);
      } else {
         data.remove(baseKey + "_short_until");
      }
      return playShort;
   }

   public static void revealArtoriaWindVeiledExcalibur(ServerPlayer player, int ticks) {
      if (player == null || !(player.level() instanceof ServerLevel level)) {
         return;
      }
      long until = level.getGameTime() + Math.max(1, ticks);
      player.getPersistentData().putLong(ARTORIA_WIND_REVEAL_UNTIL_TAG, until);
      revealArtoriaWindVeiledStack(player.getMainHandItem(), until);
      revealArtoriaWindVeiledStack(player.getOffhandItem(), until);
      player.getInventory().setChanged();
   }

   public static void revealArtoriaWindVeiledExcaliburForNp(ServerPlayer player, int ticks) {
      if (player == null || !(player.level() instanceof ServerLevel level)) {
         return;
      }
      long until = level.getGameTime() + Math.max(1, ticks);
      player.getPersistentData().putLong(ARTORIA_EXCALIBUR_WIND_LOCK_UNTIL_TAG, until);
      revealArtoriaWindVeiledExcalibur(player, ticks);
   }

   public static boolean isArtoriaExcaliburWindLocked(ServerPlayer player) {
      return player != null
         && player.level() instanceof ServerLevel level
         && player.getPersistentData().getLong(ARTORIA_EXCALIBUR_WIND_LOCK_UNTIL_TAG) > level.getGameTime();
   }

   public static void clearArtoriaExcaliburWindLock(ServerPlayer player) {
      if (player != null) {
         player.getPersistentData().remove(ARTORIA_EXCALIBUR_WIND_LOCK_UNTIL_TAG);
      }
   }

   private static void revealArtoriaWindVeiledStack(ItemStack stack, long until) {
      if (stack.isEmpty() || !stack.is(ModItems.EXCALIBUR.get())) {
         return;
      }
      updateCustomData(stack, tag -> {
         if (tag.getBoolean("ServantCardArtoriaWindVeiled")) {
            tag.putLong(ARTORIA_WIND_REVEAL_UNTIL_TAG, until);
         }
      });
   }

   private static void stopSound(ServerPlayer player, SoundEvent sound) {
      if (sound == null || !(player.level() instanceof ServerLevel level)) {
         return;
      }
      ResourceLocation location = sound.getLocation();
      ClientboundStopSoundPacket packet = new ClientboundStopSoundPacket(location, SoundSource.VOICE);
      double radiusSqr = SERVANT_CARD_CHARGE_VOICE_STOP_RADIUS * SERVANT_CARD_CHARGE_VOICE_STOP_RADIUS;
      for (ServerPlayer listener : level.getPlayers(listener -> listener.distanceToSqr(player) <= radiusSqr)) {
         listener.connection.send(packet);
      }
   }

   private static String chargeVoiceKey(String servantId) {
      return SERVANT_CARD_NP_CHARGE_VOICE_TAG + "_" + servantId;
   }

   private static boolean isServantCard(ServerPlayer player, String servantId) {
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      return vars.servant_card_transformed && servantId.equals(vars.servant_card_id);
   }

   private static CompoundTag customTag(ItemStack stack) {
      CustomData data = stack.get(DataComponents.CUSTOM_DATA);
      return data == null ? null : data.copyTag();
   }

   private static void updateCustomData(ItemStack stack, TagUpdater updater) {
      CompoundTag tag = customTag(stack);
      if (tag == null) {
         tag = new CompoundTag();
      }
      updater.update(tag);
      if (tag.isEmpty()) {
         stack.remove(DataComponents.CUSTOM_DATA);
      } else {
         stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
      }
   }

   @FunctionalInterface
   private interface TagUpdater {
      void update(CompoundTag tag);
   }
}
