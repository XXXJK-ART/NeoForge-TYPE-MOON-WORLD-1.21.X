package com.example.typemoonaddon.effect;

import com.example.typemoonaddon.TypeMoonAddon;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public final class DemonGodCurseEffect extends MobEffect {
    private static final ResourceLocation SPEED_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(
            TypeMoonAddon.MOD_ID, "demon_god_curse_slowdown");

    public DemonGodCurseEffect() {
        super(MobEffectCategory.HARMFUL, 0x5A0712);
        addAttributeModifier(
                Attributes.MOVEMENT_SPEED,
                SPEED_MODIFIER_ID,
                -0.15D,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        );
    }
}
