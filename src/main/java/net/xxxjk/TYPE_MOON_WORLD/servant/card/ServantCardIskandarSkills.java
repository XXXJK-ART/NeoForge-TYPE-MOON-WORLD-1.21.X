package net.xxxjk.TYPE_MOON_WORLD.servant.card;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.entity.BucephalusEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.GordiusWheelEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.IskandarMountEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.MacedonianSoldierEntity;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.init.ModSounds;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.PlayerNoblePhantasmHelper;
import net.xxxjk.TYPE_MOON_WORLD.magic.unlimited_blade_works.UBWInstanceManager;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.iskandar.IonioiHetairoiRankPool;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantTraitTag;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.world.dimension.ModDimensions;
import org.jetbrains.annotations.Nullable;

public final class ServantCardIskandarSkills {
   public static final String SERVANT_ID = "iskandar";
   private static final String TAG_BUCEPHALUS_UUID = "ServantCardIskandarBucephalus";
   private static final String TAG_BUCEPHALUS_HP = "ServantCardIskandarBucephalusHp";
   private static final String TAG_BUCEPHALUS_DEAD = "ServantCardIskandarBucephalusDead";
   private static final String TAG_GORDIUS_UUID = "ServantCardIskandarGordiusWheel";
   private static final String TAG_GORDIUS_HP = "ServantCardIskandarGordiusWheelHp";
   private static final String TAG_GORDIUS_DEAD = "ServantCardIskandarGordiusWheelDead";
   private static final String TAG_IONIOI_MOUNT_TYPE = "ServantCardIskandarIonioiMountType";
   private static final String TAG_IONIOI_MOUNT_FLYING = "ServantCardIskandarIonioiMountFlying";
   private static final String TAG_IONIOI_ACTIVE = "ServantCardIskandarIonioiActive";
   private static final String TAG_IONIOI_START = "ServantCardIskandarIonioiStart";
   private static final String TAG_IONIOI_RETURN_DIM = "ServantCardIskandarIonioiReturnDim";
   private static final String TAG_IONIOI_RETURN_X = "ServantCardIskandarIonioiReturnX";
   private static final String TAG_IONIOI_RETURN_Y = "ServantCardIskandarIonioiReturnY";
   private static final String TAG_IONIOI_RETURN_Z = "ServantCardIskandarIonioiReturnZ";
   private static final String TAG_IONIOI_LAST_DRAIN = "ServantCardIskandarIonioiLastDrain";
   private static final String TAG_IONIOI_DEATHS = "ServantCardIskandarIonioiDeaths";
   private static final String TAG_IONIOI_SEED = "ServantCardIskandarIonioiSeed";
   private static final String TAG_IONIOI_SESSION = "ServantCardIskandarIonioiSession";
   private static final String TAG_IONIOI_TARGET_OWNER = "IonioiHetairoiTargetOwner";
   private static final String TAG_IONIOI_TARGET_SESSION = "IonioiHetairoiTargetSession";
   private static final String TAG_IONIOI_TARGET_RETURN_DIM = "IonioiHetairoiReturnDim";
   private static final String TAG_IONIOI_TARGET_RETURN_X = "IonioiHetairoiReturnX";
   private static final String TAG_IONIOI_TARGET_RETURN_Y = "IonioiHetairoiReturnY";
   private static final String TAG_IONIOI_TARGET_RETURN_Z = "IonioiHetairoiReturnZ";
   private static final String TAG_IONIOI_TARGET_PRIMARY = "IonioiHetairoiPrimaryTarget";
   private static final String TAG_ORDER_TARGET = "ServantCardIskandarOrderTarget";
   private static final String TAG_ORDER_UNTIL = "ServantCardIskandarOrderUntil";
   private static final ResourceLocation LEADERSHIP_SPEED_ID = id("servant_card_iskandar_leadership_speed");
   private static final ResourceLocation RIDING_SPEED_ID = id("servant_card_iskandar_riding_speed");
   private static final double IONIOI_UPKEEP_MP_PER_SECOND = 5.0;
   private static final int IONIOI_FREE_UPKEEP_TICKS = 30 * 20;
   private static final int CARD_IONIOI_ACTIVE_CAP = 400;
   private static final double CARD_IONIOI_PULL_RADIUS = 64.0;
   private static final double CARD_IONIOI_ARMY_ENTRY_DISTANCE = 20.0;

   private ServantCardIskandarSkills() {
   }

   public static void initialize(ServerPlayer player) {
      clear(player);
      ItemStack sword = player.getMainHandItem();
      if (sword.is(ModItems.ISKANDAR_SHORTSWORD.get())) {
         ServantCardTransformManager.markGeneratedItem(sword, true, false);
      }
   }

   public static void tick(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      if (!player.isAlive() || !vars.servant_card_transformed || !SERVANT_ID.equals(vars.servant_card_id)) {
         clear(player);
         return;
      }
      tickMountState(player);
      tickPassives(player);
      tickIonioi(player, vars);
      tickConquerorOrder(player);
   }

   public static void clear(ServerPlayer player) {
      clearMount(player, TAG_BUCEPHALUS_UUID, false);
      clearMount(player, TAG_GORDIUS_UUID, false);
      endIonioi(player, false);
      CompoundTag data = player.getPersistentData();
      data.remove(TAG_BUCEPHALUS_UUID);
      data.remove(TAG_BUCEPHALUS_HP);
      data.remove(TAG_BUCEPHALUS_DEAD);
      data.remove(TAG_GORDIUS_UUID);
      data.remove(TAG_GORDIUS_HP);
      data.remove(TAG_GORDIUS_DEAD);
      data.remove(TAG_IONIOI_MOUNT_TYPE);
      data.remove(TAG_IONIOI_MOUNT_FLYING);
      data.remove(TAG_IONIOI_DEATHS);
      data.remove(TAG_IONIOI_SEED);
      data.remove(TAG_ORDER_TARGET);
      data.remove(TAG_ORDER_UNTIL);
      ServantCardSkillUtils.remove(player.getAttribute(Attributes.MOVEMENT_SPEED), LEADERSHIP_SPEED_ID);
      ServantCardSkillUtils.remove(player.getAttribute(Attributes.MOVEMENT_SPEED), RIDING_SPEED_ID);
   }

