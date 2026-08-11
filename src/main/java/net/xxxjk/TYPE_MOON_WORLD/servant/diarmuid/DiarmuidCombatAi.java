package net.xxxjk.TYPE_MOON_WORLD.servant.diarmuid;

import java.util.Comparator;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.DiarmuidUaDuibhneEntity;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import org.jetbrains.annotations.Nullable;

public final class DiarmuidCombatAi {
   public static final String LAST_ATTACK_TICK = "DiarmuidAiLastAttackTick";
   public static final String LAST_REPOSITION_TICK = "DiarmuidAiLastRepositionTick";
   public static final String LAST_SCAN_TICK = "DiarmuidAiLastScanTick";
   public static final String LAST_STRATEGY_TICK = "DiarmuidAiLastStrategyTick";
   public static final String PREFERRED_SPEAR_TAG = "DiarmuidAiPreferredSpear";
   public static final int SPEAR_RED = 1;
   public static final int SPEAR_YELLOW = 2;
   private static final double SCAN_RANGE = 28.0;
   private static final double RED_ROSE_RANGE = 3.7;
   private static final double YELLOW_ROSE_RANGE = 3.2;
   private static final double IDEAL_RANGE = 3.0;
   private static final double TOO_CLOSE_RANGE = 1.55;
   private static final int ATTACK_INTERVAL = 14;
   private static final int REPOSITION_INTERVAL = 18;
   private static final int STRATEGY_INTERVAL = 360;

   private DiarmuidCombatAi() {
   }

   public static boolean tick(DiarmuidUaDuibhneEntity entity, ServerLevel level) {
      LivingEntity target = entity.getTarget();
      if (!isValidTarget(entity, target)) {
         target = scanTarget(entity, level);
         entity.setTarget(target);
      }
      if (!isValidTarget(entity, target)) {
         return false;
      }

      CompoundTag data = entity.getPersistentData();
      long now = level.getGameTime();
      if (shouldRetreat(entity, target)) {
         data.putLong(DiarmuidUaDuibhneEntity.TAG_RETREAT_UNTIL, now + 80L);
      }
      boolean retreating = now < data.getLong(DiarmuidUaDuibhneEntity.TAG_RETREAT_UNTIL);
      entity.getLookControl().setLookAt(target, 50.0F, 50.0F);
      if (retreating) {
         kiteAway(entity, target, now);
         return true;
      }

      maybeUseKnightStrategy(entity, now);
      boolean red = chooseRedRose(entity, target);
      data.putInt(PREFERRED_SPEAR_TAG, red ? SPEAR_RED : SPEAR_YELLOW);
      double desiredRange = red ? RED_ROSE_RANGE : YELLOW_ROSE_RANGE;
      double distance = entity.distanceTo(target);
      if (distance > desiredRange) {
         approach(entity, target, red);
      } else if (distance < TOO_CLOSE_RANGE) {
         sideStep(entity, target, now, true);
      } else if (now - data.getLong(LAST_REPOSITION_TICK) >= REPOSITION_INTERVAL && entity.getRandom().nextFloat() < 0.22F) {
         sideStep(entity, target, now, false);
      } else {
         entity.getNavigation().moveTo(target, distance > IDEAL_RANGE ? 1.08 : 0.92);
      }
      if (distance <= desiredRange && now - data.getLong(LAST_ATTACK_TICK) >= ATTACK_INTERVAL) {
         data.putLong(LAST_ATTACK_TICK, now);
         entity.doHurtTarget(target);
      }
      return true;
   }

   public static boolean consumePreferredRedRose(DiarmuidUaDuibhneEntity entity, LivingEntity target) {
      CompoundTag data = entity.getPersistentData();
      int preferred = data.getInt(PREFERRED_SPEAR_TAG);
      data.remove(PREFERRED_SPEAR_TAG);
      if (preferred == SPEAR_RED) return true;
      if (preferred == SPEAR_YELLOW) return false;
      return chooseRedRose(entity, target);
   }

   static boolean chooseRedRose(DiarmuidUaDuibhneEntity entity, LivingEntity target) {
      boolean redReady = DiarmuidCombatHelper.isRedActive(entity);
      boolean yellowReady = DiarmuidCombatHelper.isYellowActive(entity);
      if (redReady && DiarmuidCombatHelper.shouldPreferRedRose(target)) return true;
      if (yellowReady && DiarmuidCombatHelper.shouldPreferYellowRose(target)) return false;
      if (!yellowReady) return true;
      if (!redReady) return false;
      return entity.getRandom().nextBoolean();
   }

