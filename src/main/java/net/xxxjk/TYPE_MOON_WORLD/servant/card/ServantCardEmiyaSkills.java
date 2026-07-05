package net.xxxjk.TYPE_MOON_WORLD.servant.card;

import java.util.UUID;
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
import net.minecraft.world.entity.item.FallingBlockEntity;
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
import net.xxxjk.TYPE_MOON_WORLD.entity.RhoAiasEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.UBWInterceptorSwordEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.UBWProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.ChainsOfHeavenBindingEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.DragonfangSoldierEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.EnkiduEarthWeaponProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.MedeaMagicBoltEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.OdaMatchlockBulletEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.RedSkeletonHajunEntity;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.init.ModParticles;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.PlayerNoblePhantasmHelper;
import net.xxxjk.TYPE_MOON_WORLD.magic.unlimited_blade_works.ChantHandler;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantIdentityHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.data.ServantDataRegistry;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.CursedArmHassanCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.HeraclesGodHandHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.OdaNobunagaCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantDefinition;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantParams;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantTraitTag;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXServerEffects;

import static net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardSkillUtils.*;

public final class ServantCardEmiyaSkills {
   private static final String EMIYA_LAYERED_OLD_UNTIL = "ServantCardEmiyaLayeredProjectionUntil";
   private static final String EMIYA_LAYERED_ROUNDS = "ServantCardEmiyaLayeredProjectionRounds";
   private static final String EMIYA_LAYERED_NEXT_TICK = "ServantCardEmiyaLayeredProjectionNextTick";
   private static final int EMIYA_LAYERED_TOTAL_ROUNDS = 5;
   private static final int EMIYA_LAYERED_ROUND_INTERVAL = 10;
   private static final int EMIYA_LAYERED_SWORDS_PER_ROUND = 30;
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

   public static void startUbwChant(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      vars.has_unlimited_blade_works = true;
      vars.is_chanting_ubw = true;
      vars.ubw_chant_progress = 0;
      vars.ubw_chant_timer = 0;
      player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 0, false, true, true));
   }
   public static void equipPair(ServerPlayer player, net.minecraft.world.item.Item main, net.minecraft.world.item.Item off) {
      ItemStack mainStack = new ItemStack(main);
      ItemStack offStack = new ItemStack(off);
      PlayerNoblePhantasmHelper.markUbwProjection(mainStack);
      PlayerNoblePhantasmHelper.markUbwProjection(offStack);
      player.setItemInHand(InteractionHand.MAIN_HAND, mainStack);
      player.setItemInHand(InteractionHand.OFF_HAND, offStack);
   }

   public static void cycleLoadout(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      ItemStack payload = vars.servant_card_action_mode == 1
         ? new ItemStack(ModItems.CRIMSON_HOUND.get())
         : new ItemStack(ModItems.PSEUDO_SPIRAL_SWORD.get());
      PlayerNoblePhantasmHelper.markUbwProjection(payload);
      player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.NAMELESS_BOW.get()));
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
               vars.servant_card_np_cooldown = action.cooldownTicks();
               vars.syncPlayerVariables(player);
               spawnServantCardSwordRain(player, 36, 18.0);
               return true;
            }
            return false;
         }
         player.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.ubw_need_third_line"), true);
         return false;
      }
      if (vars.servant_card_np_cooldown > 0) {
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
      vars.syncPlayerVariables(player);
      spawnServantCardUbwChantFallingSwords(player);
      player.displayClientMessage(Component.literal("\u00A7bI am the bone of my sword."), true);
      return true;
   }

   public static void spawnRhoAias(ServerPlayer player) {
      if (player.level() instanceof ServerLevel level) {
         RhoAiasEntity shield = new RhoAiasEntity(level, player, findLookTarget(player, 24.0, 2.0));
         level.addFreshEntity(shield);
         level.playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0F, 1.15F);
      }
   }

   public static boolean copyOpponentWeapon(ServerPlayer player) {
      LivingEntity target = findCopyableWeaponTarget(player, 8.0, 1.6);
      if (target == null) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.no_trace_weapon"), true);
         return false;
      }
      ItemStack weapon = target.getMainHandItem();
      if (weapon.isEmpty()) {
         weapon = target.getOffhandItem();
      }
      ItemStack traced = weapon.copy();
      traced.setCount(1);
      PlayerNoblePhantasmHelper.markUbwProjection(traced);
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

   public static LivingEntity findCopyableWeaponTarget(ServerPlayer player, double range, double inflate) {
      LivingEntity target = findLookTarget(player, range, inflate);
      if (target == null) {
         return null;
      }
      return target.getMainHandItem().isEmpty() && target.getOffhandItem().isEmpty() ? null : target;
   }

   public static void tickEmiyaContinuousProjection(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      if (!"emiya_archer".equals(vars.servant_card_id)) {
         clearEmiyaLayeredProjection(player);
         return;
      }
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
      if (player.tickCount % 10 == 0) {
         AABB area = player.getBoundingBox().inflate(38.0);
         for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area, e -> e.isAlive() && e != player && !EntityUtils.isImmunePlayerTarget(e))) {
            spawnSwordAtTarget(player, level, target);
         }
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
