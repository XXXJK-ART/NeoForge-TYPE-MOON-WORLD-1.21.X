package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantMasterProtection;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;

public class HeraclesEntity extends ServantEntity {
   public static final String SERVANT_KEY = "heracles";
   private static final double BASIC_SWEEP_RANGE = 4.2;
   private static final int BASIC_SWEEP_MAX_TARGETS = 10;

   public HeraclesEntity(EntityType<HeraclesEntity> entityType, Level level) {
      super(entityType, level, SERVANT_KEY);
   }

   @Override
   public void knockback(double strength, double x, double z) {
   }

   @Override
   public boolean doHurtTarget(Entity target) {
      if (target instanceof LivingEntity living && ServantMasterProtection.isProtectedMaster(this, living)) {
         return false;
      }
      return super.doHurtTarget(target);
   }

   @Override
   public boolean doBasicHurtTarget(LivingEntity target) {
      if (target == null || ServantMasterProtection.isProtectedMaster(this, target)) return false;
      boolean primaryHit = super.doHurtTarget(target);
      if (!(this.level() instanceof ServerLevel level)) return primaryHit;

      Vec3 forward = target.position().subtract(this.position()).multiply(1.0, 0.0, 1.0);
      if (forward.lengthSqr() < 1.0E-4) forward = this.getLookAngle().multiply(1.0, 0.0, 1.0);
      if (forward.lengthSqr() < 1.0E-4) forward = new Vec3(0.0, 0.0, 1.0);
      forward = forward.normalize();
      Vec3 sweepForward = forward;
      AABB area = this.getBoundingBox().inflate(BASIC_SWEEP_RANGE, 2.5, BASIC_SWEEP_RANGE);
      List<LivingEntity> victims = level.getEntitiesOfClass(LivingEntity.class, area,
         candidate -> isBasicSweepTarget(candidate, target, sweepForward));
      boolean secondaryHit = false;
      int hitCount = 0;
      for (LivingEntity victim : victims) {
         if (hitCount >= BASIC_SWEEP_MAX_TARGETS) break;
         if (super.doHurtTarget(victim)) {
            secondaryHit = true;
            hitCount++;
         }
      }
      if (primaryHit || secondaryHit) {
         Vec3 center = this.position().add(sweepForward.scale(2.0)).add(0.0, this.getBbHeight() * 0.52, 0.0);
         level.sendParticles(ParticleTypes.SWEEP_ATTACK, center.x, center.y, center.z,
            5, 1.25, 0.28, 1.25, 0.0);
         level.playSound(null, this.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
            SoundSource.HOSTILE, 1.25F, 0.72F);
      }
      return primaryHit || secondaryHit;
   }

   private boolean isBasicSweepTarget(LivingEntity candidate, LivingEntity primary, Vec3 forward) {
      if (candidate == this || candidate == primary || !candidate.isAlive() || candidate.isAlliedTo(this)
         || this.isAlliedTo(candidate) || ServantMasterProtection.isProtectedMaster(this, candidate)
         || !EntityUtils.isValidCombatTarget(this, candidate)) {
         return false;
      }
      Vec3 offset = candidate.position().subtract(this.position());
      if (Math.abs(candidate.getY() - this.getY()) > 2.75
         || offset.horizontalDistanceSqr() > BASIC_SWEEP_RANGE * BASIC_SWEEP_RANGE
         || !HeraclesCombatRules.isInsideBasicSweepArc(forward.x, forward.z, offset.x, offset.z)) {
         return false;
      }
      return this.hasLineOfSight(candidate) || offset.horizontalDistanceSqr() <= 2.25 * 2.25;
   }
}
