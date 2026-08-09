package net.xxxjk.TYPE_MOON_WORLD.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.neoforge.common.EffectCure;
import java.util.Set;

public final class MonstrousStrengthEffect extends MobEffect {
   public MonstrousStrengthEffect() {
      super(MobEffectCategory.BENEFICIAL, 0xB66CFF);
   }

   @Override
   public void fillEffectCures(Set<EffectCure> cures, MobEffectInstance effectInstance) {
      // This talent is intentionally immune to milk and every registered cure.
   }
}
