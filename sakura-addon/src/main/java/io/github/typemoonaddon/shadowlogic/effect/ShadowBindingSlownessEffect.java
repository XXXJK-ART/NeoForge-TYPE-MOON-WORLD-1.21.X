package io.github.typemoonaddon.shadowlogic.effect;

import io.github.typemoonaddon.TypeMoonAddon;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Distance-scaled movement penalty applied while a target resists Shadow Binding. */
public final class ShadowBindingSlownessEffect extends MobEffect {
    public ShadowBindingSlownessEffect() {
        super(MobEffectCategory.HARMFUL, 0x27030A);
        this.addAttributeModifier(
            Attributes.MOVEMENT_SPEED,
            TypeMoonAddon.id("effect.shadow_binding_slowness"),
            -0.15D,
            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        );
        this.addAttributeModifier(
            Attributes.FLYING_SPEED,
            TypeMoonAddon.id("effect.shadow_binding_flying_slowness"),
            -0.15D,
            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        );
    }
}
