package net.xxxjk.TYPE_MOON_WORLD.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.effect.NineLivesEffect;
import net.xxxjk.TYPE_MOON_WORLD.effect.ReinforcementEffect;
import net.xxxjk.TYPE_MOON_WORLD.effect.UncurableEffect;

public class ModMobEffects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, TYPE_MOON_WORLD.MOD_ID);

    public static final DeferredHolder<MobEffect, MobEffect> NINE_LIVES = MOB_EFFECTS.register("nine_lives",
            () -> new NineLivesEffect(MobEffectCategory.BENEFICIAL, 0x8B0000));

    public static final DeferredHolder<MobEffect, MobEffect> REINFORCEMENT_SELF_STRENGTH = MOB_EFFECTS.register("reinforcement_self_strength",
            () -> new ReinforcementEffect(MobEffectCategory.BENEFICIAL, 0x0000AA, ResourceLocation.withDefaultNamespace("textures/mob_effect/strength.png"))
                    .addAttributeModifier(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE, 
                        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "reinforcement_self_strength_damage"), 
                        3.0D, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE));

    public static final DeferredHolder<MobEffect, MobEffect> REINFORCEMENT_SELF_DEFENSE = MOB_EFFECTS.register("reinforcement_self_defense",
            () -> new ReinforcementEffect(MobEffectCategory.BENEFICIAL, 0x0000FF, ResourceLocation.withDefaultNamespace("textures/mob_effect/resistance.png"))
                    .addAttributeModifier(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR, 
                        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "reinforcement_self_defense_armor"),
                        2.0D, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE)
                    .addAttributeModifier(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS, 
                        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "reinforcement_self_defense_toughness"),
                        1.0D, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE));

    public static final DeferredHolder<MobEffect, MobEffect> REINFORCEMENT_SELF_AGILITY = MOB_EFFECTS.register("reinforcement_self_agility",
            () -> new ReinforcementEffect(MobEffectCategory.BENEFICIAL, 0x00AAAA, ResourceLocation.withDefaultNamespace("textures/mob_effect/speed.png"))
                    .addAttributeModifier(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED, 
                        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "reinforcement_self_agility_speed"),
                        0.2D, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
                    .addAttributeModifier(net.minecraft.world.entity.ai.attributes.Attributes.JUMP_STRENGTH, 
                        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "reinforcement_self_agility_jump"),
                        0.1D, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE));

    public static final DeferredHolder<MobEffect, MobEffect> REINFORCEMENT_SELF_SIGHT = MOB_EFFECTS.register("reinforcement_self_sight",
            () -> new ReinforcementEffect(MobEffectCategory.BENEFICIAL, 0xAAAAFF, ResourceLocation.withDefaultNamespace("textures/mob_effect/night_vision.png")));

    public static final DeferredHolder<MobEffect, MobEffect> REINFORCEMENT_OTHER_STRENGTH = MOB_EFFECTS.register("reinforcement_other_strength",
            () -> new ReinforcementEffect(MobEffectCategory.BENEFICIAL, 0x0000AA, ResourceLocation.withDefaultNamespace("textures/item/totem_of_undying.png"))
                    .addAttributeModifier(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE, 
                        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "reinforcement_other_strength_damage"), 
                        3.0D, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE));

    public static final DeferredHolder<MobEffect, MobEffect> REINFORCEMENT_OTHER_DEFENSE = MOB_EFFECTS.register("reinforcement_other_defense",
            () -> new ReinforcementEffect(MobEffectCategory.BENEFICIAL, 0x0000FF, ResourceLocation.withDefaultNamespace("textures/item/totem_of_undying.png"))
                    .addAttributeModifier(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR, 
                        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "reinforcement_other_defense_armor"),
                        2.0D, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE)
                    .addAttributeModifier(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS, 
                        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "reinforcement_other_defense_toughness"),
                        1.0D, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE));

    public static final DeferredHolder<MobEffect, MobEffect> REINFORCEMENT_OTHER_AGILITY = MOB_EFFECTS.register("reinforcement_other_agility",
            () -> new ReinforcementEffect(MobEffectCategory.BENEFICIAL, 0x00AAAA, ResourceLocation.withDefaultNamespace("textures/item/totem_of_undying.png"))
                    .addAttributeModifier(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED, 
                        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "reinforcement_other_agility_speed"),
                        0.2D, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
                    .addAttributeModifier(net.minecraft.world.entity.ai.attributes.Attributes.JUMP_STRENGTH, 
                        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "reinforcement_other_agility_jump"),
                        0.1D, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE));

    public static final DeferredHolder<MobEffect, MobEffect> REINFORCEMENT_OTHER_SIGHT = MOB_EFFECTS.register("reinforcement_other_sight",
            () -> new ReinforcementEffect(MobEffectCategory.BENEFICIAL, 0xAAAAFF, ResourceLocation.withDefaultNamespace("textures/item/totem_of_undying.png")));

    public static void register(IEventBus eventBus) {
        MOB_EFFECTS.register(eventBus);
    }
}
