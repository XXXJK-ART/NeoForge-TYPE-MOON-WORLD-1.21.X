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

public final class ServantCardEnkiduSkills {
   private ServantCardEnkiduSkills() {
   }

   public static void performEnkiduTransfiguration(ServerPlayer player) {
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      int[] points = parseTransfigurationPoints(vars);
      player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 260, Math.max(0, points[0] / 3 - 1), false, true, true));
      player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 260, Math.max(0, points[1] / 3 - 1), false, true, true));
      player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 260, Math.max(0, points[2] / 3 - 1), false, true, true));
      vars.servant_card_mana = Math.min(vars.servant_card_max_mana, vars.servant_card_mana + Math.max(0, points[3] * 3.0));
      if (points[4] >= 6) {
         player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 260, Math.max(0, points[4] / 4 - 1), false, true, true));
      }
      vars.syncPlayerVariables(player);
      if (player.level() instanceof ServerLevel level) {
         level.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 1.0, player.getZ(), 42, 0.75, 0.65, 0.75, 0.06);
         level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.0, player.getZ(), 22, 0.45, 0.4, 0.45, 0.04);
         level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.8F, 1.1F);
      }
   }

   public static void adjustTransfigurationPoint(ServerPlayer player, int stat, int delta) {
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (!vars.servant_card_transformed || !"enkidu".equals(vars.servant_card_id) || stat < 0 || stat >= 5 || delta == 0) {
         return;
      }
      int[] points = parseTransfigurationPoints(vars);
      int next = Mth.clamp(points[stat] + (delta > 0 ? 1 : -1), 0, 30);
      if (next == points[stat]) {
         return;
      }
      int total = 0;
      for (int i = 0; i < points.length; i++) {
         total += i == stat ? next : points[i];
      }
      if (total > 30) {
         return;
      }
      points[stat] = next;
      vars.servant_card_enkidu_transfiguration_points = serializeTransfigurationPoints(points);
      vars.syncPlayerVariables(player);
   }

   public static int[] parseTransfigurationPoints(TypeMoonWorldModVariables.PlayerVariables vars) {
      int[] points = new int[]{6, 6, 6, 6, 6};
      String raw = vars.servant_card_enkidu_transfiguration_points == null ? "" : vars.servant_card_enkidu_transfiguration_points;
      String[] parts = raw.split(",");
      for (int i = 0; i < points.length && i < parts.length; i++) {
         try {
            points[i] = Mth.clamp(Integer.parseInt(parts[i]), 0, 30);
         } catch (NumberFormatException ignored) {
            points[i] = 6;
         }
      }
      return points;
   }

   private static String serializeTransfigurationPoints(int[] points) {
      return points[0] + "," + points[1] + "," + points[2] + "," + points[3] + "," + points[4];
   }

   public static void performEnkiduPerfectForm(ServerPlayer player) {
      ServantCardSkillUtils.clearHarmfulEffects(player);
      player.heal(Math.max(18.0F, player.getMaxHealth() * 0.28F));
      player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 220, 2, false, true, true));
      player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 140, 1, false, true, true));
      player.clearFire();
      if (player.level() instanceof ServerLevel level) {
         VFXServerEffects.spawn(level, "servant_enkidu_perfect_form", player, 128.0);
         level.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 1.0, player.getZ(), 55, 0.9, 0.75, 0.9, 0.08);
         level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + 0.9, player.getZ(), 24, 0.45, 0.55, 0.45, 0.04);
         level.playSound(null, player.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.85F, 1.45F);
      }
   }

   public static void performEnkiduBulwark(ServerPlayer player) {
      player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 220, 2, false, true, true));
      player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 220, 1, false, true, true));
      if (!(player.level() instanceof ServerLevel level)) {
         return;
      }
      double radius = 7.0;
      for (Projectile projectile : level.getEntitiesOfClass(Projectile.class, player.getBoundingBox().inflate(radius), p -> p.isAlive() && p.getOwner() != player)) {
         Vec3 away = projectile.position().subtract(player.position());
         Vec3 dir = away.lengthSqr() < 0.01 ? player.getLookAngle().scale(-1.0) : away.normalize();
         projectile.setDeltaMovement(dir.scale(1.65).add(0.0, 0.12, 0.0));
         projectile.hurtMarked = true;
      }
      Vec3 forward = PlayerNoblePhantasmHelper.horizontalLook(player);
      Vec3 right = new Vec3(-forward.z, 0.0, forward.x);
      for (int i = -3; i <= 3; i++) {
         Vec3 pos = player.position().add(forward.scale(2.2)).add(right.scale(i * 0.75));
         EnkiduEarthWeaponProjectileEntity weapon = EnkiduEarthWeaponProjectileEntity.weapon(level, player, new ItemStack(Items.SHIELD), 8.0F, null, 0.0F, true);
         weapon.setPos(pos.x, pos.y - 0.2, pos.z);
         weapon.setDeltaMovement(0.0, 0.9, 0.0);
         level.addFreshEntity(weapon);
      }
      level.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX() + forward.x * 2.0, player.getY() + 0.8, player.getZ() + forward.z * 2.0, 30, 1.6, 0.45, 1.6, 0.04);
      level.playSound(null, player.blockPosition(), SoundEvents.ROOTED_DIRT_PLACE, SoundSource.PLAYERS, 1.0F, 0.75F);
   }

   public static void performEnkiduDetection(ServerPlayer player) {
      if (player.level() instanceof ServerLevel level) {
         for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(24.0), e -> e.isAlive() && e != player && !EntityUtils.isImmunePlayerTarget(e))) {
            living.addEffect(new MobEffectInstance(MobEffects.GLOWING, 180, 0, false, true, true));
         }
         level.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 1.0, player.getZ(), 32, 1.2, 0.55, 1.2, 0.04);
      }
   }

   public static void performEnkiduChains(ServerPlayer player) {
      LivingEntity target = findLookTarget(player, 26.0, 2.0);
      if (target == null || !(player.level() instanceof ServerLevel level)) {
         return;
      }
      boolean divine = hasTrait(target, ServantTraitTag.DIVINE) || hasTrait(target, ServantTraitTag.CELESTIAL);
      ChainsOfHeavenBindingEntity bind = new ChainsOfHeavenBindingEntity(level, player, target, divine ? 160 : 90, divine);
      level.addFreshEntity(bind);
      target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, divine ? 160 : 90, divine ? 6 : 3, false, true, true));
      target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, divine ? 160 : 90, divine ? 2 : 0, false, true, true));
      level.playSound(null, target.blockPosition(), SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 1.1F, divine ? 0.65F : 0.9F);
   }

   public static void performEnkiduAgeOfBabylon(ServerPlayer player, int count, float damage, boolean mega) {
      if (!(player.level() instanceof ServerLevel level)) {
         return;
      }
      LivingEntity target = findLookTarget(player, mega ? 38.0 : 28.0, 2.2);
      Vec3 aim = target == null ? player.getEyePosition().add(player.getLookAngle().scale(30.0)) : target.position().add(0.0, target.getBbHeight() * 0.55, 0.0);
      Vec3 forward = player.getLookAngle().normalize();
      Vec3 right = new Vec3(-forward.z, 0.0, forward.x).normalize();
      ItemStack[] stacks = {new ItemStack(Items.IRON_SWORD), new ItemStack(Items.IRON_AXE), new ItemStack(Items.TRIDENT), new ItemStack(Items.DIAMOND_SWORD)};
      for (int i = 0; i < count; i++) {
         Vec3 spawn = player.getEyePosition()
            .add(forward.scale(-1.0 - player.getRandom().nextDouble() * 2.5))
            .add(right.scale((i - (count - 1) * 0.5) * (mega ? 0.72 : 0.45)))
            .add(0.0, 1.2 + (i % 4) * 0.45, 0.0);
         EnkiduEarthWeaponProjectileEntity weapon = EnkiduEarthWeaponProjectileEntity.weapon(level, player, stacks[i % stacks.length], damage, target, mega ? 0.12F : 0.07F, false);
         weapon.setPos(spawn.x, spawn.y, spawn.z);
         Vec3 dir = aim.add((player.getRandom().nextDouble() - 0.5) * 1.6, (player.getRandom().nextDouble() - 0.5) * 0.8, (player.getRandom().nextDouble() - 0.5) * 1.6).subtract(spawn).normalize();
         weapon.setDeltaMovement(dir.scale(mega ? 2.65 : 2.25));
         level.addFreshEntity(weapon);
      }
      level.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 1.3, player.getZ(), mega ? 46 : 22, 0.9, 0.65, 0.9, 0.06);
      level.playSound(null, player.blockPosition(), SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.PLAYERS, 0.9F, 1.55F);
   }

   public static void performEnkiduEarthWedge(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level)) {
         return;
      }
      Vec3 look = PlayerNoblePhantasmHelper.horizontalLook(player);
      for (double dist = 2.0; dist <= 10.0; dist += 1.4) {
         Vec3 pos = player.position().add(look.scale(dist));
         EnkiduEarthWeaponProjectileEntity weapon = EnkiduEarthWeaponProjectileEntity.weapon(level, player, new ItemStack(Items.TRIDENT), 18.0F, null, 0.0F, true);
         weapon.setPos(pos.x, pos.y - 0.6, pos.z);
         weapon.setDeltaMovement(0.0, 1.25, 0.0);
         level.addFreshEntity(weapon);
      }
      hitForwardArc(player, look, 10.0, 18.0F);
   }

   public static void performEnkiduStardust(ServerPlayer player) {
      if (player.level() instanceof ServerLevel level) {
         Vec3 dir = PlayerNoblePhantasmHelper.horizontalLook(player);
         player.setDeltaMovement(player.getDeltaMovement().add(dir.x * 2.2, 0.35, dir.z * 2.2));
         player.hurtMarked = true;
         level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 0.8, player.getZ(), 24, 0.35, 0.3, 0.35, 0.08);
      }
   }

   public static void performEnkiduEnumaElish(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level)) {
         return;
      }
      VFXServerEffects.spawn(level, "servant_enkidu_enuma_elish", player, 192.0);
      CompoundTag data = player.getPersistentData();
      data.putBoolean("ServantCardEnkiduEnumaFlightActive", true);
      data.putBoolean("ServantCardEnkiduEnumaReleased", false);
      for (int tick = 1; tick <= 120; tick++) {
         final int index = tick;
         TYPE_MOON_WORLD.queueServerWork(index, () -> tickEnumaElishFlight(player, index >= 120));
      }
   }

   private static void tickEnumaElishFlight(ServerPlayer player, boolean timeout) {
      CompoundTag data = player.getPersistentData();
      if (!player.isAlive() || !data.getBoolean("ServantCardEnkiduEnumaFlightActive") || data.getBoolean("ServantCardEnkiduEnumaReleased")) {
         return;
      }
      if (!(player.level() instanceof ServerLevel level)) {
         return;
      }
      Vec3 dir = player.getLookAngle();
      if (dir.lengthSqr() < 1.0E-4) {
         dir = new Vec3(0.0, 0.0, 1.0);
      }
      player.setDeltaMovement(dir.normalize().scale(2.35));
      player.hurtMarked = true;
      level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 0.8, player.getZ(), 10, 0.25, 0.25, 0.25, 0.04);
      LivingEntity hit = findFlightHit(player, level);
      if (timeout || player.horizontalCollision || hit != null) {
         data.putBoolean("ServantCardEnkiduEnumaReleased", true);
         data.putBoolean("ServantCardEnkiduEnumaFlightActive", false);
         releaseEnumaElishImpact(player, level, hit);
      }
   }

   private static LivingEntity findFlightHit(ServerPlayer player, ServerLevel level) {
      for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(1.6), e -> e.isAlive() && e != player && !EntityUtils.isImmunePlayerTarget(e))) {
         return living;
      }
      return null;
   }

   private static void releaseEnumaElishImpact(ServerPlayer player, ServerLevel level, LivingEntity directHit) {
      Vec3 center = directHit == null ? player.position() : directHit.position();
      VFXServerEffects.spawn(level, "servant_enkidu_enuma_elish", center, 192.0);
      performEnkiduAgeOfBabylon(player, 40, 24.0F, true);
      for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(8.0), e -> e.isAlive() && e != player && !EntityUtils.isImmunePlayerTarget(e))) {
         living.invulnerableTime = 0;
         living.hurt(player.damageSources().playerAttack(player), hasTrait(living, ServantTraitTag.DIVINE) ? 110.0F : 70.0F);
         living.invulnerableTime = 0;
      }
      level.sendParticles(ParticleTypes.FLASH, center.x, center.y + 0.8, center.z, 4, 0.0, 0.0, 0.0, 0.0);
   }

}

