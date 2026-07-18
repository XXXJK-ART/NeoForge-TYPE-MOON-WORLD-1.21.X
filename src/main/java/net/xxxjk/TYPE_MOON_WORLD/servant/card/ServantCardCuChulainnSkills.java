package net.xxxjk.TYPE_MOON_WORLD.servant.card;

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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.init.ModParticles;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.PlayerNoblePhantasmHelper;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;

public final class ServantCardCuChulainnSkills {
   private static final ResourceLocation CU_TIWAZ_ATTACK_ID = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "servant_card_cu_tiwaz_attack");
   private static final ResourceLocation CU_TIWAZ_SPEED_ID = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "servant_card_cu_tiwaz_speed");
   private static final ResourceLocation CU_TIWAZ_ARMOR_ID = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "servant_card_cu_tiwaz_armor");
   public static final String CU_RUNE_ALGIZ_SHIELD_TAG = "ServantCardCuAlgizShield";
   private static final String CU_RUNE_TIWAZ_UNTIL_TAG = "ServantCardCuTiwazUntil";
   private static final String CU_RUNE_BERKANA_UNTIL_TAG = "ServantCardCuBerkanaUntil";
   private static final String CU_RUNE_BERKANA_NEXT_HEAL_TAG = "ServantCardCuBerkanaNextHeal";
   private static final String CU_RECAST_USED_TAG = "ServantCardCuRecastUsed";
   private static final double CU_RECAST_MP_COST = 35.0;

   private ServantCardCuChulainnSkills() {
   }

   public static void tick(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      if (!"cu_chulainn".equals(vars.servant_card_id)) {
         clear(player);
         return;
      }
      CompoundTag data = player.getPersistentData();
      long now = player.level().getGameTime();
      long tiwazUntil = data.getLong(CU_RUNE_TIWAZ_UNTIL_TAG);
      if (tiwazUntil > 0L && now >= tiwazUntil) {
         clearCuTiwaz(player);
      }
      long berkanaUntil = data.getLong(CU_RUNE_BERKANA_UNTIL_TAG);
      if (berkanaUntil > 0L) {
         if (now >= berkanaUntil) {
            data.remove(CU_RUNE_BERKANA_UNTIL_TAG);
            data.remove(CU_RUNE_BERKANA_NEXT_HEAL_TAG);
         } else if (now >= data.getLong(CU_RUNE_BERKANA_NEXT_HEAL_TAG)) {
            player.heal(1.0F);
            data.putLong(CU_RUNE_BERKANA_NEXT_HEAL_TAG, now + 10L);
         }
      }
      if (player.getHealth() <= player.getMaxHealth() * 0.20F && !data.getBoolean(CU_RECAST_USED_TAG)) {
         triggerCuRecastPassive(player, vars);
      } else if (player.getHealth() >= player.getMaxHealth() * 0.90F) {
         data.putBoolean(CU_RECAST_USED_TAG, false);
      }
      if (player.tickCount % 12 == 0 && player.level() instanceof ServerLevel level) {
         if (data.getFloat(CU_RUNE_ALGIZ_SHIELD_TAG) > 0.0F) {
            level.sendParticles(ParticleTypes.WAX_ON, player.getX(), player.getY() + player.getBbHeight() * 0.55, player.getZ(), 2, 0.26, 0.34, 0.26, 0.01);
         }
         if (data.getLong(CU_RUNE_TIWAZ_UNTIL_TAG) > now) {
            level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + player.getBbHeight() * 0.62, player.getZ(), 2, 0.28, 0.4, 0.28, 0.01);
         }
      }
   }

   private static void triggerCuRecastPassive(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      if (!ServantCardManaService.consume(player, vars, CU_RECAST_MP_COST)) {
         return;
      }
      CompoundTag data = player.getPersistentData();
      data.putBoolean(CU_RECAST_USED_TAG, true);
      ServantCardSkillUtils.clearHarmfulEffects(player);
      player.heal(Math.max(8.0F, player.getMaxHealth() * 0.35F));
      player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 160, 1, false, true, true));
      player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 1, false, true, true));
      player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 120, 1, false, true, true));
      if (player.level() instanceof ServerLevel level) {
         spawnRuneParticles(level, player, ModParticles.TIWAZ_RUNE.get(), 18, 0.45, 0.02);
         level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + player.getBbHeight() * 0.5, player.getZ(), 20, 0.4, 0.6, 0.4, 0.05);
         level.playSound(null, player.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.85F, 1.2F);
      }
      vars.syncPlayerVariables(player);
   }

   public static void clear(ServerPlayer player) {
      CompoundTag data = player.getPersistentData();
      data.remove(CU_RUNE_ALGIZ_SHIELD_TAG);
      data.remove(CU_RUNE_TIWAZ_UNTIL_TAG);
      data.remove(CU_RUNE_BERKANA_UNTIL_TAG);
      data.remove(CU_RUNE_BERKANA_NEXT_HEAL_TAG);
      data.remove(CU_RECAST_USED_TAG);
      clearCuTiwaz(player);
   }

   public static void performAnsuzRune(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level)) {
         return;
      }
      LivingEntity target = ServantCardSkillUtils.findLookTarget(player, 18.0, 1.6);
      Vec3 start = player.position().add(0.0, player.getBbHeight() * 0.65, 0.0);
      Vec3 impact = target == null
         ? start.add(player.getLookAngle().normalize().scale(8.0))
         : target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
      spawnRuneParticles(level, player, ModParticles.ANSUZ_RUNE.get(), 18, 0.45, 0.02);
      spawnAnsuzTrace(level, start, impact);
      TYPE_MOON_WORLD.queueServerWork(6, () -> {
         if (!(player.level() instanceof ServerLevel delayedLevel) || !player.isAlive()) {
            return;
         }
         AABB fireBox = new AABB(impact, impact).inflate(target == null ? 2.2 : 2.8);
         for (LivingEntity living : delayedLevel.getEntitiesOfClass(
            LivingEntity.class,
            fireBox,
            e -> e != player && e.isAlive() && !EntityUtils.isImmunePlayerTarget(e)
         )) {
            living.invulnerableTime = 0;
            living.hurt(player.damageSources().playerAttack(player), target == null ? 42.0F : 58.0F);
            living.invulnerableTime = 0;
            living.setRemainingFireTicks(Math.max(living.getRemainingFireTicks(), 100));
         }
         delayedLevel.sendParticles(ParticleTypes.FLAME, impact.x, impact.y, impact.z, 28, 0.85, 0.45, 0.85, 0.055);
         delayedLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, impact.x, impact.y + 0.15, impact.z, 14, 0.55, 0.35, 0.55, 0.04);
         delayedLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, impact.x, impact.y, impact.z, 16, 0.7, 0.35, 0.7, 0.03);
         delayedLevel.playSound(null, impact.x, impact.y, impact.z, SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0F, 0.88F);
      });
   }

   public static void performLaguzRune(ServerPlayer player) {
      player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 220, 2, false, true, true));
      player.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 220, 0, false, true, true));
      player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 400, 0, false, true, true));
      player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 160, 0, false, true, true));
      if (player.level() instanceof ServerLevel level) {
         spawnRuneParticles(level, player, ModParticles.LAGUZ_RUNE.get(), 16, 0.65, 0.01);
         level.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + player.getBbHeight() * 0.6, player.getZ(), 20, 0.6, 0.8, 0.6, 0.04);
         level.playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.75F, 1.35F);
      }
   }

   public static void performTiwazRune(ServerPlayer player) {
      long until = player.level().getGameTime() + 300L;
      player.getPersistentData().putLong(CU_RUNE_TIWAZ_UNTIL_TAG, until);
      ServantCardSkillUtils.addOrReplaceMultiplied(player.getAttribute(Attributes.ATTACK_DAMAGE), CU_TIWAZ_ATTACK_ID, 0.30);
      ServantCardSkillUtils.addOrReplaceMultiplied(player.getAttribute(Attributes.MOVEMENT_SPEED), CU_TIWAZ_SPEED_ID, 0.22);
      ServantCardSkillUtils.addOrReplaceMultiplied(player.getAttribute(Attributes.ARMOR), CU_TIWAZ_ARMOR_ID, 0.20);
      if (player.level() instanceof ServerLevel level) {
         spawnRuneParticles(level, player, ModParticles.TIWAZ_RUNE.get(), 18, 0.45, 0.02);
         level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + player.getBbHeight() * 0.6, player.getZ(), 16, 0.45, 0.7, 0.45, 0.03);
         level.playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.8F, 0.9F);
      }
   }

   public static void performAlgizRune(ServerPlayer player) {
      player.getPersistentData().putFloat(CU_RUNE_ALGIZ_SHIELD_TAG, 100.0F);
      if (player.level() instanceof ServerLevel level) {
         spawnRuneParticles(level, player, ModParticles.ALGIZ_RUNE.get(), 16, 0.55, 0.01);
         level.sendParticles(ParticleTypes.WAX_ON, player.getX(), player.getY() + player.getBbHeight() * 0.55, player.getZ(), 18, 0.5, 0.8, 0.5, 0.03);
         level.playSound(null, player.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.95F, 1.35F);
      }
   }

   public static void performBerkanaRune(ServerPlayer player) {
      long now = player.level().getGameTime();
      player.getPersistentData().putLong(CU_RUNE_BERKANA_UNTIL_TAG, now + 180L);
      player.getPersistentData().putLong(CU_RUNE_BERKANA_NEXT_HEAL_TAG, now + 1L);
      ServantCardSkillUtils.clearHarmfulEffects(player);
      player.heal(4.0F);
      player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 120, 1, false, true, true));
      if (player.level() instanceof ServerLevel level) {
         spawnRuneParticles(level, player, ModParticles.BERKANA_RUNE.get(), 16, 0.5, 0.01);
         level.sendParticles(ParticleTypes.HEART, player.getX(), player.getY() + player.getBbHeight() * 0.7, player.getZ(), 10, 0.4, 0.55, 0.4, 0.02);
         level.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + player.getBbHeight() * 0.6, player.getZ(), 12, 0.45, 0.7, 0.45, 0.03);
         level.playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.75F, 1.6F);
      }
   }

   public static void performCrouchThrust(ServerPlayer player) {
      Vec3 dir = PlayerNoblePhantasmHelper.horizontalLook(player);
      player.setDeltaMovement(player.getDeltaMovement().add(dir.x * 1.18, 0.12, dir.z * 1.18));
      player.hurtMarked = true;
      ServantCardSkillUtils.hitForwardArc(player, dir, 4.2, 24.0F);
      if (player.level() instanceof ServerLevel level) {
         Vec3 fx = player.position().add(dir.scale(2.0)).add(0.0, player.getBbHeight() * 0.55, 0.0);
         level.sendParticles(ParticleTypes.SWEEP_ATTACK, fx.x, fx.y, fx.z, 4, 0.0, 0.0, 0.0, 0.0);
         level.sendParticles(ParticleTypes.CLOUD, fx.x, fx.y - 0.25, fx.z, 12, 0.45, 0.12, 0.45, 0.035);
         level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.85F, 1.35F);
      }
   }

   public static void performDisengage(ServerPlayer player) {
      ServantCardSkillUtils.clearHarmfulEffects(player);
      player.heal(Math.max(4.0F, player.getMaxHealth() * 0.1F));
      Vec3 back = PlayerNoblePhantasmHelper.horizontalLook(player).scale(-1.6);
      player.setDeltaMovement(player.getDeltaMovement().add(back.x, 0.25, back.z));
      player.hurtMarked = true;
      player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 1, false, true, true));
   }

   private static void spawnRuneParticles(ServerLevel level, ServerPlayer player, net.minecraft.core.particles.SimpleParticleType particle, int count, double spread, double speed) {
      double baseY = player.getY() + player.getBbHeight() * 0.62;
      level.sendParticles(particle, player.getX(), baseY, player.getZ(), count, spread, spread * 0.7, spread, speed);
   }

   private static void spawnAnsuzTrace(ServerLevel level, Vec3 start, Vec3 end) {
      for (double t = 0.0; t <= 1.0; t += 0.1) {
         Vec3 pos = start.lerp(end, t);
         level.sendParticles(ParticleTypes.FLAME, pos.x, pos.y, pos.z, 1, 0.03, 0.03, 0.03, 0.0);
      }
   }

   private static void clearHarmfulEffects(ServerPlayer player) {
      java.util.List<MobEffectInstance> active = java.util.List.copyOf(player.getActiveEffects());
      for (MobEffectInstance effect : active) {
         if (effect.getEffect().value().getCategory() == net.minecraft.world.effect.MobEffectCategory.HARMFUL) {
            player.removeEffect(effect.getEffect());
         }
      }
   }

   private static void clearCuTiwaz(ServerPlayer player) {
      player.getPersistentData().remove(CU_RUNE_TIWAZ_UNTIL_TAG);
      ServantCardSkillUtils.remove(player.getAttribute(Attributes.ATTACK_DAMAGE), CU_TIWAZ_ATTACK_ID);
      ServantCardSkillUtils.remove(player.getAttribute(Attributes.MOVEMENT_SPEED), CU_TIWAZ_SPEED_ID);
      ServantCardSkillUtils.remove(player.getAttribute(Attributes.ARMOR), CU_TIWAZ_ARMOR_ID);
   }


}
