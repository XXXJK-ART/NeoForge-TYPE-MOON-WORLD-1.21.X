package net.xxxjk.TYPE_MOON_WORLD.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.xxxjk.TYPE_MOON_WORLD.effect.NineLivesEffect;
import net.xxxjk.TYPE_MOON_WORLD.effect.ReinforcementEffect;

public class ModMobEffects {
   public static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, "typemoonworld");
   public static final DeferredHolder<MobEffect, MobEffect> NINE_LIVES = MOB_EFFECTS.register(
      "nine_lives", () -> new NineLivesEffect(MobEffectCategory.BENEFICIAL, 9109504)
   );
   public static final DeferredHolder<MobEffect, MobEffect> REINFORCEMENT_SELF_STRENGTH = MOB_EFFECTS.register(
      "reinforcement_self_strength",
      () -> new ReinforcementEffect(MobEffectCategory.BENEFICIAL, 170, ResourceLocation.withDefaultNamespace("textures/mob_effect/strength.png"))
         .addAttributeModifier(
            Attributes.ATTACK_DAMAGE, ResourceLocation.fromNamespaceAndPath("typemoonworld", "reinforcement_self_strength_damage"), 3.0, Operation.ADD_VALUE
         )
   );
   public static final DeferredHolder<MobEffect, MobEffect> REINFORCEMENT_SELF_DEFENSE = MOB_EFFECTS.register(
      "reinforcement_self_defense",
      () -> new ReinforcementEffect(MobEffectCategory.BENEFICIAL, 255, ResourceLocation.withDefaultNamespace("textures/mob_effect/resistance.png"))
         .addAttributeModifier(
            Attributes.ARMOR, ResourceLocation.fromNamespaceAndPath("typemoonworld", "reinforcement_self_defense_armor"), 2.0, Operation.ADD_VALUE
         )
         .addAttributeModifier(
            Attributes.ARMOR_TOUGHNESS,
            ResourceLocation.fromNamespaceAndPath("typemoonworld", "reinforcement_self_defense_toughness"),
            1.0,
            Operation.ADD_VALUE
         )
   );
   public static final DeferredHolder<MobEffect, MobEffect> REINFORCEMENT_SELF_AGILITY = MOB_EFFECTS.register(
      "reinforcement_self_agility",
      () -> new ReinforcementEffect(MobEffectCategory.BENEFICIAL, 43690, ResourceLocation.withDefaultNamespace("textures/mob_effect/speed.png"))
         .addAttributeModifier(
            Attributes.MOVEMENT_SPEED,
            ResourceLocation.fromNamespaceAndPath("typemoonworld", "reinforcement_self_agility_speed"),
            0.2,
            Operation.ADD_MULTIPLIED_TOTAL
         )
         .addAttributeModifier(
            Attributes.JUMP_STRENGTH, ResourceLocation.fromNamespaceAndPath("typemoonworld", "reinforcement_self_agility_jump"), 0.1, Operation.ADD_VALUE
         )
   );
   public static final DeferredHolder<MobEffect, MobEffect> REINFORCEMENT_SELF_SIGHT = MOB_EFFECTS.register(
      "reinforcement_self_sight",
      () -> new ReinforcementEffect(MobEffectCategory.BENEFICIAL, 11184895, ResourceLocation.withDefaultNamespace("textures/mob_effect/night_vision.png"))
   );
   public static final DeferredHolder<MobEffect, MobEffect> REINFORCEMENT_OTHER_STRENGTH = MOB_EFFECTS.register(
      "reinforcement_other_strength",
      () -> new ReinforcementEffect(MobEffectCategory.BENEFICIAL, 170, ResourceLocation.withDefaultNamespace("textures/item/totem_of_undying.png"))
         .addAttributeModifier(
            Attributes.ATTACK_DAMAGE, ResourceLocation.fromNamespaceAndPath("typemoonworld", "reinforcement_other_strength_damage"), 3.0, Operation.ADD_VALUE
         )
   );
   public static final DeferredHolder<MobEffect, MobEffect> REINFORCEMENT_OTHER_DEFENSE = MOB_EFFECTS.register(
      "reinforcement_other_defense",
      () -> new ReinforcementEffect(MobEffectCategory.BENEFICIAL, 255, ResourceLocation.withDefaultNamespace("textures/item/totem_of_undying.png"))
         .addAttributeModifier(
            Attributes.ARMOR, ResourceLocation.fromNamespaceAndPath("typemoonworld", "reinforcement_other_defense_armor"), 2.0, Operation.ADD_VALUE
         )
         .addAttributeModifier(
            Attributes.ARMOR_TOUGHNESS,
            ResourceLocation.fromNamespaceAndPath("typemoonworld", "reinforcement_other_defense_toughness"),
            1.0,
            Operation.ADD_VALUE
         )
   );
   public static final DeferredHolder<MobEffect, MobEffect> REINFORCEMENT_OTHER_AGILITY = MOB_EFFECTS.register(
      "reinforcement_other_agility",
      () -> new ReinforcementEffect(MobEffectCategory.BENEFICIAL, 43690, ResourceLocation.withDefaultNamespace("textures/item/totem_of_undying.png"))
         .addAttributeModifier(
            Attributes.MOVEMENT_SPEED,
            ResourceLocation.fromNamespaceAndPath("typemoonworld", "reinforcement_other_agility_speed"),
            0.2,
            Operation.ADD_MULTIPLIED_TOTAL
         )
         .addAttributeModifier(
            Attributes.JUMP_STRENGTH, ResourceLocation.fromNamespaceAndPath("typemoonworld", "reinforcement_other_agility_jump"), 0.1, Operation.ADD_VALUE
         )
   );
   public static final DeferredHolder<MobEffect, MobEffect> REINFORCEMENT_OTHER_SIGHT = MOB_EFFECTS.register(
      "reinforcement_other_sight",
      () -> new ReinforcementEffect(MobEffectCategory.BENEFICIAL, 11184895, ResourceLocation.withDefaultNamespace("textures/item/totem_of_undying.png"))
   );

   public static void register(IEventBus eventBus) {
      MOB_EFFECTS.register(eventBus);
   }
}
