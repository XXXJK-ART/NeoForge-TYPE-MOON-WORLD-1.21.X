package net.xxxjk.TYPE_MOON_WORLD.servant.shadowhassan;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
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
   private static final String TAG_AMBUSH_READY = "ShadowHassanAmbushReady";
   private static final String TAG_BIND_READY = "ShadowHassanBindReady";
   private static final String TAG_FLURRY_READY = "ShadowHassanFlurryReady";
   private static final int SLASH_COOLDOWN = 300;
   private static final int SLASH_STRIKE_COUNT = 200;
   private static final int SLASH_STRIKES_PER_TICK = 2;
   private static final long SLASH_DURATION_TICKS = 100L;

   private ShadowHassanCombatHelper() {
   }

   public static void tick(ShadowHassanEntity hassan, ServantAiContext context) {
      if (hassan.isPossessing()) return;
      if (ShadowHassanRules.isTotalDarkness(hassan.level(), hassan.blockPosition())) {
         hassan.setTarget(null);
         return;
      }
      LivingEntity target = context.target();
      if (target == null || !EntityUtils.isValidCombatTarget(hassan, target)
         || !ShadowHassanRules.isNpcAssassinationTarget(hassan, target)
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
      boolean fullyExposed = hassan.isFullyExposed();
      if (!fullyExposed) {
         if (tryAmbush(hassan, target, distance, now)) return;
         if (distance > 8.0 && hassan.tickCount % 40 == 0 && hassan.tryStartPossession(target)) return;
         pursueWithoutAttacking(hassan, target, distance, now);
         return;
      }
      if (trySlash(hassan, target, distance, now)) return;
      if (distance <= 7.0 && now >= hassan.getPersistentData().getLong(TAG_FLURRY_READY)
         && hassan.getCurrentMp() >= 24.0) {
         performFlurry(hassan, target, now);
         return;
      }
      if (distance <= 16.0 && now >= hassan.getPersistentData().getLong(TAG_BIND_READY)
         && hassan.getCurrentMp() >= 18.0) {
         performBind(hassan, target, now);
         return;
      }
      if (distance <= 18.0 && now >= hassan.getPersistentData().getLong(TAG_AMBUSH_READY)
         && hassan.getCurrentMp() >= 20.0) {
         if (tryAmbush(hassan, target, distance, now)) return;
      }
      pursueWithoutAttacking(hassan, target, distance, now);
   }

   private static boolean trySlash(ShadowHassanEntity hassan, LivingEntity target, double distance, long now) {
      if (distance > 8.0 || hassan.getPersistentData().contains(TAG_LAST_SLASH)
         && now - hassan.getPersistentData().getLong(TAG_LAST_SLASH) < SLASH_COOLDOWN
         || hassan.getCurrentMp() < 30.0) return false;
      hassan.getPersistentData().putLong(TAG_LAST_SLASH, now);
      performSlash(hassan, target);
      return true;
   }

   private static boolean tryAmbush(ShadowHassanEntity hassan, LivingEntity target, double distance, long now) {
      if (distance > 18.0 || now < hassan.getPersistentData().getLong(TAG_AMBUSH_READY)
         || hassan.getCurrentMp() < 20.0) return false;
      Vec3 behind = target.position().subtract(horizontal(target.getLookAngle()).scale(1.8));
      if (!hassan.teleportToShadowPoint(behind, target)) return false;
      hassan.getPersistentData().putLong(TAG_AMBUSH_READY, now + 120L);
      hassan.setCurrentMp(hassan.getCurrentMp() - 20.0);
      hassan.revealForAttack();
      strike(hassan, target, 34.0F);
      target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, false, false, true));
      if (hassan.level() instanceof ServerLevel level) spawnStrikeFx(level, target, 18);
      return true;
   }

   private static void pursueWithoutAttacking(ShadowHassanEntity hassan, LivingEntity target, double distance, long now) {
      if (distance > 2.4) {
         ServantNavigationHelper.moveToTargetThrottled(hassan, target, 1.38, now,
            ServantNavigationHelper.SHORT_REPATH_INTERVAL, 0.55, "ShadowHassanChase");
      } else if (!hassan.isFullyExposed()) {
         hassan.getNavigation().stop();
      } else if (now - hassan.getPersistentData().getLong(TAG_LAST_BASIC) >= 8L
         && !hassan.isPerformingAction()) {
         hassan.getPersistentData().putLong(TAG_LAST_BASIC, now);
         hassan.triggerAttackSwing();
         hassan.doHurtTarget(target);
      }
   }

   public static void performSlash(ShadowHassanEntity hassan, LivingEntity target) {
      long now = hassan.level().getGameTime();
      if (hassan.getCurrentMp() < 30.0 || ShadowHassanRules.isTotalDarkness(hassan.level(), hassan.blockPosition())) return;
      hassan.setCurrentMp(hassan.getCurrentMp() - 30.0);
      hassan.getPersistentData().putLong(TAG_SLASH_UNTIL, now + SLASH_DURATION_TICKS);
      hassan.getNavigation().stop();
      hassan.revealForAttack();
      float damage = Math.max(1.0F, (float)hassan.getAttributeValue(Attributes.ATTACK_DAMAGE));
      for (int index = 0; index < SLASH_STRIKE_COUNT; index++) {
         int delay = 1 + index / SLASH_STRIKES_PER_TICK;
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

   private static void performBind(ShadowHassanEntity hassan, LivingEntity target, long now) {
      hassan.getPersistentData().putLong(TAG_BIND_READY, now + 160L);
      hassan.setCurrentMp(hassan.getCurrentMp() - 18.0);
      hassan.revealForAttack();
      strike(hassan, target, 12.0F);
      target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 120, 0, false, false, true), hassan);
      target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 4, false, true, true), hassan);
      target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 1, false, true, true), hassan);
      if (hassan.level() instanceof ServerLevel level) spawnStrikeFx(level, target, 24);
   }

   private static void performFlurry(ShadowHassanEntity hassan, LivingEntity target, long now) {
      hassan.getPersistentData().putLong(TAG_FLURRY_READY, now + 180L);
      hassan.setCurrentMp(hassan.getCurrentMp() - 24.0);
      hassan.revealForAttack();
      for (int delay : new int[]{0, 4, 8}) {
         TYPE_MOON_WORLD.queueServerWork(delay, () -> {
            if (!hassan.isAlive() || !target.isAlive() || hassan.level() != target.level()
               || hassan.distanceToSqr(target) > 9.0 * 9.0
               || target.getType().is(ShadowHassanDamageTypes.BLADE_IMMUNE)) return;
            strike(hassan, target, 15.0F);
            if (hassan.level() instanceof ServerLevel level) spawnStrikeFx(level, target, 10);
         });
      }
   }

   private static Vec3 horizontal(Vec3 direction) {
      Vec3 horizontal = new Vec3(direction.x, 0.0, direction.z);
      return horizontal.lengthSqr() < 1.0E-5 ? new Vec3(0.0, 0.0, 1.0) : horizontal.normalize();
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
      target.hurt(ShadowHassanDamageSource.forTarget(hassan.damageSources().mobAttack(hassan), target), damage);
      target.invulnerableTime = 0;
      target.hurtTime = 0;
      target.hurtDuration = 0;
   }

   private static void spawnStrikeFx(ServerLevel level, LivingEntity target, int count) {
      level.sendParticles(ParticleTypes.SQUID_INK, target.getX(), target.getY() + target.getBbHeight() * 0.52,
         target.getZ(), count, 0.34, 0.52, 0.34, 0.018);
      level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + target.getBbHeight() * 0.52,
         target.getZ(), Math.max(4, count / 2), 0.3, 0.45, 0.3, 0.08);
      level.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 0.8F, 0.55F);
   }
}
