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
   private static final String LAST_MAUL_TICK = "LancelotAiLastMaulTick";
   private static final String LAST_ROAR_TICK = "LancelotAiLastRoarTick";
   private static final double SCAN_RANGE = 34.0;
   private static final double MELEE_RANGE = 3.2;
   private static final float PHASE_TWO_HEALTH_RATIO = 0.60F;

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
      if (shouldGroundSlam(entity, target, distance)
         && LancelotCombatHelper.tryGroundSlam(entity, level, phaseTwo)) {
         return true;
      }
      if (distance <= (phaseTwo ? 4.8 : 4.2) && tryMaul(entity, target, now, phaseTwo)) {
         return true;
      }
      if (shouldThrowWeapon(entity, target, distance, phaseTwo)) {
         LancelotCombatHelper.throwHeldWeapon(entity, level, target);
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

   private static boolean tryMaul(LancelotBerserkerEntity entity, LivingEntity target, long now, boolean phaseTwo) {
      CompoundTag data = entity.getPersistentData();
      entity.getNavigation().moveTo(target, phaseTwo ? 1.72 : 1.55);
      Vec3 toward = target.position().subtract(entity.position()).multiply(1.0, 0.0, 1.0);
      if (toward.lengthSqr() > 1.0E-4) {
         Vec3 rush = toward.normalize().scale(phaseTwo ? 0.36 : 0.26);
         entity.setDeltaMovement(entity.getDeltaMovement().add(rush.x, 0.02, rush.z));
         entity.hurtMarked = true;
      }
      long interval = phaseTwo ? 6L : 8L;
      if (now - data.getLong(LAST_MAUL_TICK) < interval) {
         return false;
      }
      data.putLong(LAST_MAUL_TICK, now);
      entity.triggerBasicAttackAnimation();
      entity.doHurtTarget(target);
      if (toward.lengthSqr() > 1.0E-4) {
         Vec3 shove = toward.normalize();
         target.push(shove.x * (phaseTwo ? 0.45 : 0.32), 0.08, shove.z * (phaseTwo ? 0.45 : 0.32));
      }
      return true;
   }

   private static boolean shouldGroundSlam(LancelotBerserkerEntity entity, LivingEntity target, double distance) {
      double horizontalSqr = entity.position().multiply(1.0, 0.0, 1.0).distanceToSqr(target.position().multiply(1.0, 0.0, 1.0));
      double vertical = entity.getY() - target.getY();
      return distance <= 5.2 && (horizontalSqr <= 3.2 * 3.2 || vertical >= 1.25);
   }

   private static boolean shouldThrowWeapon(LancelotBerserkerEntity entity, LivingEntity target, double distance, boolean phaseTwo) {
      if (LancelotCombatHelper.isAroundightMode(entity)) {
         return false;
      }
      double verticalGap = Math.abs(target.getY() - entity.getY());
      boolean awkwardReach = verticalGap >= 2.2 && distance >= 4.8;
      boolean chaseToss = distance >= 7.0 && distance <= 18.0;
      boolean veryFar = distance > 18.0 && distance <= 30.0;
      if (!awkwardReach && !chaseToss && !veryFar) {
         return false;
      }
      float chance = phaseTwo ? 0.055F : 0.018F;
      if (veryFar || awkwardReach) {
         chance += phaseTwo ? 0.025F : 0.012F;
      }
      return entity.getRandom().nextFloat() < chance;
   }

   static boolean isPhaseTwo(LancelotBerserkerEntity entity, LivingEntity target) {
      float ratio = entity.getHealth() / Math.max(1.0F, entity.getMaxHealth());
      return ratio <= PHASE_TWO_HEALTH_RATIO || ServantIdentityHelper.hasTrait(target, ServantTraitTag.DRAGON)
         || target.getMaxHealth() >= entity.getMaxHealth() * 0.85F;
   }

   private static boolean shouldDrawAroundight(LancelotBerserkerEntity entity, LivingEntity target) {
      if (LancelotCombatHelper.isAroundightMode(entity)) return false;
      if (entity.getCurrentMp() < LancelotCombatHelper.AROUNDIGHT_DRAW_MP_COST) return false;
      return ServantIdentityHelper.hasTrait(target, ServantTraitTag.DRAGON)
         || entity.getHealth() <= entity.getMaxHealth() * PHASE_TWO_HEALTH_RATIO
         || target.getMaxHealth() >= 180.0F;
   }

   private static void rush(LancelotBerserkerEntity entity, LivingEntity target, long now, boolean phaseTwo) {
      entity.getNavigation().moveTo(target, phaseTwo ? 1.82 : 1.62);
      if (now - entity.getPersistentData().getLong(LAST_DASH_TICK) < (phaseTwo ? 24L : 34L)) {
         return;
      }
      entity.getPersistentData().putLong(LAST_DASH_TICK, now);
      Vec3 dir = target.position().subtract(entity.position()).multiply(1.0, 0.0, 1.0);
      if (dir.lengthSqr() < 1.0E-4) return;
      dir = dir.normalize();
      entity.setDeltaMovement(entity.getDeltaMovement().add(dir.scale(phaseTwo ? 1.18 : 0.88)).add(0.0, 0.08, 0.0));
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
      double attackDamage = candidate.getAttribute(Attributes.ATTACK_DAMAGE) != null
         ? candidate.getAttributeValue(Attributes.ATTACK_DAMAGE) : 0.0;
      double bias = Math.min(80.0, attackDamage * 3.0 + candidate.getMaxHealth() * 0.05);
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
