package net.xxxjk.TYPE_MOON_WORLD.item.custom;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
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
import net.xxxjk.TYPE_MOON_WORLD.entity.ExpandingRingEffectEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.GaeBulgArmyProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.GaeBulgProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.RubyProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ArtoriaPendragonCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.CuChulainnCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.EmiyaArcherEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.MedeaCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.utils.ManaHelper;

public final class PlayerNoblePhantasmHelper {
   public static final String ONE_SHOT_TSUBAME_TAG = "TypeMoonOneShotTsubame";
   private static final String GAE_DEATH_FLIGHT_TAG = "TypeMoonGaeBulgDeathFlight";
   private static final String GAE_DEATH_FLIGHT_PAID_TAG = "TypeMoonGaeBulgDeathFlightPaid";
   private static final String EXCALIBUR_CHARGE_TAG = "TypeMoonExcaliburCharge";
   private static final int GAE_DEATH_FLIGHT_CHARGE_TICKS = 30;
   private static final int EXCALIBUR_MAX_CHARGE_TICKS = 100;
   private static final int GAE_BULG_PLAYER_COOLDOWN = 100;
   private static final double CHARGE_MANA_PER_TICK = 20.0;

   private PlayerNoblePhantasmHelper() {
   }

   public static boolean consumeStrict(ServerPlayer player, double amount) {
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
         player.getItemInHand(hand).shrink(1);
      }
      return true;
   }

   public static boolean useGaeBulgMelee(ServerPlayer player) {
      if (!consumeStrict(player, 20.0)) {
         return false;
      }
      LivingEntity target = findLookTarget(player, 4.0, 1.15);
      if (target == null) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.no_target"), true);
         return true;
      }
      resolveGaeBulgHit(player, target);
      addGaeBulgCooldown(player);
      return true;
   }

   public static void startGaeBulgDeathFlight(ServerPlayer player) {
      player.getPersistentData().putBoolean(GAE_DEATH_FLIGHT_TAG, true);
      player.getPersistentData().putInt(GAE_DEATH_FLIGHT_TAG + "Ticks", 0);
      player.getPersistentData().remove(GAE_DEATH_FLIGHT_PAID_TAG);
   }

   public static void tickGaeBulgUse(Level level, LivingEntity living, int useTicks) {
      if (!(living instanceof ServerPlayer player) || !player.getPersistentData().getBoolean(GAE_DEATH_FLIGHT_TAG)) {
         return;
      }
      int charged = Math.min(GAE_DEATH_FLIGHT_CHARGE_TICKS, useTicks);
      player.getPersistentData().putInt(GAE_DEATH_FLIGHT_TAG + "Ticks", charged);
      if (useTicks >= GAE_DEATH_FLIGHT_CHARGE_TICKS && !player.getPersistentData().getBoolean(GAE_DEATH_FLIGHT_PAID_TAG)) {
         if (!consumeStrict(player, CHARGE_MANA_PER_TICK * GAE_DEATH_FLIGHT_CHARGE_TICKS)) {
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
      player.getPersistentData().remove(GAE_DEATH_FLIGHT_TAG);
      player.getPersistentData().remove(GAE_DEATH_FLIGHT_TAG + "Ticks");
      player.getPersistentData().remove(GAE_DEATH_FLIGHT_PAID_TAG);
      if (deathFlight && charged >= GAE_DEATH_FLIGHT_CHARGE_TICKS && deathFlightPaid) {
         throwGaeBulgArmy(player);
         addGaeBulgCooldown(player);
         return true;
      }
      if (crouchingRelease || deathFlight) {
         if (consumeStrict(player, 20.0)) {
            throwGaeBulgSingle(player);
            addGaeBulgCooldown(player);
         }
         return true;
      }
      return false;
   }

   public static void startExcaliburCharge(ServerPlayer player) {
      player.getPersistentData().putInt(EXCALIBUR_CHARGE_TAG, 0);
   }

   public static void tickExcaliburCharge(Level level, LivingEntity living, int useTicks) {
      if (!(living instanceof ServerPlayer player)) {
         return;
      }
      int charged = Math.min(EXCALIBUR_MAX_CHARGE_TICKS, useTicks);
      player.getPersistentData().putInt(EXCALIBUR_CHARGE_TAG, charged);
      if (useTicks > 0 && useTicks <= EXCALIBUR_MAX_CHARGE_TICKS) {
         if (!consumeStrict(player, CHARGE_MANA_PER_TICK)) {
            player.releaseUsingItem();
         } else if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.0, player.getZ(), 6, 0.55, 0.55, 0.55, 0.04);
         }
      }
   }

   public static void releaseExcalibur(ServerPlayer player) {
      int charged = player.getPersistentData().getInt(EXCALIBUR_CHARGE_TAG);
      player.getPersistentData().remove(EXCALIBUR_CHARGE_TAG);
      if (charged <= 0 || !(player.level() instanceof ServerLevel level)) {
         return;
      }
      int duration = Math.max(30, Math.min(150, charged + 50));
      Vec3 start = player.position().add(0.0, player.getBbHeight() * 0.66, 0.0).add(player.getLookAngle().normalize().scale(1.2));
      ArtoriaExcaliburBeamEntity beam = new ArtoriaExcaliburBeamEntity(level, player, start, duration);
      level.addFreshEntity(beam);
      spawnExcaliburReleaseFx(player, level, start);
      level.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 2.5F, 0.85F);
      level.playSound(null, player.blockPosition(), SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 1.1F, 1.65F);
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
         dashSlashOveredge(player);
      } else {
         throwKanshouBakuya(player, hand);
      }
      player.getCooldowns().addCooldown(stack.getItem(), 30);
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

   private static void addGaeBulgCooldown(ServerPlayer player) {
      if (player.getMainHandItem().is(ModItems.GAE_BULG.get())) {
         player.getCooldowns().addCooldown(player.getMainHandItem().getItem(), GAE_BULG_PLAYER_COOLDOWN);
      }
      if (player.getOffhandItem().is(ModItems.GAE_BULG.get())) {
         player.getCooldowns().addCooldown(player.getOffhandItem().getItem(), GAE_BULG_PLAYER_COOLDOWN);
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
      level.playSound(null, player.blockPosition(), SoundEvents.TRIDENT_THROW.value(), SoundSource.PLAYERS, 0.8F, 1.55F);
      level.sendParticles(ParticleTypes.CRIT, spawn.x, spawn.y, spawn.z, 10, 0.12, 0.12, 0.12, 0.06);
   }

   private static void dashSlashOveredge(ServerPlayer player) {
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
            target.hurt(player.damageSources().mobAttack(player), 72.0F);
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
      level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.8F, 0.5F);
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
         level.levelEvent(2001, pos, Block.getId(state));
         if (level.destroyBlock(pos, false, owner)) {
            broken++;
         }
      }
   }

   private static void spawnExcaliburReleaseFx(Player player, ServerLevel level, Vec3 start) {
      Vec3 look = player.getLookAngle().normalize();
      level.sendParticles(ParticleTypes.FLASH, start.x, start.y, start.z, 4, 0.1, 0.1, 0.1, 0.0);
      level.sendParticles(ParticleTypes.END_ROD, start.x, start.y, start.z, 80, 0.8, 0.55, 0.8, 0.12);
      for (int i = 0; i < 4; i++) {
         Vec3 center = start.add(look.scale(1.5 + i * 2.0));
         level.addFreshEntity(new ExpandingRingEffectEntity(level, center.x, center.y, center.z, 0.18F, 1.2F + i * 0.7F, 0.08F, 16 + i * 2, 0xF7F7FF, 0.54F, 0.025F, 70.0F, 0.0F));
      }
   }

   public static Vec3 horizontalLook(LivingEntity entity) {
      Vec3 look = entity.getLookAngle();
      Vec3 horizontal = new Vec3(look.x, 0.0, look.z);
      return horizontal.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : horizontal.normalize();
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
