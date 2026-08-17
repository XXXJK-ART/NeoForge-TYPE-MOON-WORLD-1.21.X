package com.example.typemoonaddon.registry;

import com.example.typemoonaddon.TypeMoonAddon;
import com.example.typemoonaddon.effect.DemonGodCurseEffect;
import com.example.typemoonaddon.effect.SakuraSimpleEffect;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public final class AddonMobEffects {
    private static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, TypeMoonAddon.MOD_ID);

    public static final DeferredHolder<MobEffect, DemonGodCurseEffect> DEMON_GOD_CURSE =
            MOB_EFFECTS.register("demon_god_curse", DemonGodCurseEffect::new);
    public static final DeferredHolder<MobEffect, MobEffect> BANISHMENT =
            MOB_EFFECTS.register("banishment", () -> new SakuraSimpleEffect(MobEffectCategory.HARMFUL, 0x29143D));
    public static final DeferredHolder<MobEffect, MobEffect> BLACK_MUD_CORRUPTION =
            MOB_EFFECTS.register("black_mud_corruption", () -> new SakuraSimpleEffect(MobEffectCategory.HARMFUL, 0x1A0610));
    public static final DeferredHolder<MobEffect, MobEffect> SPIRITUAL_DAMAGE =
            MOB_EFFECTS.register("spiritual_damage", () -> new SakuraSimpleEffect(MobEffectCategory.HARMFUL, 0x702060));
    public static final DeferredHolder<MobEffect, MobEffect> SHADOW_BINDING_SLOWNESS =
            MOB_EFFECTS.register("shadow_binding_slowness", () -> new SakuraSimpleEffect(MobEffectCategory.HARMFUL, 0x12051F)
                    .addAttributeModifier(
                            Attributes.MOVEMENT_SPEED,
                            ResourceLocation.fromNamespaceAndPath(TypeMoonAddon.MOD_ID, "effect.shadow_binding_slowness"),
                            -1.0D,
                            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
                    )
                    .addAttributeModifier(
                            Attributes.FLYING_SPEED,
                            ResourceLocation.fromNamespaceAndPath(TypeMoonAddon.MOD_ID, "effect.shadow_binding_flying_slowness"),
                            -1.0D,
                            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
                    ));

    private AddonMobEffects() {
    }

    public static void register(IEventBus modEventBus) {
        MOB_EFFECTS.register(modEventBus);
    }
}