   public static boolean performBucephalus(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level)) {
         return false;
      }
      CompoundTag data = player.getPersistentData();
      if (data.getBoolean(TAG_BUCEPHALUS_DEAD)) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.iskandar_bucephalus_dead"), true);
         return false;
      }
      BucephalusEntity existing = getBucephalus(player);
      if (existing != null) {
         player.startRiding(existing, true);
         return true;
      }
      BucephalusEntity horse = ModEntities.BUCEPHALUS.get().create(level);
      if (horse == null) {
         return false;
      }
      Vec3 pos = player.position().add(PlayerNoblePhantasmHelper.horizontalLook(player).scale(1.2));
      horse.moveTo(pos.x, pos.y, pos.z, player.getYRot(), 0.0F);
      horse.bindCardOwner(player);
      double savedHp = data.contains(TAG_BUCEPHALUS_HP) ? data.getDouble(TAG_BUCEPHALUS_HP) : horse.getMaxHealth();
      horse.setHealth(Mth.clamp((float)savedHp, 1.0F, horse.getMaxHealth()));
      if (!level.addFreshEntity(horse)) {
         return false;
      }
      data.putUUID(TAG_BUCEPHALUS_UUID, horse.getUUID());
      player.startRiding(horse, true);
      level.playSound(null, horse.blockPosition(), SoundEvents.HORSE_ARMOR, SoundSource.PLAYERS, 0.9F, 0.82F);
      level.sendParticles(ParticleTypes.CLOUD, horse.getX(), horse.getY() + 0.3, horse.getZ(), 24, 0.8, 0.18, 0.8, 0.05);
      return true;
   }

   public static boolean performRoyalSwordAssault(ServerPlayer player) {
      Vec3 dir = PlayerNoblePhantasmHelper.horizontalLook(player);
      player.swing(InteractionHand.MAIN_HAND, true);
      player.setDeltaMovement(player.getDeltaMovement().add(dir.x * 0.75, 0.05, dir.z * 0.75));
      player.hurtMarked = true;
      ServantCardSkillUtils.hitForwardArc(player, dir, 4.2, (float)(player.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.1 + 7.0));
      slashFx(player, dir, 20, 0.8F);
      return true;
   }

   public static boolean performConquerorOrder(ServerPlayer player) {
      LivingEntity target = findPreferredTarget(player, 28.0, 2.2);
      if (target == null) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.no_target"), true);
         return false;
      }
      CompoundTag data = player.getPersistentData();
      data.putUUID(TAG_ORDER_TARGET, target.getUUID());
      data.putLong(TAG_ORDER_UNTIL, player.level().getGameTime() + 12L * 20L);
      player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 80, 0, false, true, true));
      if (player.level() instanceof ServerLevel level) {
         level.sendParticles(ParticleTypes.END_ROD, target.getX(), target.getY() + target.getBbHeight() * 0.7, target.getZ(), 28, 0.45, 0.55, 0.45, 0.05);
         level.playSound(null, player.blockPosition(), SoundEvents.RAID_HORN.value(), SoundSource.PLAYERS, 0.9F, 0.78F);
      }
      return true;
   }

   public static boolean performThunderCall(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level)) {
         return false;
      }
      LivingEntity target = findPreferredTarget(player, 36.0, 2.5);
      Vec3 center = target == null ? player.getEyePosition().add(player.getLookAngle().normalize().scale(18.0)) : target.position();
      boolean chariot = player.getVehicle() instanceof GordiusWheelEntity;
      double radius = chariot ? 4.4 : 2.8;
      float damage = chariot ? 76.0F : 48.0F;
      spawnVisualLightning(level, center);
      level.sendParticles(ParticleTypes.ELECTRIC_SPARK, center.x, center.y + 0.5, center.z, chariot ? 80 : 46, radius * 0.32, 0.75, radius * 0.32, 0.22);
      level.playSound(null, BlockPos.containing(center), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.05F, 0.86F);
      for (LivingEntity enemy : level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(radius, 2.5, radius),
         entity -> isEnemy(player, entity))) {
         enemy.invulnerableTime = 0;
         enemy.hurt(player.damageSources().magic(), damage);
         enemy.invulnerableTime = 0;
      }
      return true;
   }

   public static boolean performBattlefieldStride(ServerPlayer player) {
      LivingEntity target = findPreferredTarget(player, 10.0, 1.8);
      Vec3 dir = PlayerNoblePhantasmHelper.horizontalLook(player);
      if (target != null) {
         Vec3 side = new Vec3(-dir.z, 0.0, dir.x).scale(player.getRandom().nextBoolean() ? 2.0 : -2.0);
         Vec3 toTarget = player.position().subtract(target.position()).multiply(1.0, 0.0, 1.0);
         if (toTarget.lengthSqr() < 1.0E-4) {
            toTarget = dir.scale(-1.0);
         }
         ServantCardSkillUtils.trySafeHorizontalTeleport(player, target.position().add(toTarget.normalize().scale(1.8)).add(side));
         dir = target.position().subtract(player.position()).multiply(1.0, 0.0, 1.0);
         if (dir.lengthSqr() > 1.0E-4) {
            dir = dir.normalize();
         }
      } else {
         ServantCardSkillUtils.trySafeHorizontalTeleport(player, player.position().add(dir.scale(5.0)));
      }
      ServantCardSkillUtils.hitForwardArc(player, dir, 3.8, 16.0F);
      slashFx(player, dir, 12, 1.2F);
      return true;
   }

   public static boolean performVanguardSummon(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level)) {
         return false;
      }
      LivingEntity target = findPreferredTarget(player, 28.0, 2.0);
      Vec3 dir = PlayerNoblePhantasmHelper.horizontalLook(player);
      player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 6 * 20, 0, false, true, true));
      player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 4 * 20, 0, false, true, true));
      player.setDeltaMovement(player.getDeltaMovement().add(dir.x * 1.05, 0.06, dir.z * 1.05));
      player.hurtMarked = true;
      player.fallDistance = 0.0F;
      if (target != null && target.isAlive()) {
         Vec3 toward = target.position().subtract(player.position()).multiply(1.0, 0.0, 1.0);
         if (toward.lengthSqr() > 1.0E-4) {
            dir = toward.normalize();
         }
      }
      ServantCardSkillUtils.hitForwardArc(player, dir, 5.4, 24.0F);
      for (LivingEntity enemy : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(5.8, 1.8, 5.8),
         entity -> isEnemy(player, entity))) {
         Vec3 push = enemy.position().subtract(player.position()).multiply(1.0, 0.0, 1.0);
         if (push.lengthSqr() < 1.0E-4) {
            push = dir;
         }
         push = push.normalize();
         enemy.push(push.x * 1.15, 0.25, push.z * 1.15);
         enemy.hurtMarked = true;
      }
      level.sendParticles(ParticleTypes.CRIT, player.getX(), player.getY() + 0.9, player.getZ(), 42, 1.8, 0.55, 1.8, 0.10);
      level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.2, player.getZ(), 30, 2.2, 0.16, 2.2, 0.08);
      level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 0.72F);
      level.playSound(null, player.blockPosition(), SoundEvents.RAVAGER_STEP, SoundSource.PLAYERS, 0.9F, 0.76F);
      return true;
   }

   public static boolean performKinglyWarCry(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level)) {
         return false;
      }
      player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 8 * 20, 0, false, true, true));
      for (LivingEntity enemy : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(6.0, 2.2, 6.0),
         entity -> isEnemy(player, entity))) {
         enemy.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 55, 1, false, true, true));
         Vec3 push = enemy.position().subtract(player.position()).multiply(1.0, 0.0, 1.0);
         if (push.lengthSqr() < 1.0E-4) {
            push = PlayerNoblePhantasmHelper.horizontalLook(player);
         }
         push = push.normalize();
         enemy.push(push.x * 1.25, 0.25, push.z * 1.25);
         enemy.hurtMarked = true;
      }
      level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 1.1, player.getZ(), 46, 2.4, 0.7, 2.4, 0.11);
      level.playSound(null, player.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 1.0F, 0.72F);
      return true;
   }

   public static boolean performCharge(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level)) {
         return false;
      }
      Entity vehicle = player.getVehicle();
      if (vehicle instanceof GordiusWheelEntity wheel && wheel.isCardOwner(player)) {
         wheel.performCharge(level, player, 42.0F, 4.2);
         return true;
      }
      if (vehicle instanceof BucephalusEntity horse && horse.isCardOwner(player)) {
         horse.performCharge(level, player, 32.0F, 3.2);
         return true;
      }
      Vec3 dir = PlayerNoblePhantasmHelper.horizontalLook(player);
      player.setSprinting(true);
      player.setDeltaMovement(player.getDeltaMovement().add(dir.x * 1.65, 0.08, dir.z * 1.65));
      player.hurtMarked = true;
      player.fallDistance = 0.0F;
      ServantCardSkillUtils.hitForwardArc(player, dir, 7.5, 56.0F);
      for (LivingEntity enemy : nearbyEnemies(player, 7.5)) {
         enemy.push(dir.x * 1.2, 0.28, dir.z * 1.2);
         enemy.hurtMarked = true;
      }
      slashFx(player, dir, 28, 0.62F);
      return true;
   }

   public static boolean performGordiusWheel(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level)) {
         return false;
      }
      CompoundTag data = player.getPersistentData();
      if (data.getBoolean(TAG_GORDIUS_DEAD)) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.iskandar_gordius_dead"), true);
         return false;
      }
      GordiusWheelEntity existing = getGordiusWheel(player);
      if (existing != null) {
         storeAndDiscardGordiusWheel(player, existing, false);
         return true;
      }
      GordiusWheelEntity wheel = ModEntities.GORDIUS_WHEEL.get().create(level);
      if (wheel == null) {
         return false;
      }
      Vec3 pos = player.position().add(PlayerNoblePhantasmHelper.horizontalLook(player).scale(2.0));
      wheel.moveTo(pos.x, pos.y, pos.z, player.getYRot(), 0.0F);
      wheel.bindCardOwner(player);
      double savedHp = data.contains(TAG_GORDIUS_HP) ? data.getDouble(TAG_GORDIUS_HP) : wheel.getMaxHealth();
      wheel.setHealth(Mth.clamp((float)savedHp, 1.0F, wheel.getMaxHealth()));
      wheel.snapRearBodyToCurrentPosition();
      if (!level.addFreshEntity(wheel)) {
         return false;
      }
      data.putUUID(TAG_GORDIUS_UUID, wheel.getUUID());
      player.startRiding(wheel, true);
      level.sendParticles(ParticleTypes.ELECTRIC_SPARK, wheel.getX(), wheel.getY() + 0.9, wheel.getZ(), 90, 2.8, 1.0, 2.8, 0.22);
      level.playSound(null, wheel.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.35F, 0.72F);
      return true;
   }

   public static boolean hasActiveGordiusWheel(ServerPlayer player) {
      return getGordiusWheel(player) != null;
   }

   public static boolean isIonioiActiveOrInside(ServerPlayer player) {
      return data(player).getBoolean(TAG_IONIOI_ACTIVE)
         || player.level() instanceof ServerLevel level && ModDimensions.isIonioiHetairoiDimension(level.dimension().location());
   }

   public static boolean performIonioiHetairoi(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      if (!(player.level() instanceof ServerLevel source)) {
         return false;
      }
      if (isIonioiActiveOrInside(player)) {
         endIonioi(player, true);
         player.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.iskandar_ionioi_released"), true);
         return true;
      }
      CompoundTag data = data(player);
      int lifetimeDeaths = Mth.clamp(data.getInt(TAG_IONIOI_DEATHS), 0, IonioiHetairoiRankPool.TOTAL_SIZE);
      if (lifetimeDeaths >= IonioiHetairoiRankPool.TOTAL_SIZE) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.iskandar_ionioi_exhausted"), true);
         return false;
      }
      LivingEntity target = findPreferredTarget(player, 64.0, 3.0);
      if (target == null) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.no_target"), true);
         return false;
      }
      ServerLevel ionioiLevel = source.getServer().getLevel(ModDimensions.IONIOI_HETAIROI_KEY);
      if (ionioiLevel == null) {
         return false;
      }
      String returnDimension = source.dimension().location().toString();
      double returnX = player.getX();
      double returnY = player.getY();
      double returnZ = player.getZ();
      storeMountedBeforeIonioi(player);
      List<LivingEntity> pulled = collectCardIonioiTargets(player, target);
      Vec3 enemyEntry = safeIonioiEntry(ionioiLevel, randomIonioiEntry(player), target.getBbWidth(), target.getBbHeight());
      Vec3 approach = player.position().subtract(target.position()).multiply(1.0, 0.0, 1.0);
      if (approach.lengthSqr() < 1.0E-4) {
         approach = PlayerNoblePhantasmHelper.horizontalLook(player).reverse();
      }
      if (approach.lengthSqr() < 1.0E-4) {
         approach = new Vec3(0.0, 0.0, -1.0);
      }
      Vec3 armyEntry = safeIonioiEntry(ionioiLevel, enemyEntry.add(approach.normalize().scale(CARD_IONIOI_ARMY_ENTRY_DISTANCE)), player.getBbWidth(), player.getBbHeight());
      UUID session = UUID.randomUUID();
      LivingEntity movedPrimary = moveCardIonioiTargets(source, ionioiLevel, pulled, target, enemyEntry, player.getUUID(), session);

      CompoundTag tag = data(player);
      tag.putBoolean(TAG_IONIOI_ACTIVE, true);
      tag.putLong(TAG_IONIOI_START, ionioiLevel.getGameTime());
      tag.putLong(TAG_IONIOI_LAST_DRAIN, ionioiLevel.getGameTime());
      tag.putUUID(TAG_IONIOI_SESSION, session);
      tag.putString(TAG_IONIOI_RETURN_DIM, returnDimension);
      tag.putDouble(TAG_IONIOI_RETURN_X, returnX);
      tag.putDouble(TAG_IONIOI_RETURN_Y, returnY);
      tag.putDouble(TAG_IONIOI_RETURN_Z, returnZ);
      ServerPlayer activePlayer = player;
      if (!ModDimensions.isIonioiHetairoiDimension(activePlayer.level().dimension().location())) {
         Entity changed = activePlayer.changeDimension(new DimensionTransition(ionioiLevel, armyEntry, Vec3.ZERO, activePlayer.getYRot(), activePlayer.getXRot(), DimensionTransition.DO_NOTHING));
         if (changed instanceof ServerPlayer movedPlayer) {
            activePlayer = movedPlayer;
         }
      }
      if (ModDimensions.isIonioiHetairoiDimension(activePlayer.level().dimension().location())) {
         if (activePlayer.distanceToSqr(armyEntry) > 4.0 * 4.0) {
            activePlayer.teleportTo(armyEntry.x, armyEntry.y, armyEntry.z);
         }
         ServerPlayer movedPlayer = activePlayer;
         rescueIonioiEntity(movedPlayer, armyEntry);
         movedPlayer.getPersistentData().putBoolean(TAG_IONIOI_ACTIVE, true);
         movedPlayer.getPersistentData().putLong(TAG_IONIOI_START, ionioiLevel.getGameTime());
         movedPlayer.getPersistentData().putLong(TAG_IONIOI_LAST_DRAIN, ionioiLevel.getGameTime());
         movedPlayer.getPersistentData().putUUID(TAG_IONIOI_SESSION, session);
         movedPlayer.getPersistentData().putString(TAG_IONIOI_RETURN_DIM, returnDimension);
         movedPlayer.getPersistentData().putDouble(TAG_IONIOI_RETURN_X, returnX);
         movedPlayer.getPersistentData().putDouble(TAG_IONIOI_RETURN_Y, returnY);
         movedPlayer.getPersistentData().putDouble(TAG_IONIOI_RETURN_Z, returnZ);
         movedPlayer.getPersistentData().putInt(TAG_IONIOI_DEATHS, lifetimeDeaths);
         movedPlayer.getPersistentData().putLong(TAG_IONIOI_SEED, ionioiSeed(data(player), player));
         restoreStoredMountAfterIonioi(movedPlayer);
         movedPlayer.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 80, 4, false, false, false));
         movedPlayer.fallDistance = 0.0F;
         movedPlayer.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).syncServantCardRuntime(movedPlayer);
         scheduleIonioiEntryRescue(movedPlayer, armyEntry);
         spawnCardIonioiFormation(movedPlayer, ionioiLevel, movedPrimary != null ? movedPrimary : target);
      }
      ionioiLevel.sendParticles(ParticleTypes.FLASH, armyEntry.x, armyEntry.y + 1.2, armyEntry.z, 6, 0.0, 0.0, 0.0, 0.0);
      ionioiLevel.playSound(null, BlockPos.containing(armyEntry), SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 1.2F, 0.85F);
      return true;
   }

   public static void storeAndDiscardBucephalus(ServerPlayer player, IskandarMountEntity mount, boolean markDead) {
      if (mount instanceof BucephalusEntity) {
         storeAndDiscardMount(player, mount, TAG_BUCEPHALUS_UUID, TAG_BUCEPHALUS_HP, TAG_BUCEPHALUS_DEAD, markDead);
      }
   }

   public static void storeAndDiscardGordiusWheel(ServerPlayer player, IskandarMountEntity mount, boolean markDead) {
      if (mount instanceof GordiusWheelEntity) {
         storeAndDiscardMount(player, mount, TAG_GORDIUS_UUID, TAG_GORDIUS_HP, TAG_GORDIUS_DEAD, markDead);
      }
   }

   private static void tickMountState(ServerPlayer player) {
      CompoundTag data = data(player);
      if (data.hasUUID(TAG_BUCEPHALUS_UUID) && getBucephalus(player) == null) {
         data.remove(TAG_BUCEPHALUS_UUID);
         data.putBoolean(TAG_BUCEPHALUS_DEAD, true);
         data.putDouble(TAG_BUCEPHALUS_HP, 0.0);
      }
      if (data.hasUUID(TAG_GORDIUS_UUID) && getGordiusWheel(player) == null) {
         data.remove(TAG_GORDIUS_UUID);
         data.putBoolean(TAG_GORDIUS_DEAD, true);
         data.putDouble(TAG_GORDIUS_HP, 0.0);
      }
   }

   private static void tickPassives(ServerPlayer player) {
      boolean mounted = player.getVehicle() instanceof IskandarMountEntity mount && mount.isCardOwner(player);
      if (mounted) {
         ServantCardSkillUtils.addOrReplaceMultiplied(player.getAttribute(Attributes.MOVEMENT_SPEED), RIDING_SPEED_ID, 0.18);
      } else {
         ServantCardSkillUtils.remove(player.getAttribute(Attributes.MOVEMENT_SPEED), RIDING_SPEED_ID);
      }
      if (countNearbyAllies(player, 18.0) >= 3) {
         ServantCardSkillUtils.addOrReplaceMultiplied(player.getAttribute(Attributes.MOVEMENT_SPEED), LEADERSHIP_SPEED_ID, 0.08);
      } else {
         ServantCardSkillUtils.remove(player.getAttribute(Attributes.MOVEMENT_SPEED), LEADERSHIP_SPEED_ID);
      }
   }

   private static void tickIonioi(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      CompoundTag data = data(player);
      if (!data.getBoolean(TAG_IONIOI_ACTIVE)) {
         return;
      }
      if (!(player.level() instanceof ServerLevel level)) {
         return;
      }
      if (!player.isAlive() || !ModDimensions.isIonioiHetairoiDimension(level.dimension().location())) {
         endIonioi(player, true);
         return;
      }
      long now = level.getGameTime();
      long start = data.getLong(TAG_IONIOI_START);
      if (now > start + IONIOI_FREE_UPKEEP_TICKS && now - data.getLong(TAG_IONIOI_LAST_DRAIN) >= 20L) {
         if (!ServantCardManaService.consumeSilently(player, vars, IONIOI_UPKEEP_MP_PER_SECOND)) {
            endIonioi(player, true);
            return;
         }
         data.putLong(TAG_IONIOI_LAST_DRAIN, now);
      }
      if (data.getInt(TAG_IONIOI_DEATHS) >= IonioiHetairoiRankPool.TOTAL_SIZE) {
         endIonioi(player, true);
         return;
      }
      if (player.tickCount % 8 == 0) {
         level.sendParticles(ParticleTypes.DUST_PLUME, player.getX(), player.getY() + 0.2, player.getZ(), 10, 1.2, 0.12, 1.2, 0.05);
      }
   }

   private static void tickConquerorOrder(ServerPlayer player) {
      CompoundTag data = data(player);
      if (data.getLong(TAG_ORDER_UNTIL) <= player.level().getGameTime() || !data.hasUUID(TAG_ORDER_TARGET)) {
         data.remove(TAG_ORDER_TARGET);
         data.remove(TAG_ORDER_UNTIL);
         return;
      }
      if (!(player.level() instanceof ServerLevel level)) {
         return;
      }
      Entity entity = level.getEntity(data.getUUID(TAG_ORDER_TARGET));
      if (!(entity instanceof LivingEntity target) || !isEnemy(player, target)) {
         data.remove(TAG_ORDER_TARGET);
         data.remove(TAG_ORDER_UNTIL);
         return;
      }
      for (MacedonianSoldierEntity soldier : level.getEntitiesOfClass(MacedonianSoldierEntity.class, player.getBoundingBox().inflate(32.0))) {
         if (soldier.isAlliedTo(player)) {
            soldier.setTarget(target);
         }
      }
      for (Mob mob : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(24.0),
         mob -> mob.isAlliedTo(player) || player.isAlliedTo(mob))) {
         mob.setTarget(target);
      }
   }

   private static void endIonioi(ServerPlayer player, boolean returnPlayer) {
      CompoundTag data = data(player);
      if (!(player.level() instanceof ServerLevel level)) {
         clearIonioiData(data);
         return;
      }
      boolean inIonioi = ModDimensions.isIonioiHetairoiDimension(level.dimension().location());
      if (data.getBoolean(TAG_IONIOI_ACTIVE) || inIonioi) {
         UUID ownerId = player.getUUID();
         UUID session = data.hasUUID(TAG_IONIOI_SESSION) ? data.getUUID(TAG_IONIOI_SESSION) : null;
         boolean hasReturn = data.contains(TAG_IONIOI_RETURN_DIM);
         ServerLevel returnLevel = hasReturn ? resolveDimension(level, data.getString(TAG_IONIOI_RETURN_DIM)) : level.getServer().overworld();
         Vec3 returnPos = hasReturn
            ? new Vec3(data.getDouble(TAG_IONIOI_RETURN_X), data.getDouble(TAG_IONIOI_RETURN_Y), data.getDouble(TAG_IONIOI_RETURN_Z))
            : Vec3.atBottomCenterOf(returnLevel.getSharedSpawnPos());
         returnPos = safeEntry(returnLevel, returnPos, player.getBbWidth(), player.getBbHeight());
         if (!returnPlayer) {
            clearIonioiData(data);
            cleanupCardIonioiAfterExit(ownerId, session, level, returnLevel);
            return;
         }
         storeMountedBeforeIonioi(player);
         clearIonioiData(data);
         player.teleportTo(returnLevel, returnPos.x, returnPos.y, returnPos.z, player.getYRot(), player.getXRot());
         clearIonioiData(player.getPersistentData());
         restoreStoredMountAfterIonioi(player);
         player.fallDistance = 0.0F;
         player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 4, false, false, false));
         cleanupCardIonioiAfterExit(ownerId, session, level, returnLevel);
         TYPE_MOON_WORLD.queueServerWork(5, () -> cleanupCardIonioiAfterExit(ownerId, session, level, returnLevel));
         return;
      }
      clearIonioiData(data);
   }

   private static void cleanupCardIonioiAfterExit(UUID ownerId, @Nullable UUID session, ServerLevel sourceLevel, ServerLevel fallbackLevel) {
      returnCardIonioiTargets(ownerId, session, sourceLevel, fallbackLevel);
      discardCardIonioiSoldiers(ownerId, sourceLevel);
      MacedonianSoldierEntity.clearFormationCache(ownerId);
   }

   private static void storeMountedBeforeIonioi(ServerPlayer player) {
      Entity vehicle = player.getVehicle();
      if (vehicle instanceof BucephalusEntity horse && horse.isCardOwner(player)) {
         data(player).putString(TAG_IONIOI_MOUNT_TYPE, "bucephalus");
         data(player).putBoolean(TAG_IONIOI_MOUNT_FLYING, false);
         storeAndDiscardBucephalus(player, horse, false);
      } else if (vehicle instanceof GordiusWheelEntity wheel && wheel.isCardOwner(player)) {
         data(player).putString(TAG_IONIOI_MOUNT_TYPE, "gordius_wheel");
         data(player).putBoolean(TAG_IONIOI_MOUNT_FLYING, wheel.isFlyingMode());
         storeAndDiscardGordiusWheel(player, wheel, false);
      }
   }

   private static void restoreStoredMountAfterIonioi(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level)) {
         return;
      }
      CompoundTag data = data(player);
      String type = data.getString(TAG_IONIOI_MOUNT_TYPE);
      if (type.isBlank()) {
         return;
      }
      IskandarMountEntity mount = "gordius_wheel".equals(type)
         ? ModEntities.GORDIUS_WHEEL.get().create(level)
         : ModEntities.BUCEPHALUS.get().create(level);
      if (mount == null) {
         return;
      }
      Vec3 forward = PlayerNoblePhantasmHelper.horizontalLook(player);
      Vec3 pos = safeEntry(level, player.position().subtract(forward.scale(1.0)), mount.getBbWidth(), mount.getBbHeight());
      mount.moveTo(pos.x, pos.y, pos.z, player.getYRot(), 0.0F);
      mount.bindCardOwner(player);
      double hp = "gordius_wheel".equals(type) ? data.getDouble(TAG_GORDIUS_HP) : data.getDouble(TAG_BUCEPHALUS_HP);
      mount.setHealth(Mth.clamp((float)hp, 1.0F, mount.getMaxHealth()));
      if (mount instanceof GordiusWheelEntity wheel) {
         wheel.restoreFlyingMode(data.getBoolean(TAG_IONIOI_MOUNT_FLYING));
         wheel.snapRearBodyToCurrentPosition();
      }
      if (!level.addFreshEntity(mount)) {
         return;
      }
      if (mount instanceof GordiusWheelEntity) {
         data.putUUID(TAG_GORDIUS_UUID, mount.getUUID());
      } else {
         data.putUUID(TAG_BUCEPHALUS_UUID, mount.getUUID());
      }
      player.startRiding(mount, true);
      data.remove(TAG_IONIOI_MOUNT_TYPE);
      data.remove(TAG_IONIOI_MOUNT_FLYING);
   }

   private static void storeAndDiscardMount(ServerPlayer player, IskandarMountEntity mount, String uuidTag, String hpTag, String deadTag, boolean markDead) {
      CompoundTag data = data(player);
      data.putDouble(hpTag, Math.max(0.0F, mount.getHealth()));
      if (markDead || mount.getHealth() <= 0.0F) {
         data.putBoolean(deadTag, true);
         data.putDouble(hpTag, 0.0);
      }
      data.remove(uuidTag);
      if (mount.isPassenger()) {
         mount.stopRiding();
      }
      mount.ejectPassengers();
      if (!mount.isRemoved()) {
         mount.discard();
      }
   }

   private static void clearMount(ServerPlayer player, String uuidTag, boolean markDead) {
      CompoundTag data = data(player);
      if (data.hasUUID(uuidTag) && player.level() instanceof ServerLevel level) {
         Entity entity = level.getEntity(data.getUUID(uuidTag));
         if (entity instanceof IskandarMountEntity mount) {
            if (uuidTag.equals(TAG_BUCEPHALUS_UUID)) {
               storeAndDiscardBucephalus(player, mount, markDead);
            } else {
               storeAndDiscardGordiusWheel(player, mount, markDead);
            }
            return;
         }
      }
      data.remove(uuidTag);
   }

   @Nullable
   private static BucephalusEntity getBucephalus(ServerPlayer player) {
      Entity entity = getEntity(player, TAG_BUCEPHALUS_UUID);
      return entity instanceof BucephalusEntity horse && horse.isAlive() && horse.isCardOwner(player) ? horse : null;
   }

   @Nullable
   private static GordiusWheelEntity getGordiusWheel(ServerPlayer player) {
      Entity entity = getEntity(player, TAG_GORDIUS_UUID);
      return entity instanceof GordiusWheelEntity wheel && wheel.isAlive() && wheel.isCardOwner(player) ? wheel : null;
   }

   private static List<LivingEntity> collectCardIonioiTargets(ServerPlayer player, LivingEntity primary) {
      List<LivingEntity> pulled = new ArrayList<>();
      pulled.add(primary);
      if (!(player.level() instanceof ServerLevel level)) {
         return pulled;
      }
      for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(CARD_IONIOI_PULL_RADIUS),
         entity -> entity != primary && isEnemy(player, entity))) {
         if (pulled.size() >= 16) {
            break;
         }
         pulled.add(candidate);
      }
      return pulled;
   }

   @Nullable
   private static LivingEntity moveCardIonioiTargets(ServerLevel source, ServerLevel ionioiLevel, List<LivingEntity> pulled,
      LivingEntity primary, Vec3 enemyEntry, UUID ownerId, UUID session) {
      LivingEntity movedPrimary = null;
      int index = 0;
      for (LivingEntity target : pulled) {
         if (target == null || !target.isAlive()) {
            continue;
         }
         CompoundTag targetData = target.getPersistentData();
         targetData.putUUID(TAG_IONIOI_TARGET_OWNER, ownerId);
         targetData.putUUID(TAG_IONIOI_TARGET_SESSION, session);
         targetData.putString(TAG_IONIOI_TARGET_RETURN_DIM, target.level().dimension().location().toString());
         targetData.putDouble(TAG_IONIOI_TARGET_RETURN_X, target.getX());
         targetData.putDouble(TAG_IONIOI_TARGET_RETURN_Y, target.getY());
         targetData.putDouble(TAG_IONIOI_TARGET_RETURN_Z, target.getZ());
         targetData.putBoolean(TAG_IONIOI_TARGET_PRIMARY, target == primary);
         double angle = index == 0 ? 0.0 : (Math.PI * 2.0 * index / Math.max(2, pulled.size()));
         double radius = index == 0 ? 0.0 : 2.0 + (index % 3) * 1.35;
         Vec3 destination = safeIonioiEntry(ionioiLevel,
            enemyEntry.add(Math.cos(angle) * radius, 0.0, Math.sin(angle) * radius),
            target.getBbWidth(), target.getBbHeight());
         LivingEntity moved = target;
         if (target.level() != ionioiLevel) {
            Entity changed = target.changeDimension(new DimensionTransition(ionioiLevel, destination, Vec3.ZERO, target.getYRot(), target.getXRot(), DimensionTransition.DO_NOTHING));
            if (changed instanceof LivingEntity living) {
               moved = living;
            }
         } else {
            target.teleportTo(destination.x, destination.y, destination.z);
         }
         rescueIonioiEntity(moved, destination);
         scheduleIonioiEntryRescue(moved, destination);
         CompoundTag movedData = moved.getPersistentData();
         movedData.putUUID(TAG_IONIOI_TARGET_OWNER, ownerId);
         movedData.putUUID(TAG_IONIOI_TARGET_SESSION, session);
         movedData.putString(TAG_IONIOI_TARGET_RETURN_DIM, targetData.getString(TAG_IONIOI_TARGET_RETURN_DIM));
         movedData.putDouble(TAG_IONIOI_TARGET_RETURN_X, targetData.getDouble(TAG_IONIOI_TARGET_RETURN_X));
         movedData.putDouble(TAG_IONIOI_TARGET_RETURN_Y, targetData.getDouble(TAG_IONIOI_TARGET_RETURN_Y));
         movedData.putDouble(TAG_IONIOI_TARGET_RETURN_Z, targetData.getDouble(TAG_IONIOI_TARGET_RETURN_Z));
         movedData.putBoolean(TAG_IONIOI_TARGET_PRIMARY, target == primary);
         moved.fallDistance = 0.0F;
         if (target == primary) {
            movedPrimary = moved;
         }
         index++;
      }
      return movedPrimary;
   }

   private static void spawnCardIonioiFormation(ServerPlayer owner, ServerLevel level, @Nullable LivingEntity primaryTarget) {
      CompoundTag data = data(owner);
      int startIndex = Mth.clamp(data.getInt(TAG_IONIOI_DEATHS), 0, IonioiHetairoiRankPool.TOTAL_SIZE);
      IonioiHetairoiRankPool pool = IonioiHetairoiRankPool.create(ionioiSeed(data, owner));
      Vec3 forward = primaryTarget != null && primaryTarget.isAlive()
         ? primaryTarget.position().subtract(owner.position()).multiply(1.0, 0.0, 1.0)
         : PlayerNoblePhantasmHelper.horizontalLook(owner);
      if (forward.lengthSqr() < 1.0E-4) {
         forward = new Vec3(0.0, 0.0, 1.0);
      }
      forward = forward.normalize();
      Vec3 right = new Vec3(-forward.z, 0.0, forward.x);
      int side = 5;
      double spacing = 1.35;
      double groupPitch = spacing * side + 2.4;
      int count = Math.min(CARD_IONIOI_ACTIVE_CAP, IonioiHetairoiRankPool.TOTAL_SIZE - startIndex);
      for (int i = 0; i < count; i++) {
         MacedonianSoldierEntity soldier = ModEntities.MACEDONIAN_SOLDIER.get().create(level);
         if (soldier == null) {
            continue;
         }
         int formation = i / 25;
         int local = i % 25;
         int row = local / side;
         int col = local % side;
         double localX = (formation % 8 - 3.5) * groupPitch + (col - 2.0) * spacing;
         double localZ = (formation / 8 - 0.5) * groupPitch + (row - 2.0) * spacing;
         Vec3 pos = safeIonioiEntry(level, owner.position().add(right.scale(localX)).add(forward.scale(localZ)), soldier.getBbWidth(), soldier.getBbHeight());
         soldier.moveTo(pos.x, pos.y, pos.z, owner.getYRot(), 0.0F);
         int poolIndex = startIndex + i;
         soldier.initializeForServantCardIonioi(owner, primaryTarget, pool.rankAt(poolIndex), poolIndex);
         level.addFreshEntity(soldier);
      }
   }

   public static void onMacedonianSoldierDeath(ServerPlayer owner, MacedonianSoldierEntity soldier) {
      CompoundTag data = data(owner);
      if (!data.getBoolean(TAG_IONIOI_ACTIVE)) {
         return;
      }
      data.putInt(TAG_IONIOI_DEATHS, Math.min(IonioiHetairoiRankPool.TOTAL_SIZE, data.getInt(TAG_IONIOI_DEATHS) + 1));
   }

   private static void returnCardIonioiTargets(UUID ownerId, @Nullable UUID session, ServerLevel sourceLevel, ServerLevel fallbackLevel) {
      List<LivingEntity> toReturn = new ArrayList<>();
      for (ServerLevel scanLevel : sourceLevel.getServer().getAllLevels()) {
         for (Entity candidate : scanLevel.getEntities().getAll()) {
            if (candidate instanceof LivingEntity living && isPulledByCurrentCardIonioi(ownerId, session, living)) {
               toReturn.add(living);
            }
         }
      }
      for (LivingEntity living : toReturn) {
         CompoundTag targetData = living.getPersistentData();
         ServerLevel returnLevel = resolveDimension(fallbackLevel, targetData.getString(TAG_IONIOI_TARGET_RETURN_DIM));
         Vec3 returnPos = safeEntry(returnLevel, new Vec3(
            targetData.getDouble(TAG_IONIOI_TARGET_RETURN_X),
            targetData.getDouble(TAG_IONIOI_TARGET_RETURN_Y),
            targetData.getDouble(TAG_IONIOI_TARGET_RETURN_Z)
         ), living.getBbWidth(), living.getBbHeight());
         clearCardIonioiTarget(living);
         if (living.level() != returnLevel) {
            living.changeDimension(new DimensionTransition(returnLevel, returnPos, Vec3.ZERO, living.getYRot(), living.getXRot(), DimensionTransition.DO_NOTHING));
         } else {
            living.teleportTo(returnPos.x, returnPos.y, returnPos.z);
         }
         living.fallDistance = 0.0F;
      }
   }

   private static boolean isPulledByCurrentCardIonioi(UUID ownerId, @Nullable UUID session, LivingEntity living) {
      CompoundTag data = living.getPersistentData();
      return session != null
         && data.hasUUID(TAG_IONIOI_TARGET_OWNER)
         && ownerId.equals(data.getUUID(TAG_IONIOI_TARGET_OWNER))
         && data.hasUUID(TAG_IONIOI_TARGET_SESSION)
         && session.equals(data.getUUID(TAG_IONIOI_TARGET_SESSION));
   }

   private static void discardCardIonioiSoldiers(UUID ownerId, ServerLevel level) {
      for (ServerLevel scanLevel : level.getServer().getAllLevels()) {
         for (Entity candidate : scanLevel.getEntities().getAll()) {
            if (candidate instanceof MacedonianSoldierEntity soldier && soldier.isServantCardIonioiSoldier(ownerId)) {
               soldier.discard();
            }
         }
      }
   }

   private static void clearCardIonioiTarget(LivingEntity living) {
      CompoundTag data = living.getPersistentData();
      data.remove(TAG_IONIOI_TARGET_OWNER);
      data.remove(TAG_IONIOI_TARGET_SESSION);
      data.remove(TAG_IONIOI_TARGET_RETURN_DIM);
      data.remove(TAG_IONIOI_TARGET_RETURN_X);
      data.remove(TAG_IONIOI_TARGET_RETURN_Y);
      data.remove(TAG_IONIOI_TARGET_RETURN_Z);
      data.remove(TAG_IONIOI_TARGET_PRIMARY);
   }

   private static Vec3 randomIonioiEntry(ServerPlayer player) {
      Vec3 entry = UBWInstanceManager.randomEntryPosition(player.getRandom());
      return new Vec3(entry.x, 72.0, entry.z);
   }

   private static long ionioiSeed(CompoundTag data, ServerPlayer owner) {
      if (!data.contains(TAG_IONIOI_SEED)) {
         data.putLong(TAG_IONIOI_SEED, owner.getRandom().nextLong());
      }
      return data.getLong(TAG_IONIOI_SEED);
   }

   @Nullable
   private static Entity getEntity(ServerPlayer player, String tag) {
      CompoundTag data = data(player);
      if (!data.hasUUID(tag) || !(player.level() instanceof ServerLevel level)) {
         return null;
      }
      return level.getEntity(data.getUUID(tag));
   }

   @Nullable
   private static LivingEntity findPreferredTarget(ServerPlayer player, double range, double inflate) {
      LivingEntity look = ServantCardSkillUtils.findAutomaticLookTarget(player, range, inflate);
      if (look != null) {
         return look;
      }
      LivingEntity lastHurt = player.getLastHurtMob();
      if (isEnemy(player, lastHurt) && player.distanceToSqr(lastHurt) <= range * range) {
         return lastHurt;
      }
      LivingEntity attacker = player.getLastHurtByMob();
      if (isEnemy(player, attacker) && player.distanceToSqr(attacker) <= range * range) {
         return attacker;
      }
      return nearbyEnemies(player, range).stream().findFirst().orElse(null);
   }

   private static List<LivingEntity> nearbyEnemies(ServerPlayer player, double radius) {
      if (!(player.level() instanceof ServerLevel level)) {
         return List.of();
      }
      return level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(radius),
         entity -> isEnemy(player, entity));
   }

   private static boolean isEnemy(ServerPlayer player, @Nullable LivingEntity entity) {
      return entity != null
         && entity != player
         && entity.isAlive()
         && !EntityUtils.isImmunePlayerTarget(entity)
         && !ServantMasterTargeting.isContractMaster(player, entity)
         && !ServantMasterProtection.isProtectedMaster(player, entity)
         && !player.isAlliedTo(entity)
         && !entity.isAlliedTo(player)
         && (!(entity instanceof IskandarMountEntity mount) || !mount.isCardOwner(player))
         && EntityUtils.isValidCombatTarget(player, entity);
   }

   private static int countNearbyAllies(ServerPlayer player, double radius) {
      if (!(player.level() instanceof ServerLevel level)) {
         return 0;
      }
      int count = 0;
      for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(radius))) {
         if (entity != player && entity.isAlive() && (player.isAlliedTo(entity) || entity.isAlliedTo(player)
            || entity instanceof MacedonianSoldierEntity soldier && soldier.isAlliedTo(player)
            || entity instanceof IskandarMountEntity mount && mount.isCardOwner(player))) {
            count++;
         }
      }
      return count;
   }

   private static void slashFx(ServerPlayer player, Vec3 dir, int particles, float pitch) {
      if (!(player.level() instanceof ServerLevel level)) {
         return;
      }
      Vec3 center = player.position().add(dir.normalize().scale(2.0)).add(0.0, player.getBbHeight() * 0.55, 0.0);
      level.sendParticles(ParticleTypes.SWEEP_ATTACK, center.x, center.y, center.z, 3, 0.0, 0.0, 0.0, 0.0);
      level.sendParticles(ParticleTypes.CRIT, center.x, center.y, center.z, particles, 0.45, 0.32, 0.45, 0.08);
      level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.8F, pitch);
   }

   private static void spawnVisualLightning(ServerLevel level, Vec3 pos) {
      net.minecraft.world.entity.LightningBolt lightning = net.minecraft.world.entity.EntityType.LIGHTNING_BOLT.create(level);
      if (lightning == null) {
         return;
      }
      lightning.moveTo(pos.x, pos.y, pos.z);
      lightning.setVisualOnly(true);
      level.addFreshEntity(lightning);
   }

   private static Vec3 safeIonioiEntry(ServerLevel level, Vec3 requested, float width, float height) {
      Vec3 checked = safeEntryUpward(level, requested.x, requested.y, requested.z, width, height);
      if (checked != null) {
         return checked;
      }
      int surfaceY = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(requested.x), Mth.floor(requested.z));
      checked = safeEntryUpward(level, requested.x, Math.max(requested.y, surfaceY + 1.0), requested.z, width, height);
      if (checked != null) {
         return checked;
      }
      return new Vec3(requested.x, Mth.clamp(surfaceY + 2.0, level.getMinBuildHeight() + 2.0, level.getMaxBuildHeight() - height - 1.0), requested.z);
   }

   private static void rescueIonioiEntity(Entity entity, Vec3 requested) {
      if (!(entity.level() instanceof ServerLevel level) || !ModDimensions.isIonioiHetairoiDimension(level.dimension().location())) {
         return;
      }
      float width = entity.getBbWidth();
      float height = entity.getBbHeight();
      Vec3 safe = safeIonioiEntry(level, requested, width, height);
      entity.teleportTo(safe.x, safe.y, safe.z);
      entity.setDeltaMovement(Vec3.ZERO);
      entity.fallDistance = 0.0F;
      entity.hurtMarked = true;
   }

   private static void scheduleIonioiEntryRescue(Entity entity, Vec3 requested) {
      UUID id = entity.getUUID();
      TYPE_MOON_WORLD.queueServerWork(1, () -> rescueIonioiEntityById(entity, id, requested));
      TYPE_MOON_WORLD.queueServerWork(5, () -> rescueIonioiEntityById(entity, id, requested));
   }

   private static void rescueIonioiEntityById(Entity captured, UUID id, Vec3 requested) {
      if (!(captured.level() instanceof ServerLevel level)) {
         return;
      }
      Entity current = level.getEntity(id);
      if (current != null && current.isAlive()) {
         rescueIonioiEntity(current, requested);
      }
   }

   private static Vec3 safeEntry(ServerLevel level, Vec3 requested, float width, float height) {
      Vec3 checked = safeEntryColumn(level, requested.x, requested.y, requested.z, width, height);
      if (checked != null) {
         return checked;
      }
      for (int radius = 1; radius <= 8; radius++) {
         for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
               if (Math.abs(dx) != radius && Math.abs(dz) != radius) {
                  continue;
               }
               checked = safeEntryColumn(level, requested.x + dx, requested.y, requested.z + dz, width, height);
               if (checked != null) {
                  return checked;
               }
            }
         }
      }
      int surfaceY = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(requested.x), Mth.floor(requested.z));
      return new Vec3(requested.x, Math.max(level.getMinBuildHeight() + 2, surfaceY + 1), requested.z);
   }

   @Nullable
   private static Vec3 safeEntryUpward(ServerLevel level, double x, double y, double z, float width, float height) {
      int startY = Mth.clamp(Mth.floor(y), level.getMinBuildHeight() + 1, level.getMaxBuildHeight() - Mth.ceil(height) - 1);
      int maxY = level.getMaxBuildHeight() - Mth.ceil(height) - 1;
      for (int candidateY = startY; candidateY <= maxY; candidateY++) {
         Vec3 pos = new Vec3(x, candidateY + 0.05, z);
         if (isSafeEntry(level, pos, width, height)) {
            return pos;
         }
      }
      return null;
   }

   @Nullable
   private static Vec3 safeEntryColumn(ServerLevel level, double x, double requestedY, double z, float width, float height) {
      if (!level.getWorldBorder().isWithinBounds(BlockPos.containing(x, requestedY, z))) {
         return null;
      }
      int blockX = Mth.floor(x);
      int blockZ = Mth.floor(z);
      int y = Mth.floor(requestedY);
      for (int dy = 0; dy <= 8; dy++) {
         Vec3 pos = new Vec3(x, y + dy, z);
         if (isSafeEntry(level, pos, width, height)) {
            return pos;
         }
      }
      int surfaceY = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, blockX, blockZ);
      Vec3 surface = new Vec3(x, Math.max(level.getMinBuildHeight() + 2, surfaceY + 1), z);
      return isSafeEntry(level, surface, width, height) ? surface : null;
   }

   private static boolean isSafeEntry(ServerLevel level, Vec3 pos, float width, float height) {
      if (!level.getWorldBorder().isWithinBounds(BlockPos.containing(pos))) {
         return false;
      }
      AABB box = new AABB(
         pos.x - width * 0.5F,
         pos.y,
         pos.z - width * 0.5F,
         pos.x + width * 0.5F,
         pos.y + height,
         pos.z + width * 0.5F
      );
      return pos.y >= level.getMinBuildHeight() + 1
         && pos.y + height < level.getMaxBuildHeight() - 1
         && level.noCollision(box);
   }

   private static ServerLevel resolveDimension(ServerLevel level, String id) {
      ResourceLocation location = ResourceLocation.tryParse(id == null ? "" : id);
      if (location != null) {
         ServerLevel resolved = level.getServer().getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, location));
         if (resolved != null) {
            return resolved;
         }
      }
      return level.getServer().overworld();
   }

   private static void clearIonioiData(CompoundTag data) {
      data.remove(TAG_IONIOI_ACTIVE);
      data.remove(TAG_IONIOI_START);
      data.remove(TAG_IONIOI_RETURN_DIM);
      data.remove(TAG_IONIOI_RETURN_X);
      data.remove(TAG_IONIOI_RETURN_Y);
      data.remove(TAG_IONIOI_RETURN_Z);
      data.remove(TAG_IONIOI_LAST_DRAIN);
      data.remove(TAG_IONIOI_SESSION);
   }

   private static CompoundTag data(ServerPlayer player) {
      return player.getPersistentData();
   }

   private static ResourceLocation id(String path) {
      return ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, path);
   }
}
