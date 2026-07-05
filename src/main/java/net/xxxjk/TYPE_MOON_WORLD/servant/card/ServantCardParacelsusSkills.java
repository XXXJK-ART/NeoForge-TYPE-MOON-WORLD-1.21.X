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

public final class ServantCardParacelsusSkills {
   private ServantCardParacelsusSkills() {
   }

   public static void cycleElement(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      vars.servant_card_action_mode = (vars.servant_card_action_mode + 1) % 4;
      player.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.element_mode", vars.servant_card_action_mode + 1), true);
   }

   public static void performParacelsusChant(ServerPlayer player) {
      player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 260, 1, false, true, true));
      player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 180, 0, false, true, true));
      player.getPersistentData().putInt("ServantCardParacelsusChantUntil", player.tickCount + 260);
      if (player.level() instanceof ServerLevel level) {
         level.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 1.0, player.getZ(), 48, 0.9, 0.65, 0.9, 0.08);
         level.sendParticles(ParticleTypes.WITCH, player.getX(), player.getY() + 1.1, player.getZ(), 18, 0.5, 0.4, 0.5, 0.03);
         level.playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.9F, 1.55F);
      }
   }

   public static void performParacelsusSpirit(ServerPlayer player) {
      player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 180, 0, false, true, true));
      player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0, false, true, true));
      if (player.level() instanceof ServerLevel level) {
         level.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 1.0, player.getZ(), 38, 0.7, 0.45, 0.7, 0.06);
         level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.85F, 1.2F);
      }
   }

   public static void performParacelsusStone(ServerPlayer player) {
      ServantCardSkillUtils.clearHarmfulEffects(player);
      player.heal(Math.max(12.0F, player.getMaxHealth() * 0.18F));
      player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 180, 2, false, true, true));
      player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 240, 2, false, true, true));
      player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 140, 1, false, true, true));
      if (!(player.level() instanceof ServerLevel level)) {
         return;
      }
      for (ServerPlayer other : level.getEntitiesOfClass(ServerPlayer.class, player.getBoundingBox().inflate(8.0), p -> p != player && player.isAlliedTo(p))) {
         other.heal(8.0F);
         other.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 120, 1, false, true, true));
      }
      VFXServerEffects.spawn(level, "servant_paracelsus_philosopher_stone", player, 128.0);
      level.sendParticles(ParticleTypes.HEART, player.getX(), player.getY() + 1.2, player.getZ(), 12, 0.55, 0.45, 0.55, 0.02);
      level.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 1.0, player.getZ(), 46, 0.85, 0.65, 0.85, 0.07);
      level.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.8F, 1.4F);
   }

   public static void performParacelsusElement(ServerPlayer player, String element) {
      if (!(player.level() instanceof ServerLevel level)) {
         return;
      }
      MedeaMagicBoltEntity bolt = new MedeaMagicBoltEntity(level, player);
      bolt.setMagicDamage(switch (element) {
         case "fire" -> 24.0F;
         case "earth" -> 22.0F;
         case "water" -> 18.0F;
         default -> 20.0F;
      });
      bolt.setMode(switch (element) {
         case "fire" -> MedeaMagicBoltEntity.Mode.FIRE_BOLT;
         case "water" -> MedeaMagicBoltEntity.Mode.FROST_BOLT;
         default -> MedeaMagicBoltEntity.Mode.SUPER_BOLT;
      });
      Vec3 spawn = player.getEyePosition().add(player.getLookAngle().scale(0.7));
      bolt.setPos(spawn.x, spawn.y, spawn.z);
      bolt.setDeltaMovement(player.getLookAngle().normalize().scale(2.0));
      level.addFreshEntity(bolt);
      if ("wind".equals(element)) {
         hitForwardArc(player, PlayerNoblePhantasmHelper.horizontalLook(player), 7.0, 16.0F);
      }
   }

   public static void performParacelsusMixedElement(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level)) {
         return;
      }
      Vec3 center = player.position().add(player.getLookAngle().normalize().scale(7.0));
      for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(5.0, 3.0, 5.0), e -> e.isAlive() && e != player && !EntityUtils.isImmunePlayerTarget(e))) {
         living.setRemainingFireTicks(80);
         living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 2, false, true, true));
         living.invulnerableTime = 0;
         living.hurt(player.damageSources().playerAttack(player), 38.0F);
         living.invulnerableTime = 0;
      }
      level.sendParticles(ParticleTypes.ENCHANT, center.x, center.y + 0.7, center.z, 42, 2.5, 1.2, 2.5, 0.09);
      level.sendParticles(ParticleTypes.FLAME, center.x, center.y + 0.3, center.z, 18, 1.8, 0.5, 1.8, 0.06);
      level.sendParticles(ParticleTypes.SPLASH, center.x, center.y + 0.3, center.z, 18, 1.8, 0.5, 1.8, 0.04);
      level.playSound(null, BlockPos.containing(center), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.75F, 1.35F);
   }

}