   private static void maybeUseKnightStrategy(DiarmuidUaDuibhneEntity entity, long now) {
      CompoundTag data = entity.getPersistentData();
      if (now - data.getLong(LAST_STRATEGY_TICK) < STRATEGY_INTERVAL || entity.getCurrentMp() < 10.0) {
         return;
      }
      data.putLong(LAST_STRATEGY_TICK, now);
      entity.setCurrentMp(Math.max(0.0, entity.getCurrentMp() - 10.0));
      entity.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 15 * 20, 0, false, true, true));
      entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 15 * 20, 0, false, true, true));
   }

   private static void approach(DiarmuidUaDuibhneEntity entity, LivingEntity target, boolean red) {
      if (!red && entity.distanceToSqr(target) > 8.0 * 8.0 && entity.getCurrentMp() >= 15.0 && entity.getRandom().nextFloat() < 0.08F) {
         entity.setCurrentMp(Math.max(0.0, entity.getCurrentMp() - 15.0));
         entity.addEffect(new MobEffectInstance(MobEffects.JUMP, 10 * 20, 1, false, true, true));
         entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 10 * 20, 1, false, true, true));
      }
      entity.getNavigation().moveTo(target, red ? 1.18 : 1.28);
   }

   private static void kiteAway(DiarmuidUaDuibhneEntity entity, LivingEntity target, long now) {
      CompoundTag data = entity.getPersistentData();
      if (now - data.getLong(LAST_REPOSITION_TICK) < REPOSITION_INTERVAL) {
         return;
      }
      data.putLong(LAST_REPOSITION_TICK, now);
      Vec3 away = entity.position().subtract(target.position()).multiply(1.0, 0.0, 1.0);
      if (away.lengthSqr() < 1.0E-4) away = entity.getLookAngle().multiply(-1.0, 0.0, -1.0);
      Vec3 side = new Vec3(-away.z, 0.0, away.x).normalize().scale(entity.getRandom().nextBoolean() ? 2.5 : -2.5);
      Vec3 pos = entity.position().add(away.normalize().scale(6.0)).add(side);
      entity.getNavigation().moveTo(pos.x, pos.y, pos.z, 1.28);
   }

   private static void sideStep(DiarmuidUaDuibhneEntity entity, LivingEntity target, long now, boolean urgent) {
      CompoundTag data = entity.getPersistentData();
      if (!urgent && now - data.getLong(LAST_REPOSITION_TICK) < REPOSITION_INTERVAL) {
         return;
      }
      data.putLong(LAST_REPOSITION_TICK, now);
      Vec3 toward = target.position().subtract(entity.position()).multiply(1.0, 0.0, 1.0);
      if (toward.lengthSqr() < 1.0E-4) toward = entity.getLookAngle();
      Vec3 side = new Vec3(-toward.z, 0.0, toward.x).normalize().scale(entity.getRandom().nextBoolean() ? 3.0 : -3.0);
      Vec3 back = toward.normalize().scale(urgent ? -1.4 : 0.8);
      Vec3 pos = entity.position().add(side).add(back);
      entity.getNavigation().moveTo(pos.x, pos.y, pos.z, urgent ? 1.35 : 1.18);
   }

   private static boolean shouldRetreat(DiarmuidUaDuibhneEntity entity, LivingEntity target) {
      float ratio = entity.getHealth() / Math.max(1.0F, entity.getMaxHealth());
      if (ratio < 0.30F) return true;
      if (entity.getAttributeValue(Attributes.ARMOR) < 4.0 && entity.distanceToSqr(target) < 2.0 * 2.0) return true;
      return false;
   }

   @Nullable
   private static LivingEntity scanTarget(DiarmuidUaDuibhneEntity entity, ServerLevel level) {
      CompoundTag data = entity.getPersistentData();
      long now = level.getGameTime();
      if (now - data.getLong(LAST_SCAN_TICK) < 20L) {
         return null;
      }
      data.putLong(LAST_SCAN_TICK, now);
      AABB area = entity.getBoundingBox().inflate(SCAN_RANGE);
      return level.getEntitiesOfClass(LivingEntity.class, area, candidate -> isValidTarget(entity, candidate)).stream()
         .min(Comparator.comparingDouble(entity::distanceToSqr))
         .orElse(null);
   }

   private static boolean isValidTarget(DiarmuidUaDuibhneEntity entity, @Nullable LivingEntity target) {
      return target != null && target != entity && target.isAlive()
         && !EntityUtils.isImmunePlayerTarget(target)
         && !entity.isAlliedTo(target) && !target.isAlliedTo(entity)
         && EntityUtils.isValidCombatTarget(entity, target);
   }
}
