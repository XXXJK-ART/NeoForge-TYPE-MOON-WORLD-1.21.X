package net.xxxjk.TYPE_MOON_WORLD.servant.card;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.PlayerNoblePhantasmHelper;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXServerEffects;

import static net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardSkillUtils.*;

public final class ServantCardLiShuwenSkills {
   private static final String CONCEALMENT_UNTIL_TAG = "ServantCardConcealmentUntil";
   private static final int CIRCLE_REALM_DURATION = 1400;
   private static final int WU_ER_DA_HIT_COOLDOWN = 1200;
   private static final double WU_ER_DA_HIT_COST = 15.0;

   private ServantCardLiShuwenSkills() {
   }

   public static void tick(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      if (!vars.servant_card_transformed || !"li_shuwen".equals(vars.servant_card_id)) {
         clear(player);
         return;
      }
      if (player.tickCount % 60 == 0 && player.getHealth() < player.getMaxHealth()) {
         player.heal(1.0F);
      }
      int concealmentUntil = player.getPersistentData().getInt(CONCEALMENT_UNTIL_TAG);
      if (concealmentUntil <= 0) {
         return;
      }
      if (player.tickCount > concealmentUntil || !player.hasEffect(MobEffects.INVISIBILITY)) {
         player.getPersistentData().remove(CONCEALMENT_UNTIL_TAG);
         return;
      }
      ServantCardConcealmentHelper.tick(player);
   }

   public static void clear(ServerPlayer player) {
      player.getPersistentData().remove(CONCEALMENT_UNTIL_TAG);
   }

   public static void performCircleRealm(ServerPlayer player) {
      ServantCardConcealmentHelper.apply(player, CIRCLE_REALM_DURATION);
      player.getPersistentData().putInt(CONCEALMENT_UNTIL_TAG, player.tickCount + CIRCLE_REALM_DURATION);
      if (player.level() instanceof ServerLevel level) {
         level.playSound(null, player.blockPosition(), SoundEvents.WOOL_BREAK, SoundSource.PLAYERS, 0.55F, 0.55F);
      }
   }

