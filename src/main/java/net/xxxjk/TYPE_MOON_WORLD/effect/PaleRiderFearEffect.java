package net.xxxjk.TYPE_MOON_WORLD.effect;

import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

public final class PaleRiderFearEffect extends UncurableEffect {
   public PaleRiderFearEffect() {
      super(MobEffectCategory.HARMFUL, 0x8A8A8A);
   }

   @Override
   public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
      return true;
   }

   @Override
   public boolean applyEffectTick(LivingEntity entity, int amplifier) {
      entity.setDeltaMovement(0.0, Math.min(0.0, entity.getDeltaMovement().y), 0.0);
      entity.setSprinting(false);
      entity.stopUsingItem();
      if (entity instanceof Mob mob) {
         mob.getNavigation().stop();
      }
      return true;
   }
}
