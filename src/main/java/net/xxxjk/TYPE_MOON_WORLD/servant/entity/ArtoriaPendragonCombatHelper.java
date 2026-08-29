package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.entity.ArtoriaExcaliburBeamEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.ExpandingRingEffectEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.MedeaBeamEffectEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.MedeaMagicBoltEntity;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.AvalonItem;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantAiContext;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatPhase;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatSystem;
import net.xxxjk.TYPE_MOON_WORLD.servant.skill.ServantNoblePhantasmResourceService;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXServerEffects;

public final class ArtoriaPendragonCombatHelper {
   public static final String TAG_INVISIBLE_AIR_ACTIVE = "ArtoriaInvisibleAirActive";
   public static final String TAG_MANA_BURST_UNTIL = "ArtoriaManaBurstUntil";
   public static final String TAG_MANA_BURST_ACTIVE = "ArtoriaManaBurstActive";
   public static final String TAG_CHARISMA_UNTIL = "ArtoriaCharismaUntil";
   public static final String TAG_EXCALIBUR_WINDUP_UNTIL = "ArtoriaExcaliburWindupUntil";
   public static final String TAG_EXCALIBUR_RELEASE_UNTIL = "ArtoriaExcaliburReleaseUntil";
   public static final String TAG_HAS_AVALON = "ArtoriaHasAvalon";
   public static final String TAG_INVISIBLE_AIR_RELEASED = "ArtoriaInvisibleAirReleased";
   public static final String TAG_INVISIBLE_AIR_DAMAGE_BYPASS_UNTIL = "ArtoriaInvisibleAirDamageBypassUntil";
   public static final String TAG_WIND_REGATHER_UNTIL = "ArtoriaWindRegatherUntil";

   private static final String TAG_EXCALIBUR_CHARGE_START = "ArtoriaExcaliburChargeStart";
   private static final String TAG_EXCALIBUR_TARGET = "ArtoriaExcaliburTarget";
   private static final String TAG_LAST_MANA_BURST = "ArtoriaLastManaBurst";
   private static final String TAG_LAST_CHARISMA = "ArtoriaLastCharisma";
   private static final String TAG_LAST_INVISIBLE_AIR = "ArtoriaLastInvisibleAir";
   private static final String TAG_LAST_EXCALIBUR = "ArtoriaLastExcalibur";
   private static final String TAG_EXCALIBUR_BEAM_ID = "ArtoriaExcaliburBeamId";
   private static final String TAG_EXCALIBUR_POWER_SCALE = "ArtoriaExcaliburPowerScale";
   private static final String TAG_EXCALIBUR_OVERDRAFT = "ArtoriaExcaliburOverdraft";
   private static final String TAG_LAST_EXCALIBUR_CHARGE_VFX = "ArtoriaLastExcaliburChargeVfx";
   private static final String TAG_LAST_LION_LEAP = "ArtoriaLastLionLeap";
   private static final String TAG_LAST_AIR_CLEAVE = "ArtoriaLastAirCleave";
   private static final String TAG_LAST_BURST_DASH = "ArtoriaLastBurstDash";
   private static final String TAG_LAST_ROYAL_COMBO = "ArtoriaLastRoyalCombo";
   private static final String TAG_LAST_WIND_THRUST = "ArtoriaLastWindThrust";
   private static final String TAG_LAST_INVISIBLE_AIR_WRAP_VFX = "ArtoriaLastInvisibleAirWrapVfx";

