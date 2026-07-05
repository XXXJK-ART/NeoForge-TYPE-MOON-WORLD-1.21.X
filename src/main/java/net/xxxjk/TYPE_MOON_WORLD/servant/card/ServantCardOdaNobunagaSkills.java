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

public final class ServantCardOdaNobunagaSkills {
   private ServantCardOdaNobunagaSkills() {
   }

   public static void performOdaStrategy(ServerPlayer player) {
      buffNearby(player, 12.0, new MobEffectInstance(MobEffects.DAMAGE_BOOST, 260, 0, false, true, true));
      player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 260, 0, false, true, true));
      if (player.level() instanceof ServerLevel level) {
         for (ServerPlayer other : level.getEntitiesOfClass(ServerPlayer.class, player.getBoundingBox().inflate(12.0), p -> p != player && player.isAlliedTo(p))) {
            other.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 220, 0, false, true, true));
         }
         level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.0, player.getZ(), 24, 1.0, 0.5, 1.0, 0.03);
         level.playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 0.65F, 1.25F);
      }
   }

   public static void performOdaMaou(ServerPlayer player) {
      player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 300, 2, false, true, true));
      player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 400, 0, false, true, true));
      player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 220, 0, false, true, true));
      if (!(player.level() instanceof ServerLevel level)) {
         return;
      }
      VFXServerEffects.spawn(level, "servant_oda_maou", player, 160.0);
      for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(8.0, 3.0, 8.0), e -> e.isAlive() && e != player && !EntityUtils.isImmunePlayerTarget(e))) {
         OdaNobunagaCombatHelper.applyDivineDefenseBreak(player, living);
         living.setRemainingFireTicks(120);
         living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, hasTrait(living, ServantTraitTag.DIVINE) ? 2 : 0, false, true, true));
         living.invulnerableTime = 0;
         living.hurt(player.damageSources().playerAttack(player), hasTrait(living, ServantTraitTag.DIVINE) ? 24.0F : 10.0F);
         living.invulnerableTime = 0;
      }
      level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, player.getX(), player.getY() + 1.0, player.getZ(), 48, 2.1, 0.7, 2.1, 0.08);
      level.sendParticles(ParticleTypes.LARGE_SMOKE, player.getX(), player.getY() + 0.45, player.getZ(), 28, 1.8, 0.3, 1.8, 0.04);
      level.playSound(null, player.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 0.55F, 1.45F);
   }

   public static void performOdaMatchlock(ServerPlayer player, int count, float damage) {
      if (!(player.level() instanceof ServerLevel level)) {
         return;
      }
      Vec3 baseLook = player.getLookAngle().normalize();
      Vec3 right = new Vec3(-baseLook.z, 0.0, baseLook.x).normalize();
      for (int i = 0; i < count; i++) {
         double offset = count <= 1 ? 0.0 : (i - (count - 1) * 0.5) * 0.38;
         OdaMatchlockBulletEntity bullet = new OdaMatchlockBulletEntity(level, player, null, damage);
         Vec3 spawn = player.getEyePosition().add(right.scale(offset)).add(baseLook.scale(0.8));
         Vec3 dir = baseLook.add(right.scale((player.getRandom().nextDouble() - 0.5) * 0.04 * Math.max(1, count))).normalize();
         bullet.setPos(spawn.x, spawn.y, spawn.z);
         bullet.setDeltaMovement(dir.scale(3.1));
         level.addFreshEntity(bullet);
      }
      level.sendParticles(ParticleTypes.SMOKE, player.getX() + baseLook.x, player.getY() + 1.3, player.getZ() + baseLook.z, 12, 0.25, 0.12, 0.25, 0.04);
      level.sendParticles(ParticleTypes.FLAME, player.getX() + baseLook.x, player.getY() + 1.3, player.getZ() + baseLook.z, 6, 0.12, 0.08, 0.12, 0.03);
      level.playSound(null, player.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0F, 1.65F);
   }

   public static void performOdaFireBarrage(ServerPlayer player) {
      for (int wave = 0; wave < 3; wave++) {
         int delay = wave * 5;
         TYPE_MOON_WORLD.queueServerWork(delay, () -> {
            if (player.isAlive()) {
               performOdaMatchlock(player, 7, 9.0F);
            }
         });
      }
   }

   public static void performOdaAtsumori(ServerPlayer player) {
      if (player.level() instanceof ServerLevel level) {
         Vec3 dir = PlayerNoblePhantasmHelper.horizontalLook(player);
         Vec3 side = new Vec3(-dir.z, 0.0, dir.x).scale(player.isCrouching() ? -5.0 : 5.0);
         level.sendParticles(ParticleTypes.FLAME, player.getX(), player.getY() + 0.35, player.getZ(), 20, 0.35, 0.12, 0.35, 0.05);
         player.teleportTo(player.getX() + side.x, player.getY() + 0.1, player.getZ() + side.z);
         player.hurtMarked = true;
         level.sendParticles(ParticleTypes.ASH, player.getX(), player.getY() + 0.6, player.getZ(), 20, 0.45, 0.18, 0.45, 0.04);
      }
   }

   public static void performOdaAntiMystery(ServerPlayer player) {
      LivingEntity target = findLookTarget(player, 26.0, 1.8);
      if (target == null || !(player.level() instanceof ServerLevel level)) {
         return;
      }
      float damage = hasTrait(target, ServantTraitTag.DIVINE) ? 42.0F : 24.0F;
      OdaNobunagaCombatHelper.applyDivineDefenseBreak(player, target);
      target.invulnerableTime = 0;
      target.hurt(player.damageSources().playerAttack(player), damage);
      target.invulnerableTime = 0;
      spawnLineParticles(level, player.getEyePosition(), target.position().add(0.0, target.getBbHeight() * 0.55, 0.0), ParticleTypes.SOUL_FIRE_FLAME);
      level.sendParticles(ParticleTypes.FLASH, target.getX(), target.getY() + target.getBbHeight() * 0.55, target.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
   }

   public static void performOdaAshField(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level)) {
         return;
      }
      double radius = 7.0;
      for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(radius, 2.5, radius), e -> e.isAlive() && e != player && !EntityUtils.isImmunePlayerTarget(e))) {
         OdaNobunagaCombatHelper.applyDivineDefenseBreak(player, living);
         living.setRemainingFireTicks(100);
         living.invulnerableTime = 0;
         living.hurt(player.damageSources().playerAttack(player), hasTrait(living, ServantTraitTag.DIVINE) ? 15.0F : 8.0F);
         living.invulnerableTime = 0;
      }
      level.sendParticles(ParticleTypes.FLAME, player.getX(), player.getY() + 0.12, player.getZ(), 48, 3.6, 0.12, 3.6, 0.035);
      level.sendParticles(ParticleTypes.ASH, player.getX(), player.getY() + 0.4, player.getZ(), 52, 4.0, 0.2, 4.0, 0.025);
   }

   public static void performOdaHasebeRepel(ServerPlayer player) {
      LivingEntity target = findLookTarget(player, 7.0, 1.8);
      if (target == null || !(player.level() instanceof ServerLevel level)) {
         return;
      }
      Vec3 dir = target.position().subtract(player.position()).normalize();
      target.invulnerableTime = 0;
      target.hurt(player.damageSources().playerAttack(player), hasTrait(target, ServantTraitTag.DIVINE) ? 26.0F : 16.0F);
      target.invulnerableTime = 0;
      target.push(dir.x * 1.25, 0.22, dir.z * 1.25);
      target.hurtMarked = true;
      OdaNobunagaCombatHelper.applyDivineDefenseBreak(player, target);
      level.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY() + target.getBbHeight() * 0.55, target.getZ(), 3, 0.0, 0.0, 0.0, 0.0);
      level.sendParticles(ParticleTypes.FLAME, target.getX(), target.getY() + target.getBbHeight() * 0.55, target.getZ(), 12, 0.35, 0.18, 0.35, 0.05);
   }

   public static void performOdaThreeThousand(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level)) {
         return;
      }
      VFXServerEffects.spawn(level, "servant_oda_three_thousand", player, 160.0);
      for (int wave = 0; wave < 6; wave++) {
         int delay = wave * 4;
         TYPE_MOON_WORLD.queueServerWork(delay, () -> {
            if (player.isAlive()) {
               performOdaMatchlock(player, 12, 12.0F);
            }
         });
      }
   }

   public static void performOdaHajun(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level)) {
         return;
      }
      VFXServerEffects.spawn(level, "servant_oda_hajun", player, 192.0);
      RedSkeletonHajunEntity skeleton = new RedSkeletonHajunEntity(level, player, 260);
      skeleton.setPos(player.getX(), player.getY(), player.getZ());
      level.addFreshEntity(skeleton);
      for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(14.0, 5.0, 14.0), e -> e.isAlive() && e != player && !EntityUtils.isImmunePlayerTarget(e))) {
         OdaNobunagaCombatHelper.applyDivineDefenseBreak(player, living);
         living.addEffect(new MobEffectInstance(MobEffects.WITHER, 140, 1, false, true, true));
         living.invulnerableTime = 0;
         living.hurt(player.damageSources().playerAttack(player), hasTrait(living, ServantTraitTag.DIVINE) ? 60.0F : 34.0F);
         living.invulnerableTime = 0;
      }
      level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, player.getX(), player.getY() + 0.2, player.getZ(), 72, 4.0, 0.18, 4.0, 0.04);
   }

}

