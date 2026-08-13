package net.xxxjk.TYPE_MOON_WORLD.servant.card;

import java.util.UUID;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.xxxjk.TYPE_MOON_WORLD.entity.CrimsonHoundProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.RhoAiasEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.UBWInterceptorSwordEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.UBWProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.SwordBarrelProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.ChainsOfHeavenBindingEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.DragonfangSoldierEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.EnkiduEarthWeaponProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.MedeaMagicBoltEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.OdaMatchlockBulletEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.RedSkeletonHajunEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.GilgameshCrossSlashEntity;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.init.ModParticles;
import net.xxxjk.TYPE_MOON_WORLD.init.ModSounds;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.PlayerNoblePhantasmHelper;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.NoblePhantasmItem;
import net.xxxjk.TYPE_MOON_WORLD.magic.projection.RhoAiasProjectionHelper;
import net.xxxjk.TYPE_MOON_WORLD.magic.projection.MagicStructuralAnalysis;
import net.xxxjk.TYPE_MOON_WORLD.magic.unlimited_blade_works.ChantHandler;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantIdentityHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.MagicResistanceHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.MagicResistanceRank;
import net.xxxjk.TYPE_MOON_WORLD.servant.data.ServantDataRegistry;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.CursedArmHassanCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.HeraclesGodHandHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.OdaNobunagaCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.GilgameshEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantDefinition;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantParams;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantTraitTag;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXServerEffects;

import static net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardSkillUtils.*;

@EventBusSubscriber(modid = "typemoonworld")
public final class ServantCardEmiyaSkills {
   private static final String EMIYA_UBW_FIRST_LINE = "\u00A7bI am the bone of my sword.";

   private static final String EMIYA_LAYERED_OLD_UNTIL = "ServantCardEmiyaLayeredProjectionUntil";
   private static final String EMIYA_LAYERED_ROUNDS = "ServantCardEmiyaLayeredProjectionRounds";
   private static final String EMIYA_LAYERED_NEXT_TICK = "ServantCardEmiyaLayeredProjectionNextTick";
   private static final int EMIYA_LAYERED_TOTAL_ROUNDS = 5;
   private static final int EMIYA_LAYERED_ROUND_INTERVAL = 10;
   private static final int EMIYA_LAYERED_SWORDS_PER_ROUND = 30;
   private static final String EMIYA_SPHERICAL_ROUNDS = "ServantCardEmiyaSphericalProjectionRounds";
   private static final String EMIYA_SPHERICAL_NEXT_TICK = "ServantCardEmiyaSphericalProjectionNextTick";
   private static final String EMIYA_SPHERICAL_TARGET = "ServantCardEmiyaSphericalProjectionTarget";
   private static final int EMIYA_SPHERICAL_TOTAL_ROUNDS = 10;
   private static final int EMIYA_SPHERICAL_ROUND_INTERVAL = 10;
   private static final String EMIYA_AUTO_COUNTER_UNTIL = "ServantCardEmiyaAutoCounterUntil";
   private static final String EMIYA_AUTO_COUNTER_CLAIMED = "ServantCardEmiyaAutoCounterClaimed";
   private static final int EMIYA_AUTO_COUNTER_DURATION = 300;
   private static final String EMIYA_UBW_NEXT_CRIMSON_HOUND = "ServantCardEmiyaUbwNextCrimsonHound";
   private static final int EMIYA_UBW_CRIMSON_HOUND_INTERVAL = 10 * 20;
   private static final String EMIYA_COPIED_NP_PENDING_TOKEN = "ServantCardEmiyaCopiedNpPendingToken";
   private static final String EMIYA_COPIED_NP_PENDING_TICK = "ServantCardEmiyaCopiedNpPendingTick";
   private ServantCardEmiyaSkills() {
   }

   public static void startLayeredProjection(ServerPlayer player) {
      CompoundTag data = player.getPersistentData();
      data.remove(EMIYA_LAYERED_OLD_UNTIL);
      data.putInt(EMIYA_LAYERED_ROUNDS, EMIYA_LAYERED_TOTAL_ROUNDS);
      data.putInt(EMIYA_LAYERED_NEXT_TICK, player.tickCount);
   }

   public static void cycleAction(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      vars.servant_card_action_mode = nextEmiyaCycleMode(vars);
      cycleLoadout(player, vars);
   }

   public static void equipUniqueAmmo(ServerPlayer player, Item item) {
      ItemStack displaced = player.getOffhandItem().copy();
      removeAll(player, item);
      if (!displaced.isEmpty() && !displaced.is(item) && !player.getInventory().add(displaced)) player.drop(displaced, false);
      ItemStack ammo = new ItemStack(item);
      PlayerNoblePhantasmHelper.markUbwProjection(ammo);
      ServantCardTransformManager.markGeneratedItem(ammo, true, true);
      player.setItemInHand(InteractionHand.OFF_HAND, ammo);
   }

