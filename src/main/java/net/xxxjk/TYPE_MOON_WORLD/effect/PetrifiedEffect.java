package net.xxxjk.TYPE_MOON_WORLD.effect;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

public class PetrifiedEffect extends UncurableEffect {
   public static final String TAG_PREV_NO_AI = "TypeMoonPrevNoAi";

   public PetrifiedEffect(MobEffectCategory category, int color) {
      super(category, color);
   }

   @Override
   public void onEffectStarted(LivingEntity entity, int amplifier) {
      if (entity instanceof Mob mob) {
         if (!mob.getPersistentData().contains(TAG_PREV_NO_AI)) {
            mob.getPersistentData().putBoolean(TAG_PREV_NO_AI, mob.isNoAi());
         }
         mob.setNoAi(true);
      }
   }

   @Override
   public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
      return true;
   }

   @Override
   public boolean applyEffectTick(LivingEntity entity, int amplifier) {
      entity.setSprinting(false);
      entity.stopUsingItem();
      entity.setDeltaMovement(0.0, 0.0, 0.0);
      entity.hurtMarked = true;
      if (entity instanceof Mob mob) {
         mob.getNavigation().stop();
         mob.setTarget(null);
      }

      if (entity.level() instanceof ServerLevel level && entity.tickCount % 6 == 0) {
         level.sendParticles(
            new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState()),
            entity.getX(),
            entity.getY() + entity.getBbHeight() * 0.45,
            entity.getZ(),
            6,
            0.25,
            0.35,
            0.25,
            0.0
         );
      }
      return true;
   }

}
