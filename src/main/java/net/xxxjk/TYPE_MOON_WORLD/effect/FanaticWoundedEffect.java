package net.xxxjk.TYPE_MOON_WORLD.effect;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public final class FanaticWoundedEffect extends UncurableEffect {
   public FanaticWoundedEffect() {
      super(MobEffectCategory.HARMFUL, 0x7A1018);
      this.addAttributeModifier(Attributes.ATTACK_DAMAGE,
         ResourceLocation.fromNamespaceAndPath("typemoonworld", "fanatic_wounded_attack"),
         -0.20, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
      this.addAttributeModifier(Attributes.MOVEMENT_SPEED,
         ResourceLocation.fromNamespaceAndPath("typemoonworld", "fanatic_wounded_speed"),
         -0.20, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
   }
}
