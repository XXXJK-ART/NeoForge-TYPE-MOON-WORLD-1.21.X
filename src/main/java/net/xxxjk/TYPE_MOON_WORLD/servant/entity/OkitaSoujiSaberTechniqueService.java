package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantMasterDamageRouter;

public final class OkitaSoujiSaberTechniqueService {
   public static final int MUMYOUDAN_WINDUP_TICKS = 10;
   public static final double MUMYOUDAN_RANGE = 3.0;
   private static final int[] MUMYOUDAN_STRIKE_TICKS = {4, 7, 10};
   private static final float MUMYOUDAN_DAMAGE = 150.0F;

   private OkitaSoujiSaberTechniqueService() { }

   public static int[] mumyoudanStrikeTicks() {
      return MUMYOUDAN_STRIKE_TICKS.clone();
   }

   public static boolean canLandMumyoudanStrike(LivingEntity attacker, LivingEntity target, ServerLevel level) {
      return attacker != null && target != null && attacker.isAlive() && !attacker.isRemoved()
         && target.isAlive() && !target.isRemoved() && attacker.level() == level && target.level() == level
         && attacker.distanceTo(target) <= MUMYOUDAN_RANGE && attacker.hasLineOfSight(target);
   }

   public static boolean strikeMumyoudanPoint(LivingEntity attacker, LivingEntity target, ServerLevel level, int strikeIndex) {
      if (!canLandMumyoudanStrike(attacker, target, level) || strikeIndex < 0 || strikeIndex >= MUMYOUDAN_STRIKE_TICKS.length) return false;
      Vec3 point = mumyoudanStrikePoint(attacker, strikeIndex);
      target.stopUsingItem();
      target.removeEffect(MobEffects.DAMAGE_RESISTANCE);
      target.removeEffect(MobEffects.ABSORPTION);
      target.invulnerableTime = 0;
      boolean damaged;
      try (var segment = ServantMasterDamageRouter.attackSegment()) {
         damaged = target.hurt(attacker.damageSources().source(OkitaSoujiSaberDamageTypes.MUMYOUDAN_ZUKI, attacker), MUMYOUDAN_DAMAGE);
      }
      target.invulnerableTime = 0;
      target.getPersistentData().putLong("OkitaMumyoudanDefenseDisabledUntil", level.getGameTime() + 100L);
      level.sendParticles(ParticleTypes.CRIT, point.x, point.y, point.z, 12, 0.25, 0.35, 0.25, 0.08);
      return damaged;
   }

   /** The three server hit points follow the forward-most sword-tip positions in the imported clip. */
   public static Vec3 mumyoudanStrikePoint(LivingEntity attacker, int strikeIndex) {
      Vec3 forward = attacker.getLookAngle().multiply(1.0, 0.0, 1.0);
      forward = forward.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : forward.normalize();
      double distance = 2.05 + strikeIndex * 0.48;
      return attacker.position().add(0.0, attacker.getBbHeight() * 0.55, 0.0).add(forward.scale(distance));
   }
}
