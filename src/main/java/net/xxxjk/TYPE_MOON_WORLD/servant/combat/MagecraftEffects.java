package net.xxxjk.TYPE_MOON_WORLD.servant.combat;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.xxxjk.typemoonworld.api.MagecraftDamageSource;
import net.xxxjk.typemoonworld.api.MagicComplexity;

/** Explicit target effects; vanilla potions never enter this path. */
public final class MagecraftEffects {
   private MagecraftEffects() { }

   public static boolean add(LivingEntity target, MobEffectInstance effect, String magicId) {
      return add(target, effect, MagecraftDamageSource.resolveComplexity(magicId));
   }

   public static boolean add(LivingEntity target, MobEffectInstance effect, MagicComplexity complexity) {
      if (effect.getEffect().value().getCategory() == net.minecraft.world.effect.MobEffectCategory.BENEFICIAL) return target.addEffect(effect);
      return addHarmful(target, effect, complexity);
   }

   /** Also supports control effects represented by a normally beneficial effect, such as Jump Boost 128. */
   public static boolean addHarmful(LivingEntity target, MobEffectInstance effect, MagicComplexity complexity) {
      int ticks = MagicResistanceHelper.applyHarmfulMagicEffectResistance(target, effect.getDuration(), complexity);
      return ticks > 0 && target.addEffect(new MobEffectInstance(effect.getEffect(), ticks, effect.getAmplifier(),
         effect.isAmbient(), effect.isVisible(), effect.showIcon()));
   }
}
