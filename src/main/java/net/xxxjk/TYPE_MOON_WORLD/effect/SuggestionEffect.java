package net.xxxjk.TYPE_MOON_WORLD.effect;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class SuggestionEffect extends UncurableEffect {
   public static final String TAG_COMMAND = "TypeMoonSuggestionCommand";
   public static final String TAG_CASTER = "TypeMoonSuggestionCaster";
   public static final String TAG_ATTACK_TARGET = "TypeMoonSuggestionAttackTarget";
   private static final DustParticleOptions VIOLET_DUST = new DustParticleOptions(new Vector3f(0.72F, 0.24F, 1.0F), 1.0F);

   public SuggestionEffect(MobEffectCategory category, int color) {
      super(category, color);
   }

   @Override
   public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
      return true;
   }

   @Override
   public boolean applyEffectTick(LivingEntity entity, int amplifier) {
      if (amplifier >= 3) {
         entity.setDeltaMovement(Vec3.ZERO);
         entity.hurtMarked = true;
         entity.stopUsingItem();
         if (entity instanceof Mob mob) {
            mob.getNavigation().stop();
         }
      }

      if (entity.level() instanceof ServerLevel level && entity.tickCount % 8 == 0) {
         level.sendParticles(VIOLET_DUST, entity.getX(), entity.getY() + entity.getBbHeight() * 0.72, entity.getZ(), 6, 0.22, 0.12, 0.22, 0.0);
      }
      return true;
   }
}
