package net.xxxjk.TYPE_MOON_WORLD.effect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

public class MartialControlEffect extends UncurableEffect {
   private final boolean freeze;

   public MartialControlEffect(MobEffectCategory category, int color, boolean freeze) {
      super(category, color);
      this.freeze = freeze;
   }

   @Override
   public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
      return true;
   }

   @Override
   public boolean applyEffectTick(LivingEntity entity, int amplifier) {
      double factor = this.freeze ? 0.0 : 0.18;
      Vec3 motion = entity.getDeltaMovement();
      entity.setDeltaMovement(motion.x * factor, Math.min(0.0, motion.y), motion.z * factor);
      entity.stopUsingItem();
      entity.hurtMarked = true;
      if (entity instanceof Mob mob) {
         mob.getNavigation().stop();
      }
      if (entity.level() instanceof ServerLevel level && entity.tickCount % 5 == 0) {
         level.sendParticles(this.freeze ? net.minecraft.core.particles.ParticleTypes.CRIT : net.minecraft.core.particles.ParticleTypes.CLOUD,
            entity.getX(), entity.getY() + 0.25, entity.getZ(), 5, 0.25, 0.08, 0.25, 0.01);
      }
      return true;
   }
}
