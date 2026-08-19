package net.xxxjk.TYPE_MOON_WORLD.effect;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

public class CremationRiteEffect extends UncurableEffect {
   public CremationRiteEffect() {
      super(MobEffectCategory.HARMFUL, 0xD86A28);
   }

   @Override
   public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
      return duration % 10 == 0;
   }

   @Override
   public boolean applyEffectTick(LivingEntity entity, int amplifier) {
      entity.igniteForSeconds(1.5F + amplifier * 0.75F);
      if (entity.level() instanceof ServerLevel level) {
         level.sendParticles(ParticleTypes.FLAME, entity.getX(), entity.getY() + entity.getBbHeight() * 0.55, entity.getZ(),
            2 + Math.min(6, amplifier), 0.25, 0.25, 0.25, 0.01);
      }
      return true;
   }
}
