package net.xxxjk.TYPE_MOON_WORLD.servant.shadowhassan;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ShadowHassanEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantAiContext;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantNavigationHelper;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;

public final class ShadowHassanCombatHelper {
   private static final String TAG_ATTACK_SEQUENCE = "ShadowHassanAttackSequence";
   private static final String TAG_LAST_BASIC = "ShadowHassanLastBasicAttack";
   private static final String TAG_LAST_SLASH = "ShadowHassanLastSlash";
   private static final String TAG_SLASH_UNTIL = "ShadowHassanSlashUntil";
   private static final int SLASH_COOLDOWN = 240;

   private ShadowHassanCombatHelper() {
   }

   public static void tick(ShadowHassanEntity hassan, ServantAiContext context) {
      LivingEntity target = context.target();
      if (target == null || !EntityUtils.isValidCombatTarget(hassan, target)
         || target.getType().is(ShadowHassanDamageTypes.BLADE_IMMUNE)) {
         hassan.setTarget(null);
         return;
      }
      hassan.setTarget(target);
      hassan.getLookControl().setLookAt(target, 50.0F, 50.0F);
      long now = context.gameTick();
      if (now < hassan.getPersistentData().getLong(TAG_SLASH_UNTIL)) {
         hassan.getNavigation().stop();
         return;
      }
      double distance = hassan.distanceTo(target);
      if (distance <= 8.0 && (!hassan.getPersistentData().contains(TAG_LAST_SLASH)
         || now - hassan.getPersistentData().getLong(TAG_LAST_SLASH) >= SLASH_COOLDOWN)) {
         hassan.getPersistentData().putLong(TAG_LAST_SLASH, now);
         performSlash(hassan, target);
         return;
      }
      if (distance > 2.4) {
         ServantNavigationHelper.moveToTargetThrottled(hassan, target, 1.38, now,
            ServantNavigationHelper.SHORT_REPATH_INTERVAL, 0.55, "ShadowHassanChase");
      } else if (now - hassan.getPersistentData().getLong(TAG_LAST_BASIC) >= 8L
         && !hassan.isPerformingAction()) {
         hassan.getPersistentData().putLong(TAG_LAST_BASIC, now);
         hassan.triggerAttackSwing();
         hassan.doHurtTarget(target);
      }
   }

   public static void performSlash(ShadowHassanEntity hassan, LivingEntity target) {
      long now = hassan.level().getGameTime();
      hassan.getPersistentData().putLong(TAG_SLASH_UNTIL, now + 46L);
      hassan.getNavigation().stop();
      hassan.revealForAttack();
      float damage = Math.max(1.0F, (float)hassan.getAttributeValue(Attributes.ATTACK_DAMAGE));
      for (int index = 0; index < 10; index++) {
         int delay = index * 5;
         TYPE_MOON_WORLD.queueServerWork(delay, () -> {
            if (!hassan.isAlive() || !target.isAlive() || hassan.level() != target.level()
               || hassan.distanceToSqr(target) > 8.0 * 8.0
               || target.getType().is(ShadowHassanDamageTypes.BLADE_IMMUNE)) return;
            hassan.revealForAttack();
            hassan.faceToward(target.position());
            strike(hassan, target, damage);
            if (hassan.level() instanceof ServerLevel level) spawnStrikeFx(level, target, 12);
         });
      }
   }

   public static void onSuccessfulAttack(ShadowHassanEntity hassan, LivingEntity target, boolean ambush) {
      if (!(hassan.level() instanceof ServerLevel level) || !target.isAlive()) return;
      int sequence = hassan.getPersistentData().getInt(TAG_ATTACK_SEQUENCE) + 1;
      hassan.getPersistentData().putInt(TAG_ATTACK_SEQUENCE, sequence);

      target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0, false, false, true), hassan);
      if (ambush) {
         strike(hassan, target, Math.max(5.0F, (float)hassan.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.8F));
         target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 50, 0, false, false, true), hassan);
         target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 35, 1, false, false, true), hassan);
         spawnStrikeFx(level, target, 18);
      }

      if (sequence % 3 == 0) {
         queueStrike(hassan, target, 4, 0.45F);
         queueStrike(hassan, target, 8, 0.45F);
      }
      if (sequence % 5 == 0) {
         target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, false, false, true), hassan);
         target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2, false, false, true), hassan);
         level.sendParticles(ParticleTypes.SQUID_INK, target.getX(), target.getY() + target.getBbHeight() * 0.5,
            target.getZ(), 28, 0.48, 0.7, 0.48, 0.025);
      }
   }

   private static void queueStrike(ShadowHassanEntity hassan, LivingEntity target, int delay, float multiplier) {
      TYPE_MOON_WORLD.queueServerWork(delay, () -> {
         if (!hassan.isAlive() || !target.isAlive() || hassan.level() != target.level()
            || hassan.distanceToSqr(target) > 5.0 * 5.0
            || target.getType().is(ShadowHassanDamageTypes.BLADE_IMMUNE)) return;
         hassan.revealForAttack();
         strike(hassan, target, Math.max(3.0F, (float)hassan.getAttributeValue(Attributes.ATTACK_DAMAGE) * multiplier));
         if (hassan.level() instanceof ServerLevel level) spawnStrikeFx(level, target, 10);
      });
   }

   private static void strike(ShadowHassanEntity hassan, LivingEntity target, float damage) {
      target.invulnerableTime = 0;
      target.hurt(hassan.damageSources().mobAttack(hassan), damage);
   }

   private static void spawnStrikeFx(ServerLevel level, LivingEntity target, int count) {
      level.sendParticles(ParticleTypes.SQUID_INK, target.getX(), target.getY() + target.getBbHeight() * 0.52,
         target.getZ(), count, 0.34, 0.52, 0.34, 0.018);
      level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + target.getBbHeight() * 0.52,
         target.getZ(), Math.max(4, count / 2), 0.3, 0.45, 0.3, 0.08);
      level.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 0.8F, 0.55F);
   }
}
