package net.xxxjk.TYPE_MOON_WORLD.effect;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class BindingEffect extends UncurableEffect {
   public static final String TAG_FULL_BIND = "TypeMoonBindingFull";
   private static final DustParticleOptions GOLD_DUST = new DustParticleOptions(new Vector3f(1.0F, 0.78F, 0.18F), 1.0F);

   public BindingEffect(MobEffectCategory category, int color) {
      super(category, color);
   }

   @Override
   public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
      return true;
   }

   @Override
   public boolean applyEffectTick(LivingEntity entity, int amplifier) {
      boolean fullBind = amplifier > 0 || entity.getPersistentData().getBoolean(TAG_FULL_BIND);
      if (fullBind) {
         entity.setDeltaMovement(Vec3.ZERO);
         entity.hurtMarked = true;
         entity.stopUsingItem();
         if (entity instanceof Mob mob) {
            mob.getNavigation().stop();
         }
      } else {
         entity.setDeltaMovement(entity.getDeltaMovement().multiply(0.5, 1.0, 0.5));
         entity.hurtMarked = true;
      }

      if (entity.level() instanceof ServerLevel level && entity.tickCount % 5 == 0) {
         level.sendParticles(GOLD_DUST, entity.getX(), entity.getY() + entity.getBbHeight() * 0.25, entity.getZ(), 8, 0.35, 0.12, 0.35, 0.0);
         level.sendParticles(GOLD_DUST, entity.getX(), entity.getY() + entity.getBbHeight() * 0.75, entity.getZ(), fullBind ? 8 : 3, 0.28, 0.2, 0.28, 0.0);
      }
      return true;
   }
}
