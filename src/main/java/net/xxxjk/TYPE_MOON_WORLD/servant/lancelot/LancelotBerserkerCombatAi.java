package net.xxxjk.TYPE_MOON_WORLD.servant.lancelot;

import java.util.Comparator;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantIdentityHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.LancelotBerserkerEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantTraitTag;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import org.jetbrains.annotations.Nullable;

public final class LancelotBerserkerCombatAi {
   private static final String LAST_SCAN_TICK = "LancelotAiLastScanTick";
   private static final String LAST_DASH_TICK = "LancelotAiLastDashTick";
   private static final String LAST_ROAR_TICK = "LancelotAiLastRoarTick";
   private static final double SCAN_RANGE = 34.0;
   private static final double MELEE_RANGE = 3.2;

   private LancelotBerserkerCombatAi() {
   }

   public static boolean tick(LancelotBerserkerEntity entity, ServerLevel level) {
      long now = level.getGameTime();
      if (LancelotCombatHelper.isUnable(entity, now)) {
         return true;
      }
      LivingEntity target = entity.getTarget();
      if (!isValidTarget(entity, target)) {
         target = scanTarget(entity, level, now);
         entity.setTarget(target);
      }
      if (!isValidTarget(entity, target)) {
         return false;
      }

      CompoundTag data = entity.getPersistentData();
      entity.getLookControl().setLookAt(target, 60.0F, 60.0F);
      boolean phaseTwo = isPhaseTwo(entity, target);
      if (phaseTwo && shouldDrawAroundight(entity, target)) {
         LancelotCombatHelper.tryDrawAroundight(entity, level, target);
      }
      maybeRoar(entity, level, now, phaseTwo);

      double distance = entity.distanceTo(target);
      if (!LancelotCombatHelper.isAroundightMode(entity) && distance >= 8.0 && distance <= 22.0
         && entity.getRandom().nextFloat() < (phaseTwo ? 0.15F : 0.08F)
         && LancelotCombatHelper.throwHeldWeapon(entity, level, target)) {
         return true;
      }
      if (distance > MELEE_RANGE) {
         rush(entity, target, now, phaseTwo);
      } else if (phaseTwo && now - data.getLong(LAST_DASH_TICK) > 32L && entity.getRandom().nextFloat() < 0.2F) {
         sideCrush(entity, target, now);
      } else {
         entity.getNavigation().moveTo(target, phaseTwo ? 1.45 : 1.25);
      }
      return true;
   }

   static boolean isPhaseTwo(LancelotBerserkerEntity entity, LivingEntity target) {
      float ratio = entity.getHealth() / Math.max(1.0F, entity.getMaxHealth());
      return ratio <= 0.50F || ServantIdentityHelper.hasTrait(target, ServantTraitTag.DRAGON)
         || target.getMaxHealth() >= entity.getMaxHealth() * 0.85F;
   }

   private static boolean shouldDrawAroundight(LancelotBerserkerEntity entity, LivingEntity target) {
      if (LancelotCombatHelper.isAroundightMode(entity)) return false;
      if (entity.getCurrentMp() < LancelotCombatHelper.AROUNDIGHT_DRAW_MP_COST) return false;
      return ServantIdentityHelper.hasTrait(target, ServantTraitTag.DRAGON)
         || entity.getHealth() <= entity.getMaxHealth() * 0.45F
         || target.getMaxHealth() >= 180.0F;
   }

   private static void rush(LancelotBerserkerEntity entity, LivingEntity target, long now, boolean phaseTwo) {
      entity.getNavigation().moveTo(target, phaseTwo ? 1.55 : 1.35);
      if (now - entity.getPersistentData().getLong(LAST_DASH_TICK) < (phaseTwo ? 35L : 55L)) {
         return;
      }
      entity.getPersistentData().putLong(LAST_DASH_TICK, now);
      Vec3 dir = target.position().subtract(entity.position()).multiply(1.0, 0.0, 1.0);
      if (dir.lengthSqr() < 1.0E-4) return;
      dir = dir.normalize();
      entity.setDeltaMovement(entity.getDeltaMovement().add(dir.scale(phaseTwo ? 0.95 : 0.65)).add(0.0, 0.08, 0.0));
      entity.hurtMarked = true;
   }

   private static void sideCrush(LancelotBerserkerEntity entity, LivingEntity target, long now) {
      entity.getPersistentData().putLong(LAST_DASH_TICK, now);
      Vec3 toward = target.position().subtract(entity.position()).multiply(1.0, 0.0, 1.0);
      if (toward.lengthSqr() < 1.0E-4) toward = entity.getLookAngle();
      Vec3 side = new Vec3(-toward.z, 0.0, toward.x).normalize().scale(entity.getRandom().nextBoolean() ? 2.2 : -2.2);
      Vec3 pos = target.position().add(side);
      entity.getNavigation().moveTo(pos.x, pos.y, pos.z, 1.55);
      entity.setDeltaMovement(entity.getDeltaMovement().add(side.normalize().scale(0.55)));
      entity.hurtMarked = true;
   }

   private static void maybeRoar(LancelotBerserkerEntity entity, ServerLevel level, long now, boolean phaseTwo) {
      CompoundTag data = entity.getPersistentData();
      if (now - data.getLong(LAST_ROAR_TICK) < (phaseTwo ? 260L : 420L) || entity.getCurrentMp() < 15.0) {
         return;
      }
      data.putLong(LAST_ROAR_TICK, now);
      entity.setCurrentMp(Math.max(0.0, entity.getCurrentMp() - 15.0));
      entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 160, phaseTwo ? 1 : 0, false, true, true));
      entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 120, 0, false, true, true));
      level.playSound(null, entity.blockPosition(), net.xxxjk.TYPE_MOON_WORLD.init.ModSounds.LANCELOT_BERSERKER_VOICE_ROAR.get(),
         net.minecraft.sounds.SoundSource.HOSTILE, 1.2F, 0.72F);
   }

   @Nullable
   private static LivingEntity scanTarget(LancelotBerserkerEntity entity, ServerLevel level, long now) {
      CompoundTag data = entity.getPersistentData();
      if (now - data.getLong(LAST_SCAN_TICK) < 12L) {
         return null;
      }
      data.putLong(LAST_SCAN_TICK, now);
      AABB area = entity.getBoundingBox().inflate(SCAN_RANGE);
      return level.getEntitiesOfClass(LivingEntity.class, area, candidate -> isValidTarget(entity, candidate)).stream()
         .min(Comparator.comparingDouble(candidate -> entity.distanceToSqr(candidate) - threatBias(candidate)))
         .orElse(null);
   }

   private static double threatBias(LivingEntity candidate) {
      double bias = Math.min(80.0, candidate.getAttributeValue(Attributes.ATTACK_DAMAGE) * 3.0 + candidate.getMaxHealth() * 0.05);
      if (ServantIdentityHelper.hasTrait(candidate, ServantTraitTag.DRAGON)) bias += 40.0;
      return bias;
   }

   private static boolean isValidTarget(LancelotBerserkerEntity entity, @Nullable LivingEntity target) {
      return target != null && target != entity && target.isAlive()
         && !EntityUtils.isImmunePlayerTarget(target)
         && !entity.isAlliedTo(target) && !target.isAlliedTo(entity)
         && !net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantMasterProtection.isProtectedMaster(entity, target)
         && EntityUtils.isValidCombatTarget(entity, target);
   }
}