   public static void performLiYinYang(ServerPlayer player) {
      revealCircleRealm(player);
      player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 1, false, true, true));
      player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 1, false, true, true));
      if (player.level() instanceof ServerLevel level) {
         level.sendParticles(ParticleTypes.SMOKE, player.getX(), player.getY() + 0.9, player.getZ(), 22, 0.5, 0.3, 0.5, 0.025);
      }
   }

   public static void performLiShoulder(ServerPlayer player) {
      revealCircleRealm(player);
      LivingEntity target = findLookTarget(player, 7.0, 1.6);
      Vec3 dir = target == null ? PlayerNoblePhantasmHelper.horizontalLook(player) : target.position().subtract(player.position()).normalize();
      player.setDeltaMovement(player.getDeltaMovement().add(dir.x * 1.9, 0.16, dir.z * 1.9));
      player.hurtMarked = true;
      hitForwardArc(player, dir, 4.0, 24.0F);
      spawnLiHitFx(player, target);
   }

   public static void performLiInterrupt(ServerPlayer player) {
      revealCircleRealm(player);
      Vec3 dir = PlayerNoblePhantasmHelper.horizontalLook(player);
      for (LivingEntity target : player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(4.5, 1.5, 4.5), e -> e.isAlive() && e != player && !EntityUtils.isImmunePlayerTarget(e))) {
         target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2, false, true, true));
         target.invulnerableTime = 0;
         target.hurt(player.damageSources().playerAttack(player), 20.0F);
         target.invulnerableTime = 0;
         target.push(dir.x * 0.4, 0.12, dir.z * 0.4);
         target.hurtMarked = true;
      }
      spawnLiHitFx(player, null);
   }

   public static void performLiCounter(ServerPlayer player) {
      revealCircleRealm(player);
      player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 90, 2, false, true, true));
      player.getPersistentData().putInt("ServantCardLiCounterUntil", player.tickCount + 90);
      if (player.level() instanceof ServerLevel level) {
         level.sendParticles(ParticleTypes.CRIT, player.getX(), player.getY() + 0.9, player.getZ(), 16, 0.35, 0.2, 0.35, 0.04);
      }
   }

   public static void performLiPursuit(ServerPlayer player) {
      revealCircleRealm(player);
      LivingEntity target = findLookTarget(player, 14.0, 1.8);
      if (target == null) {
         return;
      }
      Vec3 dir = target.position().subtract(player.position()).normalize();
      player.teleportTo(target.getX() - dir.x * 1.1, target.getY(), target.getZ() - dir.z * 1.1);
      player.hurtMarked = true;
      target.invulnerableTime = 0;
      target.hurt(player.damageSources().playerAttack(player), 26.0F);
      target.invulnerableTime = 0;
      spawnLiHitFx(player, target);
   }

   public static boolean performLiWuErDa(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      LivingEntity target = findLookTarget(player, 5.2, 1.9);
      if (target == null) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.no_target"), true);
         return false;
      }
      revealCircleRealm(player);
      if (!ServantCardManaService.consume(player, vars, WU_ER_DA_HIT_COST)) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.not_enough_mp"), true);
         return false;
      }
      ServantCardTransformManager.setNoblePhantasmCooldown(player, vars, WU_ER_DA_HIT_COOLDOWN);
      target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 180, 2, false, true, true));
      target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 5, false, true, true));
      spawnLiHitFx(player, target);
      if (player.level() instanceof ServerLevel level) {
         VFXServerEffects.spawn(level, "servant_li_wu_er_da", target, 96.0);
      }
      TYPE_MOON_WORLD.queueServerWork(10, () -> {
         if (!player.isAlive() || !target.isAlive()) {
            return;
         }
         Vec3 dir = target.position().subtract(player.position());
         if (dir.lengthSqr() < 0.001) {
            dir = PlayerNoblePhantasmHelper.horizontalLook(player);
         } else {
            dir = new Vec3(dir.x, 0.0, dir.z).normalize();
         }
         target.invulnerableTime = 0;
         target.hurt(player.damageSources().playerAttack(player), 96.0F);
         target.invulnerableTime = 0;
         target.push(dir.x * 2.8, 0.35, dir.z * 2.8);
         target.hurtMarked = true;
         spawnLiHitFx(player, target);
      });
      return true;
   }

   public static void performLiFierceTiger(ServerPlayer player) {
      Vec3 dir = PlayerNoblePhantasmHelper.horizontalLook(player);
      player.setDeltaMovement(player.getDeltaMovement().add(dir.x * 2.0, 0.12, dir.z * 2.0));
      player.hurtMarked = true;
      hitForwardArc(player, dir, 5.0, 38.0F);
      spawnLiHitFx(player, findLookTarget(player, 5.5, 1.6));
   }

   public static void performLiBajiCombo(ServerPlayer player) {
      revealCircleRealm(player);
      Vec3 dir = PlayerNoblePhantasmHelper.horizontalLook(player);
      player.setDeltaMovement(player.getDeltaMovement().add(dir.scale(1.15)).add(0.0, 0.08, 0.0));
      player.hurtMarked = true;
      hitForwardArc(player, dir, 4.5, 30.0F);
      spawnLiHitFx(player, findLookTarget(player, 5.0, 1.8));
   }

   public static void performLiHighJump(ServerPlayer player) {
      revealCircleRealm(player);
      player.setDeltaMovement(player.getDeltaMovement().x, 1.35, player.getDeltaMovement().z);
      player.fallDistance = 0.0F;
      player.hurtMarked = true;
   }

   public static void performLiFaJin(ServerPlayer player) {
      revealCircleRealm(player);
      LivingEntity target = findLookTarget(player, 6.0, 1.8);
      if (target == null) return;
      target.invulnerableTime = 0;
      target.hurt(player.damageSources().playerAttack(player), 42.0F);
      target.invulnerableTime = 0;
      Vec3 dir = target.position().subtract(player.position()).multiply(1.0, 0.0, 1.0).normalize();
      target.push(dir.x * 0.65, 0.12, dir.z * 0.65);
      target.hurtMarked = true;
      spawnLiHitFx(player, target);
   }

   public static void revealCircleRealm(ServerPlayer player) {
      player.getPersistentData().remove(CONCEALMENT_UNTIL_TAG);
      if (player.hasEffect(MobEffects.INVISIBILITY)) {
         player.removeEffect(MobEffects.INVISIBILITY);
      }
   }

   public static void spawnLiHitFx(ServerPlayer player, LivingEntity target) {
      if (player.level() instanceof ServerLevel level) {
         Vec3 pos = target == null ? player.position().add(PlayerNoblePhantasmHelper.horizontalLook(player).scale(1.5)).add(0.0, 0.9, 0.0) : target.position().add(0.0, target.getBbHeight() * 0.55, 0.0);
         level.sendParticles(ParticleTypes.SWEEP_ATTACK, pos.x, pos.y, pos.z, 1, 0.0, 0.0, 0.0, 0.0);
         level.sendParticles(ParticleTypes.SMOKE, pos.x, pos.y, pos.z, 10, 0.22, 0.18, 0.22, 0.02);
         level.playSound(null, BlockPos.containing(pos), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 0.8F, 0.72F);
      }
   }

}