   private static final int MANA_BURST_DURATION = 200;
   private static final int MANA_BURST_COOLDOWN = 300;
   private static final int CHARISMA_DURATION = 600;
   private static final int CHARISMA_COOLDOWN = 700;
   private static final int INVISIBLE_AIR_DURATION = 80;
   private static final int INVISIBLE_AIR_COOLDOWN = 200;
   private static final int INVISIBLE_AIR_RELEASE_COOLDOWN = 100;
   private static final float INVISIBLE_AIR_DAMAGE = 300.0F;
   private static final float MANA_BURST_INVISIBLE_AIR_DAMAGE_MULTIPLIER = 4.0F / 3.0F;
   private static final int EXCALIBUR_CHANT = 120;
   private static final int EXCALIBUR_CHARGE = 40;
   private static final int EXCALIBUR_WINDUP = EXCALIBUR_CHANT + EXCALIBUR_CHARGE;
   private static final int EXCALIBUR_RELEASE = 150;
   private static final int EXCALIBUR_DAMAGE_START_TICK = 58;
   private static final int EXCALIBUR_COOLDOWN = 1200;
   private static final double EXCALIBUR_RANGE = 150.0;
   private static final int LION_LEAP_COOLDOWN = 80;
   private static final int AIR_CLEAVE_COOLDOWN = 70;
   private static final int BURST_DASH_COOLDOWN = 90;
   private static final int ROYAL_COMBO_COOLDOWN = 85;
   private static final int WIND_THRUST_COOLDOWN = 65;
   private static final double ARTORIA_VFX_RADIUS = 128.0;
   private static final double MANA_BURST_JET_ACCEL = 0.095;
   private static final double MANA_BURST_JET_GROUND_ACCEL = 0.135;
   private static final double MANA_BURST_JET_CAP = 0.72;
   private static final double MANA_BURST_JET_GROUND_LIFT = 0.38;
   private static final double MANA_BURST_JET_AIR_LIFT = 0.065;
   private static final double MANA_BURST_JET_UP_CAP = 0.55;
   private static final ResourceLocation RIDING_SPEED_ID = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "artoria_riding_speed");
   private static final ResourceLocation RIDING_ARMOR_ID = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "artoria_riding_armor");
   private static final ResourceLocation MANA_BURST_ATTACK_ID = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "artoria_mana_burst_attack");
   private static final ResourceLocation MANA_BURST_SPEED_ID = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "artoria_mana_burst_speed");
   private static final ResourceLocation EXCALIBUR_REVEALED_ATTACK_ID = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "artoria_revealed_excalibur_attack");
   private static final double REVEALED_EXCALIBUR_BONUS = 117.0;
   private static final ResourceLocation CHARISMA_ATTACK_ID = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "artoria_charisma_attack");
   private static final String TAG_LAST_PERSISTENT_TICK = "ArtoriaLastPersistentStateTick";
   private static final String TAG_PERSISTENT_BUSY = "ArtoriaPersistentStateBusy";

   private ArtoriaPendragonCombatHelper() {
   }

   public static boolean tick(ArtoriaPendragonEntity entity, ServantAiContext context) {
      if (!(entity.level() instanceof ServerLevel level)) {
         return false;
      }

      long now = level.getGameTime();
      CompoundTag data = entity.getPersistentData();
      if (tickPersistentState(entity)) {
         return true;
      }
      LivingEntity target = context.target();
      if (target == null || !target.isAlive() || EntityUtils.isImmunePlayerTarget(target)) {
         return false;
      }
      if (ServantCombatSystem.skillsSuppressed(entity) || ServantCombatSystem.cannotAct(entity)) {
         return false;
      }

      double distance = entity.distanceTo(target);
      if (entity.getHealth() <= entity.getMaxHealth() * 0.35F) {
         ServantCombatSystem.forcePhaseAtLeast(entity, ServantCombatPhase.DECISIVE);
      }
      ServantCombatPhase phase = ServantCombatSystem.getPhase(entity);
      boolean normalOrDecisive = phase.id() >= ServantCombatPhase.NORMAL.id();
      boolean decisive = phase == ServantCombatPhase.DECISIVE;
      entity.getLookControl().setLookAt(target, 40.0F, 40.0F);
      if (decisive && tryStartExcalibur(entity, target, level, data, now)) {
         return true;
      }
      if (normalOrDecisive && tryManaBurst(entity, level, data, now)) {
         return true;
      }
      if (normalOrDecisive && tryCharisma(entity, level, data, now)) {
         return true;
      }
      if (tryArtoriaSmallSkill(entity, target, level, data, now, distance)) {
         return true;
      }
      if (normalOrDecisive && distance <= 5.5 && tryInvisibleAirRelease(entity, target, level, data, now)) {
         return true;
      }
      if (normalOrDecisive && distance >= 3.0 && distance <= 12.0 && tryInvisibleAir(entity, target, level, data, now)) {
         return true;
      }
      return false;
   }

   public static boolean hasAvalon(LivingEntity entity) {
      if (entity instanceof ArtoriaPendragonEntity && entity.getPersistentData().getBoolean(TAG_HAS_AVALON)) {
         return true;
      }
      return entity instanceof Player player && hasActiveAvalonInInventory(player);
   }

   private static boolean hasActiveAvalonInInventory(Player player) {
      for (ItemStack stack : player.getInventory().items) {
         if (isActiveAvalonFor(player, stack)) {
            return true;
         }
      }
      for (ItemStack stack : player.getInventory().offhand) {
         if (isActiveAvalonFor(player, stack)) {
            return true;
         }
      }
      return false;
   }

   private static boolean isActiveAvalonFor(Player player, ItemStack stack) {
      if (!stack.is(ModItems.AVALON.get())) {
         return false;
      }
      if (AvalonItem.isAvalonActivated(stack)) {
         return true;
      }
      if (player instanceof ServerPlayer serverPlayer) {
         TypeMoonWorldModVariables.PlayerVariables vars = serverPlayer.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         return vars.servant_card_transformed && "artoria_pendragon".equals(vars.servant_card_id);
      }
      return false;
   }

   public static boolean tickPersistentState(ArtoriaPendragonEntity entity) {
      if (!(entity.level() instanceof ServerLevel level) || !entity.isAlive()) return false;
      long now = level.getGameTime();
      CompoundTag data = entity.getPersistentData();
      if (data.contains(TAG_LAST_PERSISTENT_TICK) && data.getLong(TAG_LAST_PERSISTENT_TICK) == now) {
         return data.getBoolean(TAG_PERSISTENT_BUSY);
      }
      data.putLong(TAG_LAST_PERSISTENT_TICK, now);
      tickAvalon(entity, level);
      tickLakeProtection(entity);
      tickRidingB(entity);
      tickTimedModifiers(entity, now);
      tickInvisibleAirCleanup(data, now);
      syncExcaliburVisibility(entity);
      tickInvisibleAirWrapVfx(entity, level, data, now);
      boolean busy = tickExcaliburState(entity, entity.getTarget(), level, data, now);
      data.putBoolean(TAG_PERSISTENT_BUSY, busy);
      return busy;
   }

   public static boolean isInvisibleAirActive(ArtoriaPendragonEntity entity) {
      if (entity == null) {
         return false;
      }
      CompoundTag data = entity.getPersistentData();
      return data.getBoolean(TAG_INVISIBLE_AIR_ACTIVE) && data.getLong(TAG_INVISIBLE_AIR_ACTIVE + "Until") > entity.level().getGameTime();
   }

   public static boolean isInvisibleAirRevealed(LivingEntity entity) {
      if (entity == null) {
         return false;
      }
      CompoundTag data = entity.getPersistentData();
      return data.getBoolean(TAG_INVISIBLE_AIR_ACTIVE) && data.getLong(TAG_INVISIBLE_AIR_ACTIVE + "Until") > entity.level().getGameTime();
   }

   public static boolean hasReleasedInvisibleAir(ArtoriaPendragonEntity entity) {
      return entity != null && entity.getPersistentData().getBoolean(TAG_INVISIBLE_AIR_RELEASED);
   }

   public static void syncExcaliburVisibility(ArtoriaPendragonEntity entity) {
      if (entity == null || entity.level().isClientSide()) {
         return;
      }
      CompoundTag data = entity.getPersistentData();
      if (hasAvalon(entity)) {
         ServantCombatSystem.forcePhaseAtLeast(entity, ServantCombatPhase.DECISIVE);
      }
      boolean visible = shouldRenderExcalibur(entity, data, entity.level().getGameTime());
      entity.setExcaliburVisible(visible);
      // Humanoid NPCs use the vanilla held-item layer. Keep the synchronized
      // visibility state and the actual equipment in lockstep so clients do
      // not render a stale sword or an empty hand indefinitely.
      ItemStack hand = entity.getMainHandItem();
      if (visible) {
         if (!hand.is(ModItems.EXCALIBUR.get())) {
            entity.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.EXCALIBUR.get()));
            entity.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
         }
      } else if (hand.is(ModItems.EXCALIBUR.get())) {
         entity.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
      }
   }

   private static boolean shouldRenderExcalibur(ArtoriaPendragonEntity entity, CompoundTag data, long now) {
      if (isInvisibleAirActive(entity)) {
         return false;
      }
      ServantCombatPhase phase = ServantCombatSystem.getPhase(entity);
      if (hasAvalon(entity) || phase == ServantCombatPhase.DECISIVE) {
         return true;
      }
      if (phase == ServantCombatPhase.PROBING) {
         return false;
      }
      return data.getLong(TAG_LAST_INVISIBLE_AIR) > 0L && now - data.getLong(TAG_LAST_INVISIBLE_AIR) < INVISIBLE_AIR_COOLDOWN;
   }

   public static boolean isManaBurstActive(LivingEntity entity) {
      return entity != null && (entity.getPersistentData().getBoolean(TAG_MANA_BURST_ACTIVE)
         || entity.getPersistentData().getLong(TAG_MANA_BURST_UNTIL) > entity.level().getGameTime());
   }

   public static float applyManaBurstDefense(LivingEntity target, DamageSource source, float amount) {
      if (target == null || source == null || amount <= 0.0F || !isManaBurstActive(target)
         || source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
         return amount;
      }
      if (target.level() instanceof ServerLevel level && target.tickCount % 10 == 0) {
         level.sendParticles(ParticleTypes.END_ROD, target.getX(), target.getY() + target.getBbHeight() * 0.58,
            target.getZ(), 8, 0.25, 0.35, 0.25, 0.025);
         level.playSound(null, target.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.HOSTILE, 0.55F, 1.35F);
      }
      return amount * 0.30F;
   }

   public static boolean isExcaliburWindingOrReleasing(LivingEntity entity) {
      if (!(entity instanceof ArtoriaPendragonEntity artoria)) {
         return false;
      }
      long now = artoria.level().getGameTime();
      CompoundTag data = artoria.getPersistentData();
      return data.getLong(TAG_EXCALIBUR_WINDUP_UNTIL) > now || data.getLong(TAG_EXCALIBUR_RELEASE_UNTIL) > now;
   }

   public static boolean tryNegateCertainHitOrDeath(LivingEntity target, String reason) {
      if (tryProtectWithAvalon(target)) {
         return true;
      }
      if (!(target instanceof ArtoriaPendragonEntity artoria)) {
         return false;
      }
      if (artoria.getPersistentData().getBoolean("ArtoriaInstinctAActive") && artoria.getRandom().nextFloat() < 0.95F) {
         spawnInstinctFx(artoria, false);
         return true;
      }
      return false;
   }

   public static boolean tryDodgeProjectileWithInstinct(ArtoriaPendragonEntity artoria, Projectile projectile) {
      if (artoria == null || projectile == null || projectile.getOwner() == artoria) {
         return false;
      }
      if (!artoria.getPersistentData().getBoolean("ArtoriaInstinctAActive")) {
         return false;
      }
      if (artoria.getRandom().nextFloat() >= 0.90F) {
         return false;
      }
      spawnInstinctFx(artoria, false);
      return true;
   }

   public static float applyAvalonDamageReduction(LivingEntity target, DamageSource source, float amount) {
      if (amount <= 0.0F || !hasAvalon(target)) {
         return amount;
      }
      tryProtectWithAvalon(target);
      return 0.0F;
   }

   public static boolean tryProtectWithAvalon(LivingEntity target) {
      if (target == null || !hasAvalon(target)) {
         return false;
      }
      CompoundTag data = target.getPersistentData();
      data.remove("CausalSevered");
      data.remove("MasterLossForcedDeath");
      data.remove("MasterLossDecayDamage");
      target.clearFire();
      target.invulnerableTime = Math.max(target.invulnerableTime, 20);
      target.setHealth(target.getMaxHealth());
      target.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 80, 4, false, false, true));
      target.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 80, 4, false, false, true));
      spawnAvalonFx(target);
      return true;
   }

   public static boolean tryNegateMedeaSmallMagic(ArtoriaPendragonEntity entity, DamageSource source, float amount) {
      if (entity == null || source == null || amount <= 0.0F || !isMedeaSmallMagic(source, amount)) {
         return false;
      }
      if (entity.level() instanceof ServerLevel level) {
         level.sendParticles(ParticleTypes.END_ROD, entity.getX(), entity.getY() + entity.getBbHeight() * 0.62, entity.getZ(), fxCount(18), 0.34, 0.45, 0.34, 0.03);
         level.sendParticles(ParticleTypes.END_ROD, entity.getX(), entity.getY() + entity.getBbHeight() * 0.62, entity.getZ(), fxCount(12), 0.26, 0.35, 0.26, 0.02);
         level.playSound(null, entity.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.HOSTILE, 0.85F, 1.7F);
      }
      return true;
   }

   public static float applyManaBurstOutgoing(LivingEntity attacker, float amount) {
      if (!isManaBurstActive(attacker) || amount <= 0.0F) {
         return amount;
      }
      return amount * 2.0F + 20.0F;
   }

   public static void tickLakeProtection(LivingEntity entity) {
      if (entity == null || entity.level().isClientSide() || entity.isShiftKeyDown() || entity.isPassenger()) {
         return;
      }
      BlockPos feet = BlockPos.containing(entity.getX(), entity.getY() - 0.08, entity.getZ());
      BlockPos below = feet.below();
      boolean waterAtFeet = entity.level().getFluidState(feet).is(FluidTags.WATER);
      boolean waterBelow = entity.level().getFluidState(below).is(FluidTags.WATER);
      if (!waterAtFeet && !waterBelow) {
         return;
      }
      double surfaceY = (waterAtFeet ? feet.getY() : below.getY()) + 1.02;
      double lift = Mth.clamp((surfaceY - entity.getY()) * 0.42, -0.04, 0.18);
      Vec3 motion = entity.getDeltaMovement();
      entity.setDeltaMovement(motion.x, Math.max(motion.y, lift), motion.z);
      entity.setOnGround(true);
      entity.fallDistance = 0.0F;
      if (entity.tickCount % 10 == 0 && entity.level() instanceof ServerLevel level) {
         level.sendParticles(ParticleTypes.SPLASH, entity.getX(), surfaceY - 0.05, entity.getZ(), fxCount(4), 0.28, 0.02, 0.28, 0.02);
         level.sendParticles(ParticleTypes.END_ROD, entity.getX(), surfaceY + 0.02, entity.getZ(), fxCount(2), 0.22, 0.02, 0.22, 0.005);
      }
   }

   public static void tickSharedBuffCleanup(LivingEntity entity) {
      if (entity == null || entity.level().isClientSide()) {
         return;
      }
      long charismaUntil = entity.getPersistentData().getLong(TAG_CHARISMA_UNTIL);
      if (charismaUntil > 0L && charismaUntil <= entity.level().getGameTime()) {
         removeModifier(entity.getAttribute(Attributes.ATTACK_DAMAGE), CHARISMA_ATTACK_ID);
         entity.getPersistentData().remove(TAG_CHARISMA_UNTIL);
      }
   }

   public static void spawnAvalonFx(LivingEntity entity) {
      if (entity.level() instanceof ServerLevel level) {
         level.sendParticles(ParticleTypes.END_ROD, entity.getX(), entity.getY() + entity.getBbHeight() * 0.64, entity.getZ(), fxCount(12), 0.22, 0.28, 0.22, 0.02);
         level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, entity.getX(), entity.getY() + entity.getBbHeight() * 0.48, entity.getZ(), fxCount(6), 0.22, 0.26, 0.22, 0.03);
         level.playSound(null, entity.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 0.85F, 1.45F);
      }
   }

   private static void tickAvalon(ArtoriaPendragonEntity entity, ServerLevel level) {
      if (!hasAvalon(entity) || !entity.isAlive()) {
         return;
      }
      entity.heal(entity.getMaxHealth() * 0.05F);
      spawnAvalonPassiveFx(entity, level);
   }

   private static void spawnAvalonPassiveFx(ArtoriaPendragonEntity entity, ServerLevel level) {
      double time = entity.tickCount * 0.32;
      double cx = entity.getX();
      double cy = entity.getY();
      double cz = entity.getZ();
      double height = entity.getBbHeight();

      level.sendParticles(ParticleTypes.CLOUD, cx, cy + height * 0.54, cz, fxCount(3), 0.32, 0.26, 0.32, 0.015);
      level.sendParticles(ParticleTypes.END_ROD, cx, cy + height * 0.66, cz, fxCount(2), 0.24, 0.28, 0.24, 0.015);
      level.sendParticles(ParticleTypes.END_ROD, cx, cy + 0.18, cz, fxCount(3), 0.42, 0.06, 0.42, 0.03);

      for (int i = 0; i < fxCount(8); i++) {
         double angle = time + i * Math.PI * 0.5;
         double radius = 0.55 + 0.12 * Math.sin(time * 0.7 + i);
         double y = cy + 0.25 + (i % 4) * height * 0.23;
         double x = cx + Math.cos(angle) * radius;
         double z = cz + Math.sin(angle) * radius;
         double swirlX = -Math.sin(angle) * 0.018;
         double swirlZ = Math.cos(angle) * 0.018;
         level.sendParticles(ParticleTypes.END_ROD, x, y, z, 1, 0.015, 0.015, 0.015, 0.0);
         level.sendParticles(ParticleTypes.END_ROD, x, y + 0.05, z, 1, swirlX, 0.022, swirlZ, 0.0);
      }

      if (entity.tickCount % 16 == 0) {
         level.sendParticles(ParticleTypes.ENCHANT, cx, cy + height * 0.6, cz, fxCount(6), 0.45, 0.32, 0.45, 0.025);
      }
      if (entity.tickCount % 28 == 0) {
         level.addFreshEntity(new ExpandingRingEffectEntity(level, cx, cy + 0.16, cz, 0.18F, 1.65F, 0.04F, 14, 0xEFFFF8, 0.18F, 0.014F));
         level.addFreshEntity(new ExpandingRingEffectEntity(level, cx, cy + height * 0.52, cz, 0.12F, 1.0F, 0.03F, 12, 0xCFFFEF, 0.12F, 0.01F, 82.0F, (entity.tickCount * 7) % 360));
      }
   }

   private static void tickRidingB(ArtoriaPendragonEntity entity) {
      updateModifier(entity.getAttribute(Attributes.MOVEMENT_SPEED), RIDING_SPEED_ID, entity.isPassenger() ? 0.20 : 0.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
      updateModifier(entity.getAttribute(Attributes.ARMOR), RIDING_ARMOR_ID, entity.isPassenger() ? 0.10 : 0.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
   }

   private static void tickTimedModifiers(ArtoriaPendragonEntity entity, long now) {
      CompoundTag data = entity.getPersistentData();
      boolean manaBurst = isManaBurstActive(entity);
      updateModifier(entity.getAttribute(Attributes.ATTACK_DAMAGE), MANA_BURST_ATTACK_ID, manaBurst ? 1.0 : 0.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
      updateModifier(entity.getAttribute(Attributes.ATTACK_SPEED), MANA_BURST_SPEED_ID, manaBurst ? 0.30 : 0.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
      updateModifier(entity.getAttribute(Attributes.ATTACK_DAMAGE), EXCALIBUR_REVEALED_ATTACK_ID,
         entity.isExcaliburVisible() ? REVEALED_EXCALIBUR_BONUS : 0.0, AttributeModifier.Operation.ADD_VALUE);
      if (manaBurst && entity.level() instanceof ServerLevel level) {
         spawnManaBurstSustainFx(entity, level);
         tickManaBurstJetMovement(entity, level);
         if (entity.tickCount % 20 == 0) {
            entity.setCurrentMp(Math.max(0.0, entity.getCurrentMp() - 6.0));
            if (entity.getCurrentMp() <= 0.0) {
               data.putBoolean(TAG_MANA_BURST_ACTIVE, false);
               data.remove(TAG_MANA_BURST_UNTIL);
            }
         }
      }
      if (entity.getPersistentData().getLong(TAG_CHARISMA_UNTIL) <= now) {
         removeModifier(entity.getAttribute(Attributes.ATTACK_DAMAGE), CHARISMA_ATTACK_ID);
      }
   }

   private static void tickManaBurstJetMovement(ArtoriaPendragonEntity entity, ServerLevel level) {
      if (isExcaliburWindingOrReleasing(entity) || entity.isPassenger()) {
         return;
      }
      LivingEntity target = entity.getTarget();
      Vec3 desired = Vec3.ZERO;
      boolean lift = entity.horizontalCollision;
      if (target != null && target.isAlive() && !target.isAlliedTo(entity) && !EntityUtils.isImmunePlayerTarget(target)) {
         Vec3 toTarget = target.position().subtract(entity.position());
         Vec3 horizontal = new Vec3(toTarget.x, 0.0, toTarget.z);
         double distance = horizontal.length();
         if (distance > 2.35) {
            desired = horizontal.normalize();
         }
         lift = lift || toTarget.y > 0.75 && distance < 13.0;
      } else if (entity.getNavigation() != null && !entity.getNavigation().isDone()) {
         desired = horizontalLook(entity);
      }

      Vec3 motion = entity.getDeltaMovement();
      Vec3 next = motion;
      boolean moved = false;
      if (desired.lengthSqr() > 1.0E-4) {
         double accel = entity.onGround() ? MANA_BURST_JET_GROUND_ACCEL : MANA_BURST_JET_ACCEL;
         next = clampHorizontal(next.add(desired.scale(accel)), MANA_BURST_JET_CAP);
         moved = true;
      }
      if (lift) {
         double liftAmount = entity.onGround() ? MANA_BURST_JET_GROUND_LIFT : MANA_BURST_JET_AIR_LIFT;
         next = new Vec3(next.x, Math.min(MANA_BURST_JET_UP_CAP, Math.max(next.y + liftAmount, entity.onGround() ? MANA_BURST_JET_GROUND_LIFT : next.y)), next.z);
         moved = true;
      } else if (!entity.onGround() && next.y < -0.28) {
         next = new Vec3(next.x, next.y * 0.78, next.z);
         moved = true;
      }
      if (moved && !next.equals(motion)) {
         entity.setDeltaMovement(next);
         entity.hurtMarked = true;
      }
      if ((moved || desired.lengthSqr() > 1.0E-4) && entity.tickCount % 2 == 0) {
         spawnManaBurstJetFx(entity, level, desired);
      }
   }

   private static Vec3 clampHorizontal(Vec3 motion, double cap) {
      double horizontal = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
      if (horizontal <= cap || horizontal <= 1.0E-6) {
         return motion;
      }
      double scale = cap / horizontal;
      return new Vec3(motion.x * scale, motion.y, motion.z * scale);
   }

   private static void tickInvisibleAirCleanup(CompoundTag data, long now) {
      if (data.getLong(TAG_INVISIBLE_AIR_ACTIVE + "Until") <= now) {
         data.putBoolean(TAG_INVISIBLE_AIR_ACTIVE, false);
      }
   }

   private static boolean tickExcaliburState(ArtoriaPendragonEntity entity, LivingEntity target, ServerLevel level, CompoundTag data, long now) {
      LivingEntity lockedTarget = getExcaliburTarget(entity, target, level, data);
      long windupUntil = data.getLong(TAG_EXCALIBUR_WINDUP_UNTIL);
      if (windupUntil > now) {
         if (!entity.isAlive()) {
            data.remove(TAG_EXCALIBUR_WINDUP_UNTIL);
            data.remove(TAG_EXCALIBUR_CHARGE_START);
            data.remove(TAG_EXCALIBUR_TARGET);
            level.sendParticles(ParticleTypes.SMOKE, entity.getX(), entity.getY() + entity.getBbHeight() * 0.6, entity.getZ(), fxCount(18), 0.3, 0.4, 0.3, 0.03);
            return true;
         }
         if (lockedTarget != null && lockedTarget.isAlive()) {
            faceExcaliburTarget(entity, lockedTarget);
         }
         boolean charging = data.getLong(TAG_EXCALIBUR_CHARGE_START) > 0L && now >= data.getLong(TAG_EXCALIBUR_CHARGE_START);
         if (charging) {
            entity.getNavigation().stop();
            entity.setDeltaMovement(entity.getDeltaMovement().multiply(0.35, 1.0, 0.35));
            if (now - data.getLong(TAG_LAST_EXCALIBUR_CHARGE_VFX) >= 32L) {
               data.putLong(TAG_LAST_EXCALIBUR_CHARGE_VFX, now);
               VFXServerEffects.spawn(level, "artoria_excalibur_charge", entity, ARTORIA_VFX_RADIUS);
            }
            spawnExcaliburChargeFx(entity, level);
         } else if (entity.tickCount % 20 == 0) {
            level.sendParticles(ParticleTypes.END_ROD, entity.getX(), entity.getY() + entity.getBbHeight() * 0.7, entity.getZ(), fxCount(4), 0.18, 0.25, 0.18, 0.01);
         }
         return true;
      }
      if (windupUntil > 0) {
         releaseExcalibur(entity, lockedTarget, level, data, now);
         return true;
      }

      long releaseUntil = data.getLong(TAG_EXCALIBUR_RELEASE_UNTIL);
      if (releaseUntil > now) {
         entity.getNavigation().stop();
         entity.setDeltaMovement(entity.getDeltaMovement().multiply(0.35, 1.0, 0.35));
         if (lockedTarget != null && lockedTarget.isAlive()) {
            faceExcaliburTarget(entity, lockedTarget);
         }
         return true;
      }
      if (releaseUntil > 0) {
         data.remove(TAG_EXCALIBUR_RELEASE_UNTIL);
         data.remove(TAG_EXCALIBUR_BEAM_ID);
         data.remove(TAG_EXCALIBUR_TARGET);
      }
      return false;
   }

   private static boolean tryStartExcalibur(ArtoriaPendragonEntity entity, LivingEntity target, ServerLevel level, CompoundTag data, long now) {
      ServantNoblePhantasmResourceService.CastDecision resource =
         ServantNoblePhantasmResourceService.evaluateNpcCast(entity, 150.0);
      int previousCooldown = data.getBoolean(TAG_EXCALIBUR_OVERDRAFT) ? EXCALIBUR_COOLDOWN * 2 : EXCALIBUR_COOLDOWN;
      if (!resource.allowed() || ServantNoblePhantasmResourceService.isOverdraftWeak(entity)
         || now - data.getLong(TAG_LAST_EXCALIBUR) < previousCooldown
         || entity.distanceTo(target) > EXCALIBUR_RANGE) {
         return false;
      }
      boolean highHealth = target.getMaxHealth() >= 200.0F || target.getHealth() >= 150.0F;
      int clustered = level.getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(7.5), e -> e.isAlive() && e != entity && !e.isAlliedTo(entity) && !EntityUtils.isImmunePlayerTarget(e)).size();
      if (!highHealth && clustered < 3) {
         return false;
      }
      data.putLong(TAG_LAST_EXCALIBUR, now);
      data.putDouble(TAG_EXCALIBUR_POWER_SCALE, resource.powerScale());
      data.putBoolean(TAG_EXCALIBUR_OVERDRAFT, resource.overdraft());
      ServantNoblePhantasmResourceService.commitNpcCast(entity, resource);
      data.putLong(TAG_EXCALIBUR_WINDUP_UNTIL, now + EXCALIBUR_WINDUP);
      data.putLong(TAG_EXCALIBUR_CHARGE_START, now + EXCALIBUR_CHANT);
      data.putUUID(TAG_EXCALIBUR_TARGET, target.getUUID());
      entity.getNavigation().stop();
      faceExcaliburTarget(entity, target);
      entity.triggerHorizontalSwingAnimation();
      ServantVoiceHelper.tryPlayArtoriaExcalibur(entity);
      ServantCombatSystem.broadcastNoblePhantasmWindup(entity, target, EXCALIBUR_WINDUP, true);
      level.playSound(null, entity.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 1.4F, 1.2F);
      level.sendParticles(ParticleTypes.ENCHANT, entity.getX(), entity.getY() + entity.getBbHeight() * 0.75, entity.getZ(), fxCount(36), 0.75, 0.75, 0.75, 0.08);
      level.addFreshEntity(new ExpandingRingEffectEntity(level, entity.getX(), entity.getY() + 0.22, entity.getZ(), 0.35F, 3.4F, 0.08F, 24, 0xF7F7FF, 0.42F, 0.025F));
      return true;
   }

   private static void releaseExcalibur(ArtoriaPendragonEntity entity, LivingEntity target, ServerLevel level, CompoundTag data, long now) {
      data.remove(TAG_EXCALIBUR_WINDUP_UNTIL);
      data.remove(TAG_EXCALIBUR_CHARGE_START);
      if (!entity.isAlive()) {
         return;
      }
      data.putLong(TAG_EXCALIBUR_RELEASE_UNTIL, now + EXCALIBUR_RELEASE);
      entity.triggerHorizontalSwingAnimation();
      Vec3 start = entity.position().add(0.0, entity.getBbHeight() * 0.66, 0.0).add(excaliburLook(entity).scale(1.2));
      float powerScale = (float)Math.max(0.2, Math.min(1.0, data.getDouble(TAG_EXCALIBUR_POWER_SCALE)));
      ArtoriaExcaliburBeamEntity beam = new ArtoriaExcaliburBeamEntity(
         level, entity, start, EXCALIBUR_RELEASE, EXCALIBUR_DAMAGE_START_TICK, powerScale);
      level.addFreshEntity(beam);
      data.putInt(TAG_EXCALIBUR_BEAM_ID, beam.getId());
      VFXServerEffects.spawn(level, "artoria_excalibur_beam", entity, 192.0);
      spawnExcaliburReleaseFx(entity, level, start);
      level.playSound(null, entity.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 2.5F, 0.85F);
      level.playSound(null, entity.blockPosition(), SoundEvents.END_PORTAL_SPAWN, SoundSource.HOSTILE, 1.1F, 1.65F);
   }

   private static boolean tryArtoriaSmallSkill(ArtoriaPendragonEntity entity, LivingEntity target, ServerLevel level, CompoundTag data, long now, double distance) {
      double vertical = target.getY() - entity.getY();
      if ((vertical > 1.4 || distance > 5.5) && distance <= 22.0 && tryLionLeap(entity, target, level, data, now)) {
         return true;
      }
      if (vertical > 1.0 && distance <= 8.0 && tryAirCleave(entity, target, level, data, now)) {
         return true;
      }
      if (distance >= 8.0 && distance <= 28.0 && tryBurstDash(entity, target, level, data, now)) {
         return true;
      }
      if (distance <= 4.8 && entity.getRandom().nextFloat() < 0.42F && tryRoyalCombo(entity, target, level, data, now)) {
         return true;
      }
      return distance >= 4.0 && distance <= 16.0 && tryWindThrust(entity, target, level, data, now);
   }

   private static boolean tryLionLeap(ArtoriaPendragonEntity entity, LivingEntity target, ServerLevel level, CompoundTag data, long now) {
      if (!consumeSmallSkill(entity, data, now, TAG_LAST_LION_LEAP, LION_LEAP_COOLDOWN, 8.0)) {
         return false;
      }
      entity.faceToward(target.position());
      entity.triggerJumpAttackAnimation();
      Vec3 start = entity.position();
      Vec3 targetCenter = target.position().add(0.0, target.getBbHeight() * 0.55, 0.0);
      Vec3 motion = targetCenter.subtract(start).normalize().scale(1.35);
      entity.setDeltaMovement(motion.x, Math.max(0.42, motion.y + 0.35), motion.z);
      spawnDashTrail(level, start.add(0.0, 0.5, 0.0), targetCenter, ParticleTypes.CLOUD, fxCount(16));
      TYPE_MOON_WORLD.queueServerWork(7, () -> {
         if (entity.isAlive() && target.isAlive() && entity.level() instanceof ServerLevel serverLevel && entity.distanceToSqr(target) <= 49.0) {
            entity.faceToward(target.position());
            damageTarget(entity, target, (float)(entity.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.45 + 8.0));
            target.push(entity.getLookAngle().x * 0.35, 0.22, entity.getLookAngle().z * 0.35);
            target.hurtMarked = true;
            serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(), fxCount(3), 0.0, 0.0, 0.0, 0.0);
         }
      });
      return true;
   }

   private static boolean tryAirCleave(ArtoriaPendragonEntity entity, LivingEntity target, ServerLevel level, CompoundTag data, long now) {
      if (!consumeSmallSkill(entity, data, now, TAG_LAST_AIR_CLEAVE, AIR_CLEAVE_COOLDOWN, 6.0)) {
         return false;
      }
      entity.faceToward(target.position());
      entity.triggerUppercutAnimation();
      Vec3 look = horizontalLook(entity);
      AABB box = entity.getBoundingBox().expandTowards(look.scale(4.0)).inflate(1.7, 4.5, 1.7);
      for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && e != entity && !e.isAlliedTo(entity) && !EntityUtils.isImmunePlayerTarget(e))) {
         damageTarget(entity, living, (float)(entity.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.15 + 6.0));
         living.push(look.x * 0.25, 0.55, look.z * 0.25);
         living.hurtMarked = true;
      }
      for (int i = 0; i < fxCount(9); i++) {
         Vec3 pos = entity.position().add(look.scale(1.0 + i * 0.38)).add(0.0, 0.45 + i * 0.42, 0.0);
         level.sendParticles(ParticleTypes.SWEEP_ATTACK, pos.x, pos.y, pos.z, 1, 0.0, 0.0, 0.0, 0.0);
         level.sendParticles(ParticleTypes.END_ROD, pos.x, pos.y, pos.z, fxCount(3), 0.08, 0.08, 0.08, 0.01);
      }
      level.playSound(null, entity.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.HOSTILE, 1.15F, 1.35F);
      return true;
   }

   private static boolean tryBurstDash(ArtoriaPendragonEntity entity, LivingEntity target, ServerLevel level, CompoundTag data, long now) {
      if (!consumeSmallSkill(entity, data, now, TAG_LAST_BURST_DASH, BURST_DASH_COOLDOWN, 10.0)) {
         return false;
      }
      Vec3 from = entity.position();
      Vec3 toTarget = target.position().subtract(from);
      Vec3 dir = new Vec3(toTarget.x, 0.0, toTarget.z).normalize();
      Vec3 end = target.position().subtract(dir.scale(1.6));
      entity.faceToward(target.position());
      entity.triggerChargeAnimation();
      entity.teleportTo(end.x, Math.max(target.getY(), entity.getY()), end.z);
      spawnDashTrail(level, from.add(0.0, 0.55, 0.0), entity.position().add(0.0, 0.55, 0.0), ParticleTypes.END_ROD, fxCount(22));
      damageTarget(entity, target, (float)(entity.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.25 + 7.0));
      target.push(dir.x * 0.45, 0.12, dir.z * 0.45);
      target.hurtMarked = true;
      level.playSound(null, entity.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.2F, 1.45F);
      return true;
   }

   private static boolean tryRoyalCombo(ArtoriaPendragonEntity entity, LivingEntity target, ServerLevel level, CompoundTag data, long now) {
      if (!consumeSmallSkill(entity, data, now, TAG_LAST_ROYAL_COMBO, ROYAL_COMBO_COOLDOWN, 8.0)) {
         return false;
      }
      entity.faceToward(target.position());
      entity.triggerSlashAnimation();
      damageTarget(entity, target, (float)(entity.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.9 + 4.0));
      TYPE_MOON_WORLD.queueServerWork(4, () -> {
         if (entity.isAlive() && target.isAlive() && entity.distanceToSqr(target) <= 36.0) {
            entity.triggerHorizontalSwingAnimation();
            damageTarget(entity, target, (float)(entity.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.95 + 4.0));
         }
      });
      TYPE_MOON_WORLD.queueServerWork(8, () -> {
         if (entity.isAlive() && target.isAlive() && entity.distanceToSqr(target) <= 42.0) {
            entity.triggerSweepAnimation();
            damageTarget(entity, target, (float)(entity.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.1 + 5.0));
         }
      });
      return true;
   }

   private static boolean tryWindThrust(ArtoriaPendragonEntity entity, LivingEntity target, ServerLevel level, CompoundTag data, long now) {
      if (!consumeSmallSkill(entity, data, now, TAG_LAST_WIND_THRUST, WIND_THRUST_COOLDOWN, 7.0)) {
         return false;
      }
      slowFaceToward(entity, target.position(), 8.0F);
      entity.triggerHorizontalSwingAnimation();
      Vec3 origin = entity.position().add(0.0, entity.getBbHeight() * 0.58, 0.0);
      Vec3 end = target.position().add(0.0, target.getBbHeight() * 0.55, 0.0);
      spawnDashTrail(level, origin, end, ParticleTypes.CLOUD, fxCount(20));
      spawnDashTrail(level, origin, end, ParticleTypes.END_ROD, fxCount(10));
      AABB lineBox = new AABB(origin, end).inflate(1.2, 1.2, 1.2);
      Vec3 dir = end.subtract(origin).normalize();
      for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, lineBox, e -> e.isAlive() && e != entity && !e.isAlliedTo(entity) && !EntityUtils.isImmunePlayerTarget(e))) {
         damageTarget(entity, living, (float)(entity.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.9 + 5.0));
         living.push(dir.x * 0.55, 0.08, dir.z * 0.55);
         living.hurtMarked = true;
      }
      level.playSound(null, entity.blockPosition(), SoundEvents.TRIDENT_THROW.value(), SoundSource.HOSTILE, 1.0F, 1.55F);
      return true;
   }

   private static boolean tryManaBurst(ArtoriaPendragonEntity entity, ServerLevel level, CompoundTag data, long now) {
      if (isManaBurstActive(entity) || entity.getCurrentMp() < 30.0 || now - data.getLong(TAG_LAST_MANA_BURST) < MANA_BURST_COOLDOWN) {
         return false;
      }
      data.putLong(TAG_LAST_MANA_BURST, now);
      data.putBoolean(TAG_MANA_BURST_ACTIVE, true);
      entity.setCurrentMp(entity.getCurrentMp() - 30.0);
      entity.triggerSlashAnimation();
      spawnManaBurstActivationFx(entity, level);
      level.playSound(null, entity.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.HOSTILE, 1.2F, 1.35F);
      return true;
   }

   private static boolean tryCharisma(ArtoriaPendragonEntity entity, ServerLevel level, CompoundTag data, long now) {
      if (entity.getCurrentMp() < 25.0 || now - data.getLong(TAG_LAST_CHARISMA) < CHARISMA_COOLDOWN || data.getLong(TAG_CHARISMA_UNTIL) > now) {
         return false;
      }
      List<ServantEntity> allies = level.getEntitiesOfClass(ServantEntity.class, entity.getBoundingBox().inflate(30.0), s -> s.isAlive() && s != entity && s.isAlliedTo(entity));
      if (allies.isEmpty()) {
         return false;
      }
      data.putLong(TAG_LAST_CHARISMA, now);
      data.putLong(TAG_CHARISMA_UNTIL, now + CHARISMA_DURATION);
      entity.setCurrentMp(entity.getCurrentMp() - 25.0);
      applyCharisma(entity, now + CHARISMA_DURATION);
      for (ServantEntity ally : allies) {
         applyCharisma(ally, now + CHARISMA_DURATION);
      }
      level.sendParticles(ParticleTypes.HAPPY_VILLAGER, entity.getX(), entity.getY() + entity.getBbHeight() * 0.8, entity.getZ(), fxCount(24), 0.8, 0.5, 0.8, 0.03);
      level.playSound(null, entity.blockPosition(), SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.HOSTILE, 1.0F, 1.15F);
      return true;
   }

   private static void applyCharisma(LivingEntity entity, long until) {
      updateModifier(entity.getAttribute(Attributes.ATTACK_DAMAGE), CHARISMA_ATTACK_ID, 0.20, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
      entity.getPersistentData().putLong(TAG_CHARISMA_UNTIL, until);
   }

   private static boolean tryInvisibleAir(ArtoriaPendragonEntity entity, LivingEntity target, ServerLevel level, CompoundTag data, long now) {
      if (entity.getCurrentMp() < 20.0 || data.getLong(TAG_WIND_REGATHER_UNTIL) > now) {
         return false;
      }
      data.putLong(TAG_LAST_INVISIBLE_AIR, now);
      data.putBoolean(TAG_INVISIBLE_AIR_ACTIVE, true);
      data.putBoolean(TAG_INVISIBLE_AIR_RELEASED, true);
      data.putLong(TAG_INVISIBLE_AIR_ACTIVE + "Until", now + INVISIBLE_AIR_DURATION);
      data.putLong(TAG_WIND_REGATHER_UNTIL, now + INVISIBLE_AIR_COOLDOWN);
      entity.setCurrentMp(entity.getCurrentMp() - 20.0);
      entity.faceToward(target.position());
      entity.triggerHorizontalSwingAnimation();
      ServantVoiceHelper.tryPlayArtoriaInvisibleAir(entity);
      VFXServerEffects.spawn(level, "artoria_strike_air", entity, ARTORIA_VFX_RADIUS);
      performInvisibleAirCone(entity, level);
      return true;
   }

   private static boolean tryInvisibleAirRelease(ArtoriaPendragonEntity entity, LivingEntity target, ServerLevel level, CompoundTag data, long now) {
      if (entity.getCurrentMp() < 12.0 || data.getLong(TAG_WIND_REGATHER_UNTIL) > now || entity.getRandom().nextFloat() > 0.34F) {
         return false;
      }
      data.putLong(TAG_LAST_INVISIBLE_AIR, now);
      data.putBoolean(TAG_INVISIBLE_AIR_ACTIVE, true);
      data.putBoolean(TAG_INVISIBLE_AIR_RELEASED, true);
      data.putLong(TAG_INVISIBLE_AIR_ACTIVE + "Until", now + INVISIBLE_AIR_RELEASE_COOLDOWN);
      data.putLong(TAG_WIND_REGATHER_UNTIL, now + INVISIBLE_AIR_RELEASE_COOLDOWN);
      entity.setCurrentMp(entity.getCurrentMp() - 12.0);
      entity.faceToward(target.position());
      entity.triggerSweepAnimation();
      performInvisibleAirReleaseBurst(entity, level);
      level.playSound(null, entity.blockPosition(), SoundEvents.TRIDENT_RIPTIDE_1.value(), SoundSource.HOSTILE, 1.1F, 1.45F);
      return true;
   }

   private static void tickInvisibleAirWrapVfx(ArtoriaPendragonEntity entity, ServerLevel level, CompoundTag data, long now) {
      if (!entity.isAlive() || isInvisibleAirActive(entity) || isExcaliburWindingOrReleasing(entity)) {
         return;
      }
      if (data.getLong(TAG_WIND_REGATHER_UNTIL) > now) {
         return;
      }
      if (now - data.getLong(TAG_LAST_INVISIBLE_AIR_WRAP_VFX) < 34L) {
         return;
      }
      data.putLong(TAG_LAST_INVISIBLE_AIR_WRAP_VFX, now);
      VFXServerEffects.spawn(level, "artoria_invisible_air", entity, ARTORIA_VFX_RADIUS);
   }

   private static void performInvisibleAirCone(ArtoriaPendragonEntity entity, ServerLevel level) {
      Vec3 origin = entity.position().add(0.0, entity.getBbHeight() * 0.55, 0.0);
      Vec3 look = horizontalLook(entity);
      Vec3 right = new Vec3(-look.z, 0.0, look.x);
      Set<Integer> hit = new HashSet<>();
      float damage = isManaBurstActive(entity) ? INVISIBLE_AIR_DAMAGE * MANA_BURST_INVISIBLE_AIR_DAMAGE_MULTIPLIER : INVISIBLE_AIR_DAMAGE;
      for (double dist = 1.5; dist <= 10.0; dist += 0.8) {
         double halfWidth = dist * 0.45;
         double halfHeight = 0.7 + dist * 0.32;
         for (double vertical = -0.7; vertical <= halfHeight; vertical += 0.7) {
            double verticalScale = 1.0 - Math.min(0.55, Math.abs(vertical) / Math.max(1.0, halfHeight) * 0.45);
            double widthAtHeight = halfWidth * verticalScale;
            for (double side = -widthAtHeight; side <= widthAtHeight; side += 0.8) {
               Vec3 pos = origin.add(look.scale(dist)).add(right.scale(side)).add(0.0, vertical, 0.0);
               level.sendParticles(ParticleTypes.CLOUD, pos.x, pos.y, pos.z, fxCount(2), 0.05, 0.05, 0.05, 0.012);
               if (((int)(dist * 10 + side * 10 + vertical * 10)) % 3 == 0) {
                  level.sendParticles(ParticleTypes.END_ROD, pos.x, pos.y, pos.z, 1, 0.04, 0.04, 0.04, 0.01);
               }
               if (((int)(dist * 10 + side * 7)) % 5 == 0) {
                  level.sendParticles(ParticleTypes.END_ROD, pos.x, pos.y, pos.z, 1, 0.03, 0.03, 0.03, 0.0);
               }
               AABB box = new AABB(pos, pos).inflate(0.9, 0.75, 0.9);
               for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && e != entity && !e.isAlliedTo(entity) && !EntityUtils.isImmunePlayerTarget(e))) {
                  if (hit.add(living.getId())) {
                     damageTargetBypassingReactions(entity, living, damage);
                     living.push(look.x * 0.8, 0.28, look.z * 0.8);
                     living.hurtMarked = true;
                  }
               }
            }
         }
      }
      spawnInvisibleAirWhiteFx(level, origin, look);
      breakWindConeBlocks(entity, level, origin, look, right);
      level.playSound(null, entity.blockPosition(), SoundEvents.TRIDENT_RIPTIDE_3.value(), SoundSource.HOSTILE, 1.6F, 1.25F);
   }

   private static void performInvisibleAirReleaseBurst(ArtoriaPendragonEntity entity, ServerLevel level) {
      double radius = 6.2;
      Vec3 center = entity.position().add(0.0, entity.getBbHeight() * 0.48, 0.0);
      float baseDamage = isManaBurstActive(entity) ? 34.0F : 22.0F;
      for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, entity.getBoundingBox().inflate(radius, 2.2, radius), e -> e.isAlive() && e != entity && !e.isAlliedTo(entity) && !EntityUtils.isImmunePlayerTarget(e))) {
         Vec3 away = living.position().subtract(entity.position());
         Vec3 horizontal = new Vec3(away.x, 0.0, away.z);
         double distance = horizontal.length();
         if (distance > radius || distance < 0.05) {
            continue;
         }
         Vec3 dir = horizontal.normalize();
         float scaledDamage = (float)(baseDamage * Mth.clamp(1.0 - distance / (radius * 1.35), 0.35, 1.0));
         damageTarget(entity, living, scaledDamage);
         living.push(dir.x * 1.05, 0.26, dir.z * 1.05);
         living.hurtMarked = true;
      }
      for (Projectile projectile : level.getEntitiesOfClass(Projectile.class, entity.getBoundingBox().inflate(radius + 1.5), p -> p.isAlive() && p.getOwner() != entity)) {
         Entity owner = projectile.getOwner();
         if (owner instanceof LivingEntity living && entity.isAlliedTo(living)) {
            continue;
         }
         Vec3 away = projectile.position().subtract(center);
         if (away.lengthSqr() > (radius + 1.5) * (radius + 1.5)) {
            continue;
         }
         Vec3 dir = away.lengthSqr() < 0.01 ? entity.getLookAngle().scale(-1.0) : away.normalize();
         projectile.setDeltaMovement(projectile.getDeltaMovement().add(dir.scale(1.9)).add(0.0, 0.18, 0.0));
         projectile.hurtMarked = true;
      }
      for (double r = 1.2; r <= radius; r += 1.0) {
         int points = Math.max(12, Mth.floor(r * 8.0));
         for (int i = 0; i < points; i++) {
            double angle = (Math.PI * 2.0) * i / points;
            double x = entity.getX() + Math.cos(angle) * r;
            double z = entity.getZ() + Math.sin(angle) * r;
            double y = entity.getY() + 0.35 + (i % 3) * 0.18;
            if (i % 2 == 0) {
               level.sendParticles(ParticleTypes.CLOUD, x, y, z, fxCount(1), 0.02, 0.02, 0.02, 0.01);
            } else {
               level.sendParticles(ParticleTypes.END_ROD, x, y + 0.08, z, fxCount(1), 0.02, 0.02, 0.02, 0.006);
            }
         }
      }
      level.sendParticles(ParticleTypes.FLASH, entity.getX(), entity.getY() + entity.getBbHeight() * 0.56, entity.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
   }

   private static void breakWindConeBlocks(ArtoriaPendragonEntity entity, ServerLevel level, Vec3 origin, Vec3 look, Vec3 right) {
      boolean boosted = isManaBurstActive(entity);
      int broken = 0;
      int limit = boosted ? 150 : 56;
      for (double dist = 1.5; dist <= (boosted ? 11.0 : 7.5) && broken < limit; dist += 0.7) {
         double halfWidth = dist * (boosted ? 0.48 : 0.42);
         for (double side = -halfWidth; side <= halfWidth && broken < limit; side += 0.75) {
            BlockPos center = BlockPos.containing(origin.add(look.scale(dist)).add(right.scale(side)));
            int top = boosted ? 5 : 4;
            int bottom = boosted ? -2 : -1;
            for (BlockPos pos : BlockPos.betweenClosed(center.offset(0, bottom, 0), center.offset(0, top, 0))) {
               BlockState state = level.getBlockState(pos);
               float hardness = state.getDestroySpeed(level, pos);
               if (!state.isAir() && hardness >= 0.0F && hardness < (boosted ? 80.0F : 45.0F) && !state.is(Blocks.BEDROCK)) {
                  if (level.removeBlock(pos, false)) {
                     broken++;
                  }
               }
            }
         }
      }
   }

   private static void spawnInvisibleAirWhiteFx(ServerLevel level, Vec3 origin, Vec3 look) {
      level.sendParticles(ParticleTypes.FLASH, origin.x, origin.y + 0.2, origin.z, fxCount(2), 0.12, 0.08, 0.12, 0.0);
      level.sendParticles(ParticleTypes.GUST, origin.x + look.x * 2.2, origin.y + 0.25, origin.z + look.z * 2.2, fxCount(18), 0.95, 0.45, 0.95, 0.12);
      for (int i = 0; i < fxCount(4); i++) {
         double dist = 2.2 + i * 1.9;
         Vec3 center = origin.add(look.scale(dist)).add(0.0, 0.2 + i * 0.28, 0.0);
         float radius = (float)(dist * 0.55);
         level.addFreshEntity(new ExpandingRingEffectEntity(level, center.x, center.y, center.z, 0.12F, radius, 0.08F, 12 + i * 2, 0xF7F7FF, 0.38F, 0.025F, 62.0F, 0.0F));
         level.addFreshEntity(new ExpandingRingEffectEntity(level, center.x, center.y + 0.08, center.z, 0.08F, radius * 0.72F, 0.06F, 14 + i * 2, 0xFFFFFF, 0.28F, 0.02F, 82.0F, 90.0F));
         level.addFreshEntity(new ExpandingRingEffectEntity(level, center.x, center.y + 0.22, center.z, 0.08F, radius * 0.52F, 0.05F, 16 + i * 2, 0xBFFFFF, 0.22F, 0.03F, 35.0F, i * 33.0F));
         level.sendParticles(ParticleTypes.CLOUD, center.x, center.y, center.z, fxCount(18), radius * 0.18, 0.22, radius * 0.18, 0.055);
         level.sendParticles(ParticleTypes.END_ROD, center.x, center.y + 0.1, center.z, fxCount(8), radius * 0.1, 0.12, radius * 0.1, 0.015);
      }
   }

   private static void spawnExcaliburChargeFx(ArtoriaPendragonEntity entity, ServerLevel level) {
      Vec3 front = entity.position().add(horizontalLook(entity).scale(1.6)).add(0.0, entity.getBbHeight() * 0.72, 0.0);
      Vec3 look = horizontalLook(entity);
      Vec3 right = new Vec3(-look.z, 0.0, look.x);
      level.sendParticles(ParticleTypes.END_ROD, entity.getX(), entity.getY() + 0.15, entity.getZ(), fxCount(26), 1.45, 0.1, 1.45, 0.07);
      level.sendParticles(ParticleTypes.END_ROD, front.x, front.y, front.z, fxCount(18), 0.34, 0.38, 0.34, 0.045);
      level.sendParticles(ParticleTypes.ENCHANT, entity.getX(), entity.getY() + entity.getBbHeight() * 0.75, entity.getZ(), fxCount(22), 0.8, 0.65, 0.8, 0.07);
      for (int i = 0; i < fxCount(10); i++) {
         double angle = entity.tickCount * 0.45 + i * Math.PI * 0.4;
         double radius = 0.6 + i * 0.08;
         Vec3 pos = entity.position()
            .add(right.scale(Math.cos(angle) * radius))
            .add(look.scale(Math.sin(angle) * radius * 0.55))
            .add(0.0, 0.25 + (i % 5) * 0.34, 0.0);
         level.sendParticles(ParticleTypes.END_ROD, pos.x, pos.y, pos.z, 1, 0.02, 0.035, 0.02, 0.0);
         level.sendParticles(ParticleTypes.END_ROD, pos.x, pos.y + 0.04, pos.z, 1, 0.01, 0.03, 0.01, 0.0);
      }
      for (int i = 0; i < fxCount(5); i++) {
         Vec3 blade = front.add(look.scale(i * 0.55)).add(0.0, i * 0.02, 0.0);
         level.sendParticles(ParticleTypes.END_ROD, blade.x, blade.y, blade.z, fxCount(2), 0.06, 0.08, 0.06, 0.015);
      }
      if (entity.tickCount % 8 == 0) {
         level.sendParticles(ParticleTypes.FLASH, front.x, front.y, front.z, 1, 0.0, 0.0, 0.0, 0.0);
         level.addFreshEntity(new ExpandingRingEffectEntity(level, entity.getX(), entity.getY() + 0.25, entity.getZ(), 0.22F, 3.4F, 0.1F, 18, 0xF7F7FF, 0.5F, 0.045F));
         level.addFreshEntity(new ExpandingRingEffectEntity(level, front.x, front.y, front.z, 0.12F, 1.8F, 0.07F, 14, 0xFFFFE8, 0.44F, 0.02F, 80.0F, (entity.tickCount * 9) % 360));
      }
   }

   private static void spawnExcaliburReleaseFx(ArtoriaPendragonEntity entity, ServerLevel level, Vec3 start) {
      Vec3 look = excaliburLook(entity);
      level.sendParticles(ParticleTypes.FLASH, start.x, start.y, start.z, fxCount(4), 0.05, 0.05, 0.05, 0.0);
      level.sendParticles(ParticleTypes.END_ROD, start.x, start.y, start.z, fxCount(90), 0.9, 0.75, 0.9, 0.16);
      level.sendParticles(ParticleTypes.END_ROD, start.x, start.y, start.z, fxCount(80), 0.75, 0.55, 0.75, 0.12);
      level.addFreshEntity(new ExpandingRingEffectEntity(level, start.x, start.y, start.z, 0.35F, 6.0F, 0.2F, 22, 0xFFFFE8, 0.72F, 0.04F));
      level.addFreshEntity(new ExpandingRingEffectEntity(level, start.x, start.y, start.z, 0.25F, 4.2F, 0.12F, 20, 0xFFFFFF, 0.58F, 0.03F, 90.0F, 0.0F));
      for (int i = 1; i <= fxCount(8); i++) {
         Vec3 pos = start.add(look.scale(i * 3.8));
         level.sendParticles(ParticleTypes.END_ROD, pos.x, pos.y, pos.z, fxCount(14), 1.2, 0.45, 1.2, 0.04);
      }
   }

   private static void spawnManaBurstActivationFx(ArtoriaPendragonEntity entity, ServerLevel level) {
      double cx = entity.getX();
      double cy = entity.getY();
      double cz = entity.getZ();
      level.sendParticles(ParticleTypes.FLASH, cx, cy + entity.getBbHeight() * 0.72, cz, fxCount(3), 0.0, 0.0, 0.0, 0.0);
      level.sendParticles(ParticleTypes.END_ROD, cx, cy + entity.getBbHeight() * 0.58, cz, fxCount(56), 0.32, 0.58, 0.32, 0.075);
      level.sendParticles(ParticleTypes.END_ROD, cx, cy + 0.18, cz, fxCount(38), 0.46, 0.08, 0.46, 0.075);
      level.sendParticles(ParticleTypes.END_ROD, cx, cy + entity.getBbHeight() * 0.62, cz, fxCount(22), 0.26, 0.42, 0.26, 0.04);
      level.addFreshEntity(new ExpandingRingEffectEntity(level, cx, cy + 0.12, cz, 0.22F, 2.1F, 0.08F, 18, 0xFFFFE8, 0.48F, 0.035F));
      level.addFreshEntity(new ExpandingRingEffectEntity(level, cx, cy + entity.getBbHeight() * 0.5, cz, 0.12F, 1.2F, 0.06F, 16, 0xFFFFFF, 0.34F, 0.025F, 78.0F, entity.tickCount * 5.0F));
   }

   private static void spawnManaBurstSustainFx(ArtoriaPendragonEntity entity, ServerLevel level) {
      if (entity.tickCount % 2 != 0) {
         return;
      }
      double time = entity.tickCount * 0.5;
      double cx = entity.getX();
      double cy = entity.getY();
      double cz = entity.getZ();
      level.sendParticles(ParticleTypes.END_ROD, cx, cy + entity.getBbHeight() * 0.62, cz, fxCount(4), 0.2, 0.34, 0.2, 0.032);
      level.sendParticles(ParticleTypes.END_ROD, cx, cy + 0.16, cz, fxCount(5), 0.28, 0.04, 0.28, 0.045);
      for (int i = 0; i < fxCount(5); i++) {
         double angle = time + i * 1.256;
         double radius = 0.28 + 0.05 * i;
         double y = cy + 0.25 + (i % 4) * 0.42;
         level.sendParticles(
            ParticleTypes.END_ROD,
            cx + Math.cos(angle) * radius,
            y,
            cz + Math.sin(angle) * radius,
            1,
            0.015,
            0.035,
            0.015,
            0.0
         );
      }
      if (entity.tickCount % 12 == 0) {
         level.addFreshEntity(new ExpandingRingEffectEntity(level, cx, cy + 0.18, cz, 0.12F, 1.05F, 0.04F, 12, 0xFFFFE8, 0.24F, 0.02F));
      }
   }

   private static void spawnManaBurstJetFx(ArtoriaPendragonEntity entity, ServerLevel level, Vec3 desired) {
      Vec3 exhaust = desired.lengthSqr() > 1.0E-4 ? desired.normalize().scale(-1.0) : horizontalLook(entity).scale(-1.0);
      Vec3 origin = entity.position().add(0.0, 0.16, 0.0);
      int steps = entity.onGround() ? 2 : 3;
      for (int i = 0; i < steps; i++) {
         double distance = 0.18 + i * 0.24;
         Vec3 pos = origin.add(exhaust.scale(distance));
         double spread = Math.max(0.035, 0.13 - i * 0.02);
         level.sendParticles(ParticleTypes.END_ROD, pos.x, pos.y, pos.z, fxCount(4), spread, spread * 0.65, spread, 0.045);
         if (i == steps - 1) {
            level.sendParticles(ParticleTypes.CLOUD, pos.x, pos.y, pos.z, fxCount(2), spread * 0.9, spread * 0.55, spread * 0.9, 0.03);
         }
         if (!entity.onGround() && i == 0) {
            level.sendParticles(ParticleTypes.END_ROD, pos.x, pos.y + 0.04, pos.z, fxCount(2), spread * 0.5, spread * 0.5, spread * 0.5, 0.025);
         }
      }
   }

   private static boolean consumeSmallSkill(ArtoriaPendragonEntity entity, CompoundTag data, long now, String tag, int cooldown, double mpCost) {
      if (entity.getCurrentMp() < mpCost || now - data.getLong(tag) < cooldown) {
         return false;
      }
      data.putLong(tag, now);
      entity.setCurrentMp(Math.max(0.0, entity.getCurrentMp() - mpCost));
      return true;
   }

   private static void damageTarget(ArtoriaPendragonEntity entity, LivingEntity target, float damage) {
      target.invulnerableTime = 0;
      target.hurt(entity.damageSources().mobAttack(entity), damage);
      target.invulnerableTime = 0;
   }

   private static void damageTargetBypassingReactions(ArtoriaPendragonEntity entity, LivingEntity target, float damage) {
      target.getPersistentData().putLong(TAG_INVISIBLE_AIR_DAMAGE_BYPASS_UNTIL, target.level().getGameTime() + 2L);
      target.invulnerableTime = 0;
      target.hurt(entity.damageSources().magic(), damage);
      target.invulnerableTime = 0;
   }

   private static void spawnDashTrail(ServerLevel level, Vec3 start, Vec3 end, net.minecraft.core.particles.ParticleOptions particle, int count) {
      for (int i = 0; i < count; i++) {
         double t = count <= 1 ? 1.0 : (double)i / (double)(count - 1);
         Vec3 pos = start.lerp(end, t);
         level.sendParticles(particle, pos.x, pos.y, pos.z, 1, 0.05, 0.05, 0.05, 0.0);
      }
   }

   private static boolean isMedeaSmallMagic(DamageSource source, float amount) {
      Entity attacker = source.getEntity();
      Entity direct = source.getDirectEntity();
      if (direct instanceof MedeaMagicBoltEntity bolt) {
         return switch (bolt.getMode()) {
            case BOLT, FIRE_BOLT, FROST_BOLT -> true;
            default -> false;
         };
      }
      if (direct instanceof MedeaBeamEffectEntity beam) {
         return beam.getDamage() <= 35.0F;
      }
      if (attacker instanceof MedeaEntity && (source.is(DamageTypes.MAGIC) || source.is(DamageTypes.INDIRECT_MAGIC))) {
         return amount <= 35.0F;
      }
      return false;
   }

   private static Vec3 horizontalLook(LivingEntity entity) {
      Vec3 look = entity.getLookAngle();
      Vec3 horizontal = new Vec3(look.x, 0.0, look.z);
      return horizontal.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : horizontal.normalize();
   }

   public static Vec3 excaliburLook(LivingEntity entity) {
      Vec3 look = entity.getLookAngle();
      if (look.lengthSqr() < 1.0E-4) {
         return new Vec3(0.0, 0.0, 1.0);
      }
      if (entity instanceof ServerPlayer) {
         return look.normalize();
      }
      double pitch = -Math.toDegrees(Math.asin(Mth.clamp(look.y, -1.0, 1.0)));
      pitch = Mth.clamp((float)pitch, -35.0F, 35.0F);
      double yaw = Math.toDegrees(Math.atan2(-look.x, look.z));
      return Vec3.directionFromRotation((float)pitch, (float)yaw).normalize();
   }

   private static LivingEntity getExcaliburTarget(ArtoriaPendragonEntity entity, LivingEntity fallback, ServerLevel level, CompoundTag data) {
      if (data.hasUUID(TAG_EXCALIBUR_TARGET)) {
         LivingEntity locked = findLivingByUuid(level, data.getUUID(TAG_EXCALIBUR_TARGET));
         if (locked != null && locked.isAlive() && !EntityUtils.isImmunePlayerTarget(locked)) {
            entity.setTarget(locked);
            return locked;
         }
      }
      return fallback != null && fallback.isAlive() && !EntityUtils.isImmunePlayerTarget(fallback) ? fallback : null;
   }

   private static LivingEntity findLivingByUuid(ServerLevel level, UUID id) {
      if (id == null) {
         return null;
      }
      Entity entity = level.getEntity(id);
      return entity instanceof LivingEntity living ? living : null;
   }

   private static void faceExcaliburTarget(ArtoriaPendragonEntity entity, LivingEntity target) {
      Vec3 aim = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
      entity.faceToward(aim);
      entity.getLookControl().setLookAt(target, 90.0F, 90.0F);
   }

   private static int fxCount(int count) {
      return Math.max(1, Math.round(count * 0.67F));
   }

   private static void slowFaceToward(ArtoriaPendragonEntity entity, Vec3 target, float maxTurnDegrees) {
      Vec3 offset = target.subtract(entity.position());
      Vec3 horizontal = new Vec3(offset.x, 0.0, offset.z);
      if (horizontal.lengthSqr() < 1.0E-4) {
         return;
      }
      float desiredYaw = (float)(Mth.atan2(horizontal.z, horizontal.x) * 180.0F / Math.PI) - 90.0F;
      float currentYaw = entity.getYRot();
      float nextYaw = Mth.approachDegrees(currentYaw, desiredYaw, maxTurnDegrees);
      entity.setYRot(nextYaw);
      entity.yRotO = nextYaw;
      entity.setYHeadRot(nextYaw);
      entity.yHeadRotO = nextYaw;
      entity.yBodyRot = nextYaw;
      entity.yBodyRotO = nextYaw;
   }

   private static void spawnInstinctFx(ArtoriaPendragonEntity entity, boolean avalon) {
      if (entity.level() instanceof ServerLevel level) {
         level.sendParticles(avalon ? ParticleTypes.TOTEM_OF_UNDYING : ParticleTypes.CRIT, entity.getX(), entity.getY() + entity.getBbHeight() * 0.65, entity.getZ(), avalon ? 22 : 12, 0.35, 0.35, 0.35, 0.03);
         level.playSound(null, entity.blockPosition(), avalon ? SoundEvents.SHIELD_BLOCK : SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 0.9F, avalon ? 1.45F : 1.8F);
      }
   }

   private static void updateModifier(AttributeInstance attribute, ResourceLocation id, double amount, AttributeModifier.Operation operation) {
      if (attribute == null) {
         return;
      }
      if (Math.abs(amount) < 1.0E-6) {
         removeModifier(attribute, id);
         return;
      }
      AttributeModifier existing = attribute.getModifier(id);
      if (existing != null && existing.operation() == operation && Math.abs(existing.amount() - amount) < 1.0E-6) {
         return;
      }
      if (existing != null) {
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
