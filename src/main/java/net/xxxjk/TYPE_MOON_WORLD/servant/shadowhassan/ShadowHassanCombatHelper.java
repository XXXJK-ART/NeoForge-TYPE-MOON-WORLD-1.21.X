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

public final class ShadowHassanCombatHelper {
   private static final String TAG_ATTACK_SEQUENCE = "ShadowHassanAttackSequence";

   private ShadowHassanCombatHelper() {
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