   private static void removeAll(ServerPlayer player, Item item) {
      for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
         if (player.getInventory().getItem(i).is(item)) player.getInventory().setItem(i, ItemStack.EMPTY);
      }
   }

   public static void startUbwChant(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      vars.has_unlimited_blade_works = true;
      vars.is_chanting_ubw = true;
      vars.ubw_chant_progress = 0;
      vars.ubw_chant_timer = 0;
      PlayerNoblePhantasmHelper.startServantCardVoiceSession(player, "emiya_archer", ModSounds.EMIYA_ARCHER_VOICE_UBW.get());
      player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 0, false, true, true));
      player.displayClientMessage(Component.literal(EMIYA_UBW_FIRST_LINE), true);
   }
   public static void equipPair(ServerPlayer player, net.minecraft.world.item.Item main, net.minecraft.world.item.Item off) {
      ItemStack mainStack = new ItemStack(main);
      ItemStack offStack = new ItemStack(off);
      PlayerNoblePhantasmHelper.markUbwProjection(mainStack);
      PlayerNoblePhantasmHelper.markUbwProjection(offStack);
      ServantCardTransformManager.markGeneratedItem(mainStack, true, true);
      ServantCardTransformManager.markGeneratedItem(offStack, true, true);
      player.setItemInHand(InteractionHand.MAIN_HAND, mainStack);
      player.setItemInHand(InteractionHand.OFF_HAND, offStack);
   }

   public static void cycleLoadout(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      List<Item> candidates = new ArrayList<>();
      candidates.add(ModItems.CRIMSON_HOUND.get());
      candidates.add(ModItems.PSEUDO_SPIRAL_SWORD.get());
      for (String id : copiedNoblePhantasmIds(vars)) {
         ResourceLocation key = ResourceLocation.tryParse(id);
         if (key != null && BuiltInRegistries.ITEM.containsKey(key)) {
            Item item = BuiltInRegistries.ITEM.get(key);
            if (item instanceof NoblePhantasmItem && !candidates.contains(item)) candidates.add(item);
         }
      }
      ItemStack payload = new ItemStack(candidates.get(player.getRandom().nextInt(candidates.size())));
      PlayerNoblePhantasmHelper.markUbwProjection(payload);
      ServantCardTransformManager.markGeneratedItem(payload, true, true);
      ItemStack bow = new ItemStack(ModItems.NAMELESS_BOW.get());
      PlayerNoblePhantasmHelper.markUbwProjection(bow);
      ServantCardTransformManager.markGeneratedItem(bow, true, true);
      player.setItemInHand(InteractionHand.MAIN_HAND, bow);
      player.setItemInHand(InteractionHand.OFF_HAND, payload);
   }

   public static int nextEmiyaCycleMode(TypeMoonWorldModVariables.PlayerVariables vars) {
      return Math.floorMod(vars.servant_card_action_mode + 1, 2);
   }

   public static boolean performUbwAction(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars, ServantCardSkillAction action) {
      if (vars.is_in_ubw) {
         ChantHandler.returnFromUBW(player, vars);
         player.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.ubw_released"), true);
         return true;
      }
      if (vars.is_chanting_ubw) {
         if (vars.ubw_chant_progress >= 3) {
            if (!ServantCardManaService.consume(player, vars, 110.0)) {
               player.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.not_enough_mp"), true);
               return false;
            }
            if (ChantHandler.activateServantCardUbwNow(player, vars)) {
               PlayerNoblePhantasmHelper.finishServantCardVoiceSession(
                  player,
                  "emiya_archer",
                  ModSounds.EMIYA_ARCHER_VOICE_UBW.get(),
                  ModSounds.EMIYA_ARCHER_VOICE_UBW_SHORT.get()
               );
               ServantCardTransformManager.setNoblePhantasmCooldown(player, vars, action.cooldownTicks());
               vars.syncPlayerVariables(player);
               spawnServantCardSwordRain(player, 36, 18.0);
               return true;
            }
            return false;
         }
         player.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.ubw_need_third_line"), true);
         return false;
      }
      if (!ServantCardUnlimitedMode.isEnabled(player) && vars.servant_card_np_cooldown > 0) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.cooldown", String.format(java.util.Locale.ROOT, "%.1f", vars.servant_card_np_cooldown / 20.0F)), true);
         return false;
      }
      if (!ServantCardManaService.consume(player, vars, 35.0)) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.not_enough_mp"), true);
         return false;
      }
      vars.has_unlimited_blade_works = true;
      vars.is_chanting_ubw = true;
      vars.ubw_chant_progress = 1;
      vars.ubw_chant_timer = 0;
      PlayerNoblePhantasmHelper.startServantCardVoiceSession(player, "emiya_archer", ModSounds.EMIYA_ARCHER_VOICE_UBW.get());
      vars.syncPlayerVariables(player);
      spawnServantCardUbwChantFallingSwords(player);
      player.displayClientMessage(Component.literal(EMIYA_UBW_FIRST_LINE), true);
      return true;
   }

   public static void spawnRhoAias(ServerPlayer player) {
      RhoAiasProjectionHelper.spawn(player);
   }

   public static void stopRhoAias(ServerPlayer player) {
      if (player.level() instanceof ServerLevel level) {
         for (RhoAiasEntity shield : level.getEntitiesOfClass(RhoAiasEntity.class, player.getBoundingBox().inflate(10.0), e -> e.isAlive() && e.getOwnerEntity() == player)) {
            shield.discard();
         }
      }
      if (player.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)) {
         player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
      }
   }

   public static boolean copyOpponentWeapon(ServerPlayer player) {
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      GilgameshEntity crossTarget = findCrossSlashCopyTarget(player, 40.0);
      if (crossTarget != null) {
         return copyGilgameshCrossSlash(player, crossTarget);
      }
      LivingEntity target = findCopyableWeaponTarget(player, 20.0, 1.6);
      if (target == null) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.no_trace_weapon"), true);
         return false;
      }
      ItemStack original = copyableHeldItem(target);
      if (original.isEmpty()) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.no_trace_weapon"), true);
         return false;
      }
      ItemStack traced = original.copy();
      traced.setCount(1);
      PlayerNoblePhantasmHelper.markUbwProjection(traced);
      PlayerNoblePhantasmHelper.markServantCardCopiedNoblePhantasm(traced);
      ServantCardTransformManager.markGeneratedItem(traced, true, true);
      ResourceLocation copiedId = BuiltInRegistries.ITEM.getKey(traced.getItem());
      if (copiedId != null) {
         LinkedHashSet<String> ids = copiedNoblePhantasmIds(vars);
         ids.add(copiedId.toString());
         vars.servant_card_emiya_copied_noble_phantasms = String.join(",", ids);
         vars.syncPlayerVariables(player);
      }
      if (traced.is(ModItems.BIZEN_NAGAMITSU.get())) {
         PlayerNoblePhantasmHelper.armTsubameAfterAnalysis(player, traced);
      } else if (traced.is(ModItems.TEMPLE_STONE_SWORD_AXE.get())) {
         PlayerNoblePhantasmHelper.armNineLives(traced);
         player.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.emiya_nine_lives_ready"), true);
      }
      player.setItemInHand(InteractionHand.OFF_HAND, traced);
      int strength = target.getAttributeValue(Attributes.ATTACK_DAMAGE) >= player.getAttributeValue(Attributes.ATTACK_DAMAGE) + 8.0 ? 1 : 0;
      int speed = target.getAttributeValue(Attributes.MOVEMENT_SPEED) > player.getAttributeValue(Attributes.MOVEMENT_SPEED) ? 1 : 0;
      player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 220, strength, false, true, true));
      player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 220, speed, false, true, true));
      if (target.getMaxHealth() > player.getMaxHealth()) {
         player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 220, 1, false, true, true));
      }
      return true;
   }

   public static GilgameshEntity findCrossSlashCopyTarget(ServerPlayer player, double range) {
      LivingEntity target = findLookTarget(player, range, 2.4);
      if (!(target instanceof GilgameshEntity gil)) {
         return null;
      }
      return gil.getPersistentData().getLong("GilgameshCrossSlashCopyUntil") >= player.level().getGameTime() ? gil : null;
   }

   private static boolean copyGilgameshCrossSlash(ServerPlayer player, GilgameshEntity gil) {
      if (!(player.level() instanceof ServerLevel level) || player.distanceTo(gil) > 40.0) {
         return false;
      }
      Vec3 direction = gil.position().add(0.0, gil.getBbHeight() * 0.5, 0.0).subtract(player.position().add(0.0, player.getBbHeight() * 0.5, 0.0));
      if (direction.lengthSqr() < 1.0E-4) {
         direction = player.getLookAngle();
      }
      direction = direction.normalize();
      GilgameshCrossSlashEntity.spawnPair(level, player, direction, gil, player);
      for (GilgameshCrossSlashEntity original : level.getEntitiesOfClass(GilgameshCrossSlashEntity.class, gil.getBoundingBox().inflate(420.0), e -> e.isAlive() && e.isOwnedBy(gil))) {
         original.addImmuneEntity(player);
      }
      return true;
   }

   public static LivingEntity findCopyableWeaponTarget(ServerPlayer player, double range, double inflate) {
      LivingEntity target = findLookTarget(player, range, inflate);
      if (target == null) {
         return null;
      }
      return copyableHeldItem(target).isEmpty() ? null : target;
   }

   private static ItemStack copyableHeldItem(LivingEntity target) {
      ItemStack mainHand = target.getMainHandItem();
      if (isTypeMoonWorldItem(mainHand)) {
         return mainHand;
      }
      ItemStack offHand = target.getOffhandItem();
      return isTypeMoonWorldItem(offHand) ? offHand : ItemStack.EMPTY;
   }

   private static boolean isTypeMoonWorldItem(ItemStack stack) {
      if (stack == null || stack.isEmpty()) {
         return false;
      }
      if (MagicStructuralAnalysis.isProjectionBanned(stack)) {
         return false;
      }
      ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
      return key != null && TYPE_MOON_WORLD.MOD_ID.equals(key.getNamespace());
   }

   private static LinkedHashSet<String> copiedNoblePhantasmIds(TypeMoonWorldModVariables.PlayerVariables vars) {
      LinkedHashSet<String> result = new LinkedHashSet<>();
      String raw = vars.servant_card_emiya_copied_noble_phantasms == null ? "" : vars.servant_card_emiya_copied_noble_phantasms;
      if (!raw.isBlank()) {
         for (String id : raw.split(",")) if (!id.isBlank()) result.add(id.trim());
      }
      return result;
   }

   public static void tickEmiyaContinuousProjection(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      if (!"emiya_archer".equals(vars.servant_card_id)) {
         clearEmiyaProjectionRounds(player);
         return;
      }
      tickSphericalProjection(player);
      CompoundTag data = player.getPersistentData();
      data.remove(EMIYA_LAYERED_OLD_UNTIL);
      int rounds = data.getInt(EMIYA_LAYERED_ROUNDS);
      if (rounds <= 0) {
         clearEmiyaLayeredProjection(player);
         return;
      }
      int nextTick = data.getInt(EMIYA_LAYERED_NEXT_TICK);
      if (player.tickCount < nextTick) {
         return;
      }
      spawnLayeredProjectionVolley(player, EMIYA_LAYERED_SWORDS_PER_ROUND);
      rounds--;
      if (rounds <= 0) {
         clearEmiyaLayeredProjection(player);
      } else {
         data.putInt(EMIYA_LAYERED_ROUNDS, rounds);
         data.putInt(EMIYA_LAYERED_NEXT_TICK, player.tickCount + EMIYA_LAYERED_ROUND_INTERVAL);
      }
   }

   public static void clearEmiyaLayeredProjection(ServerPlayer player) {
      CompoundTag data = player.getPersistentData();
      data.remove(EMIYA_LAYERED_OLD_UNTIL);
      data.remove(EMIYA_LAYERED_ROUNDS);
      data.remove(EMIYA_LAYERED_NEXT_TICK);
   }

   private static void clearEmiyaProjectionRounds(ServerPlayer player) {
      clearEmiyaLayeredProjection(player);
      CompoundTag data = player.getPersistentData();
      data.remove(EMIYA_SPHERICAL_ROUNDS);
      data.remove(EMIYA_SPHERICAL_NEXT_TICK);
      data.remove(EMIYA_SPHERICAL_TARGET);
   }

   public static void clear(ServerPlayer player) {
      clearEmiyaProjectionRounds(player);
      CompoundTag data = player.getPersistentData();
      data.remove(EMIYA_AUTO_COUNTER_UNTIL);
      data.remove(EMIYA_UBW_NEXT_CRIMSON_HOUND);
      data.remove(EMIYA_COPIED_NP_PENDING_TOKEN);
      data.remove(EMIYA_COPIED_NP_PENDING_TICK);
   }

   @SubscribeEvent(priority = EventPriority.LOWEST)
   public static void onCopiedNoblePhantasmUse(PlayerInteractEvent.RightClickItem event) {
      if (!(event.getEntity() instanceof ServerPlayer player) || event.getLevel().isClientSide()
         || ServantMasterCarryService.isCarryingMaster(player)) return;
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      ItemStack stack = event.getItemStack();
      if (!vars.servant_card_transformed || !"emiya_archer".equals(vars.servant_card_id)
         || !PlayerNoblePhantasmHelper.isServantCardCopiedNoblePhantasm(stack)) return;
      String token = PlayerNoblePhantasmHelper.servantCardCopiedNoblePhantasmToken(stack);
      if (token.isBlank()) return;
      CompoundTag data = player.getPersistentData();
      data.putString(EMIYA_COPIED_NP_PENDING_TOKEN, token);
      data.putLong(EMIYA_COPIED_NP_PENDING_TICK, player.level().getGameTime());
   }

   public static boolean performSphericalProjection(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level)) return false;
      LivingEntity target = findLookTarget(player, 40.0, 2.5);
      if (target == null) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.no_target"), true);
         return false;
      }
      CompoundTag data = player.getPersistentData();
      data.putInt(EMIYA_SPHERICAL_ROUNDS, EMIYA_SPHERICAL_TOTAL_ROUNDS);
      data.putInt(EMIYA_SPHERICAL_NEXT_TICK, player.tickCount);
      data.putUUID(EMIYA_SPHERICAL_TARGET, target.getUUID());
      level.playSound(null, target.blockPosition(), SoundEvents.ILLUSIONER_PREPARE_MIRROR, SoundSource.PLAYERS, 0.75F, 1.35F);
      return true;
   }

   private static void tickSphericalProjection(ServerPlayer player) {
      CompoundTag data = player.getPersistentData();
      int rounds = data.getInt(EMIYA_SPHERICAL_ROUNDS);
      if (rounds <= 0 || player.tickCount < data.getInt(EMIYA_SPHERICAL_NEXT_TICK)) return;
      if (!(player.level() instanceof ServerLevel level) || !data.hasUUID(EMIYA_SPHERICAL_TARGET)) {
         clearSphericalProjection(player);
         return;
      }
      Entity entity = level.getEntity(data.getUUID(EMIYA_SPHERICAL_TARGET));
      if (!(entity instanceof LivingEntity target) || !target.isAlive() || player.distanceToSqr(target) > 4096.0) {
         clearSphericalProjection(player);
         return;
      }
      spawnSphericalProjectionVolley(player, level, target);
      rounds--;
      if (rounds <= 0) {
         clearSphericalProjection(player);
      } else {
         data.putInt(EMIYA_SPHERICAL_ROUNDS, rounds);
         data.putInt(EMIYA_SPHERICAL_NEXT_TICK, player.tickCount + EMIYA_SPHERICAL_ROUND_INTERVAL);
      }
   }

   private static void clearSphericalProjection(ServerPlayer player) {
      CompoundTag data = player.getPersistentData();
      data.remove(EMIYA_SPHERICAL_ROUNDS);
      data.remove(EMIYA_SPHERICAL_NEXT_TICK);
      data.remove(EMIYA_SPHERICAL_TARGET);
   }

   private static void spawnSphericalProjectionVolley(ServerPlayer player, ServerLevel level, LivingEntity target) {
      Vec3 center = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
      double radius = Math.max(3.2, target.getBbWidth() * 2.0 + 2.2);
      for (int i = 0; i < 20; i++) {
         double phi = player.getRandom().nextDouble() * Math.PI * 2.0;
         double u = player.getRandom().nextDouble() * 2.0 - 1.0;
         double theta = Math.acos(u);
         Vec3 offset = new Vec3(radius * Math.sin(theta) * Math.cos(phi), radius * Math.sin(theta) * Math.sin(phi), radius * Math.cos(theta));
         Vec3 spawn = center.add(offset);
         SwordBarrelProjectileEntity sword = new SwordBarrelProjectileEntity(level, player, new ItemStack(Items.IRON_SWORD));
         sword.setPos(spawn.x, spawn.y, spawn.z);
         sword.setTargetEntity(target.getId());
         sword.setHover(15, center);
         sword.setOwner(player);
         sword.setMode1Tracking(true);
         Vec3 direction = center.subtract(spawn).normalize();
         sword.setXRot((float)Math.toDegrees(Math.asin(-direction.y)));
         sword.setYRot((float)Math.toDegrees(Math.atan2(-direction.x, direction.z)));
         level.addFreshEntity(sword);
         if ((i & 3) == 0) level.sendParticles(ParticleTypes.ENCHANT, spawn.x, spawn.y, spawn.z, 5, 0.14, 0.14, 0.14, 0.03);
      }
      level.playSound(null, target.blockPosition(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 0.55F, 1.2F + player.getRandom().nextFloat() * 0.25F);
   }

   public static void startAutoCounter(ServerPlayer player) {
      player.getPersistentData().putLong(EMIYA_AUTO_COUNTER_UNTIL, player.level().getGameTime() + EMIYA_AUTO_COUNTER_DURATION);
      if (player.level() instanceof ServerLevel level) {
         level.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 1.0, player.getZ(), 32, 1.2, 0.8, 1.2, 0.08);
         level.playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.8F, 1.4F);
      }
   }

   public static void tickEmiyaEquipmentAndCounter(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      enforceSingleAmmo(player, ModItems.PSEUDO_SPIRAL_SWORD.get());
      enforceSingleAmmo(player, ModItems.CRIMSON_HOUND.get());
      updateKanshouBakuyaMagicResistance(player);
      consumeUsedCopiedNoblePhantasm(player);
      long now = player.level().getGameTime();
      if (player.getPersistentData().getLong(EMIYA_AUTO_COUNTER_UNTIL) < now || !(player.level() instanceof ServerLevel level)) return;
      if (now % 4L == 0L) {
         int intercepted = 0;
         for (Projectile projectile : level.getEntitiesOfClass(Projectile.class, player.getBoundingBox().inflate(20.0),
            p -> p.isAlive() && p.getOwner() != player && !(p instanceof UBWProjectileEntity) && !(p instanceof UBWInterceptorSwordEntity)
               && !p.getPersistentData().getBoolean(EMIYA_AUTO_COUNTER_CLAIMED))) {
            Entity owner = projectile.getOwner();
            if (owner instanceof LivingEntity living && player.isAlliedTo(living)) continue;
            projectile.getPersistentData().putBoolean(EMIYA_AUTO_COUNTER_CLAIMED, true);
            Vec3 spawn = player.position().add((player.getRandom().nextDouble() - 0.5) * 3.0, 2.0 + player.getRandom().nextDouble() * 2.0,
               (player.getRandom().nextDouble() - 0.5) * 3.0);
            level.addFreshEntity(new UBWInterceptorSwordEntity(level, projectile, player.getUUID(), spawn));
            if (++intercepted >= 6) break;
         }
      }
      if (now % 10L == 0L) {
         level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(28.0), target -> isHostileTo(player, target))
            .stream().sorted(java.util.Comparator.comparingDouble(player::distanceToSqr)).limit(2)
            .forEach(target -> spawnSwordAtTarget(player, level, target));
      }
   }

   private static void consumeUsedCopiedNoblePhantasm(ServerPlayer player) {
      CompoundTag data = player.getPersistentData();
      String token = data.getString(EMIYA_COPIED_NP_PENDING_TOKEN);
      if (token.isBlank()) return;
      if (player.level().getGameTime() <= data.getLong(EMIYA_COPIED_NP_PENDING_TICK)) return;
      if (player.isUsingItem() && token.equals(PlayerNoblePhantasmHelper.servantCardCopiedNoblePhantasmToken(player.getUseItem()))) return;
      for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
         ItemStack stack = player.getInventory().getItem(i);
         if (token.equals(PlayerNoblePhantasmHelper.servantCardCopiedNoblePhantasmToken(stack))) {
            stack.shrink(1);
            break;
         }
      }
      data.remove(EMIYA_COPIED_NP_PENDING_TOKEN);
      data.remove(EMIYA_COPIED_NP_PENDING_TICK);
   }

   private static void enforceSingleAmmo(ServerPlayer player, Item item) {
      boolean found = false;
      for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
         ItemStack stack = player.getInventory().getItem(i);
         if (!stack.is(item)) continue;
         if (!found) {
            stack.setCount(1);
            found = true;
         } else {
            player.getInventory().setItem(i, ItemStack.EMPTY);
         }
      }
   }

   private static void updateKanshouBakuyaMagicResistance(ServerPlayer player) {
      ItemStack main = player.getMainHandItem();
      ItemStack off = player.getOffhandItem();
      boolean normalPair = main.is(ModItems.GAN_JIANG.get()) && off.is(ModItems.MO_YE.get())
         || main.is(ModItems.MO_YE.get()) && off.is(ModItems.GAN_JIANG.get());
      boolean overedgePair = main.is(ModItems.GAN_JIANG_OVEREDGE.get()) && off.is(ModItems.MO_YE_OVEREDGE.get())
         || main.is(ModItems.MO_YE_OVEREDGE.get()) && off.is(ModItems.GAN_JIANG_OVEREDGE.get());
      MagicResistanceRank rank = overedgePair ? MagicResistanceRank.B : normalPair ? MagicResistanceRank.C : MagicResistanceRank.D;
      float debuffResistance = rank == MagicResistanceRank.B ? 0.175F : rank == MagicResistanceRank.C ? 0.10F : 0.0F;
      MagicResistanceHelper.setMagicResistance(player, rank, MagicResistanceHelper.damageReductionForRank(rank), debuffResistance);
   }

   private static boolean isHostileTo(ServerPlayer player, LivingEntity target) {
      if (target == player || !target.isAlive() || target.isAlliedTo(player) || EntityUtils.isImmunePlayerTarget(target)) return false;
      if (target instanceof net.minecraft.world.entity.monster.Enemy) return true;
      if (target instanceof net.minecraft.world.entity.Mob mob && mob.getTarget() == player) return true;
      return target instanceof ServerPlayer other && !other.isCreative() && !other.isSpectator();
   }

   public static void spawnLayeredProjectionVolley(ServerPlayer player, int count) {
      if (!(player.level() instanceof ServerLevel level)) {
         return;
      }
      LivingEntity target = findLookTarget(player, 28.0, 2.0);
      Vec3 aim = target == null
         ? player.getEyePosition().add(player.getLookAngle().scale(28.0))
         : target.position().add(0.0, target.getBbHeight() * 0.55, 0.0);
      Vec3 forward = player.getLookAngle().normalize();
      Vec3 right = new Vec3(-forward.z, 0.0, forward.x).normalize();
      Vec3 center = player.getEyePosition().add(forward.scale(-1.5)).add(0.0, 2.35, 0.0);
      double width = Math.min(16.0, Math.max(8.0, count * 0.45));
      for (int i = 0; i < count; i++) {
         double row = i % 2 == 0 ? 0.0 : 1.0;
         double localRight = (i - (count - 1) * 0.5) * (width / Math.max(1, count - 1));
         double localUp = (player.getRandom().nextDouble() - 0.5) * 2.1 + row * 0.8;
         Vec3 spawn = center.add(right.scale(localRight)).add(0.0, localUp, 0.0).add(forward.scale(player.getRandom().nextDouble() * 1.2));
         UBWProjectileEntity projectile = new UBWProjectileEntity(level, player, new ItemStack(Items.IRON_SWORD));
         projectile.setPos(spawn.x, spawn.y, spawn.z);
         Vec3 spreadAim = aim.add((player.getRandom().nextDouble() - 0.5) * 1.6, (player.getRandom().nextDouble() - 0.5) * 0.8, (player.getRandom().nextDouble() - 0.5) * 1.6);
         Vec3 dir = spreadAim.subtract(projectile.position()).normalize();
         projectile.setDeltaMovement(dir.scale(2.55 + player.getRandom().nextDouble() * 0.35));
         projectile.setXRot((float)(-Math.toDegrees(Math.asin(dir.y))));
         projectile.setYRot((float)Math.toDegrees(Math.atan2(-dir.x, dir.z)));
         level.addFreshEntity(projectile);
         if (i % 4 == 0) {
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT, spawn.x, spawn.y, spawn.z, 5, 0.15, 0.15, 0.15, 0.03);
         }
      }
      level.playSound(null, player.blockPosition(), SoundEvents.ILLUSIONER_PREPARE_MIRROR, SoundSource.PLAYERS, 0.45F, 1.6F);
   }

   public static void spawnServantCardSwordRain(ServerPlayer player, int count, double radius) {
      if (!(player.level() instanceof ServerLevel level)) {
         return;
      }
      for (int i = 0; i < count; i++) {
         double angle = player.getRandom().nextDouble() * Math.PI * 2.0;
         double distance = player.getRandom().nextDouble() * radius;
         double x = player.getX() + Math.cos(angle) * distance;
         double z = player.getZ() + Math.sin(angle) * distance;
         double y = player.getY() + 14.0 + player.getRandom().nextDouble() * 8.0;
         UBWProjectileEntity projectile = new UBWProjectileEntity(level, player, new ItemStack(Items.IRON_SWORD));
         projectile.setPos(x, y, z);
         projectile.setDeltaMovement(0.0, -1.6 - player.getRandom().nextDouble() * 0.8, 0.0);
         projectile.setXRot(-90.0F);
         level.addFreshEntity(projectile);
      }
   }

   public static void spawnServantCardUbwChantFallingSwords(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level)) {
         return;
      }
      LivingEntity target = findLookTarget(player, 24.0, 2.4);
      Vec3 center = target != null && target.isAlive() ? target.position() : player.position().add(player.getLookAngle().scale(7.0));
      int count = target != null && target.isAlive() ? 6 : 3;
      for (int i = 0; i < count; i++) {
         double angle = level.random.nextDouble() * Math.PI * 2.0;
         double radius = 2.5 + level.random.nextDouble() * 10.0;
         double sx = center.x + Math.cos(angle) * radius;
         double sz = center.z + Math.sin(angle) * radius;
         double sy = center.y + 11.0 + level.random.nextDouble() * 7.0;
         UBWProjectileEntity sword = new UBWProjectileEntity(level, player, new ItemStack(Items.IRON_SWORD));
         sword.setStainUbwTerrainOnImpact(true);
         sword.setPos(sx, sy, sz);
         Vec3 aim = center.add((level.random.nextDouble() - 0.5) * 3.5, 0.0, (level.random.nextDouble() - 0.5) * 3.5);
         Vec3 dir = aim.subtract(sword.position()).normalize();
         sword.setDeltaMovement(dir.scale(2.35));
         sword.setXRot((float)(-Math.toDegrees(Math.asin(dir.y))));
         level.addFreshEntity(sword);
      }
      level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME, center.x, center.y + 0.35, center.z, 12, 2.0, 0.25, 2.0, 0.04);
   }

   public static void tickEmiyaUbwChantSwords(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      if (!"emiya_archer".equals(vars.servant_card_id) || !vars.is_chanting_ubw || vars.is_in_ubw) {
         return;
      }
      if (player.tickCount % 20 == 0) {
         spawnServantCardUbwChantFallingSwords(player);
      }
   }

   public static void tickEmiyaUbwSupport(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      if (!"emiya_archer".equals(vars.servant_card_id) || !vars.is_in_ubw || !(player.level() instanceof ServerLevel level)) {
         return;
      }
      long now = level.getGameTime();
      CompoundTag data = player.getPersistentData();
      if (!data.contains(EMIYA_UBW_NEXT_CRIMSON_HOUND)) {
         data.putLong(EMIYA_UBW_NEXT_CRIMSON_HOUND, now + EMIYA_UBW_CRIMSON_HOUND_INTERVAL);
      }
      if (player.tickCount % 10 == 0) {
         AABB area = player.getBoundingBox().inflate(38.0);
         for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area, e -> e.isAlive() && e != player && !EntityUtils.isImmunePlayerTarget(e))) {
            spawnSwordAtTarget(player, level, target);
         }
      }
      if (now >= player.getPersistentData().getLong(EMIYA_UBW_NEXT_CRIMSON_HOUND)) {
         launchServantCardUbwCrimsonHounds(player, level);
         data.putLong(EMIYA_UBW_NEXT_CRIMSON_HOUND, now + EMIYA_UBW_CRIMSON_HOUND_INTERVAL);
      }
      if (player.tickCount % 8 == 0) {
         AABB area = player.getBoundingBox().inflate(18.0);
         for (Projectile projectile : level.getEntitiesOfClass(Projectile.class, area, p -> p.isAlive() && p.getOwner() != player && !(p instanceof UBWProjectileEntity) && !(p instanceof UBWInterceptorSwordEntity))) {
            Entity owner = projectile.getOwner();
            if (owner instanceof LivingEntity living && player.isAlliedTo(living)) {
               continue;
            }
            Vec3 spawn = projectile.position().add(projectile.getDeltaMovement().scale(-2.0)).add(0.0, 1.0 + player.getRandom().nextDouble(), 0.0);
            level.addFreshEntity(new UBWInterceptorSwordEntity(level, projectile, player.getUUID(), spawn));
         }
      }
   }

   private static void launchServantCardUbwCrimsonHounds(ServerPlayer player, ServerLevel level) {
      List<LivingEntity> targets = activeServantCardUbwEnemies(player, level);
      if (targets.isEmpty()) {
         return;
      }
      for (LivingEntity target : targets) {
         spawnServantCardUbwCrimsonHound(player, level, target);
      }
      level.playSound(null, player.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.9F, 0.7F);
   }

   private static List<LivingEntity> activeServantCardUbwEnemies(ServerPlayer player, ServerLevel level) {
      List<LivingEntity> targets = new ArrayList<>();
      for (Entity entity : level.getEntities().getAll()) {
         if (entity instanceof LivingEntity living && isServantCardUbwEnemy(player, living)) {
            targets.add(living);
         }
      }
      return targets;
   }

   private static boolean isServantCardUbwEnemy(ServerPlayer player, LivingEntity target) {
      if (target == player || !target.isAlive() || target.isAlliedTo(player) || EntityUtils.isImmunePlayerTarget(target)) return false;
      if (target instanceof ServerPlayer other && (other.isCreative() || other.isSpectator())) return false;
      TypeMoonWorldModVariables.UBWReturnData data = target.getData(TypeMoonWorldModVariables.UBW_RETURN_DATA);
      return player.getUUID().equals(data.ownerUUID) || isHostileTo(player, target);
   }

   private static void spawnServantCardUbwCrimsonHound(ServerPlayer player, ServerLevel level, LivingEntity target) {
      double angle = player.getRandom().nextDouble() * Math.PI * 2.0;
      double radius = 7.0 + player.getRandom().nextDouble() * 9.0;
      Vec3 spawn = target.position().add(Math.cos(angle) * radius, 5.0 + player.getRandom().nextDouble() * 6.0, Math.sin(angle) * radius);
      CrimsonHoundProjectileEntity projectile = new CrimsonHoundProjectileEntity(level, player);
      projectile.setNoGravity(true);
      projectile.setPos(spawn.x, spawn.y, spawn.z);
      projectile.setTrackedTarget(target);
      Vec3 aim = target.position().add(0.0, target.getBbHeight() * 0.45, 0.0).subtract(spawn).normalize();
      projectile.setDeltaMovement(aim.scale(2.8));
      level.addFreshEntity(projectile);
      level.sendParticles(ParticleTypes.FLAME, spawn.x, spawn.y, spawn.z, 10, 0.18, 0.18, 0.18, 0.04);
   }

   public static void spawnSwordAtTarget(ServerPlayer player, ServerLevel level, LivingEntity target) {
      double angle = player.getRandom().nextDouble() * Math.PI * 2.0;
      double radius = 4.0 + player.getRandom().nextDouble() * 10.0;
      double sx = target.getX() + Math.cos(angle) * radius;
      double sz = target.getZ() + Math.sin(angle) * radius;
      double sy = target.getY() + 5.0 + player.getRandom().nextDouble() * 6.0;
      UBWProjectileEntity sword = new UBWProjectileEntity(level, player, new ItemStack(Items.IRON_SWORD));
      sword.setPos(sx, sy, sz);
      Vec3 aim = target.position().add(0.0, target.getBbHeight() * 0.55, 0.0);
      Vec3 dir = aim.subtract(sword.position()).normalize();
      sword.setDeltaMovement(dir.scale(2.75));
      level.addFreshEntity(sword);
   }

}
