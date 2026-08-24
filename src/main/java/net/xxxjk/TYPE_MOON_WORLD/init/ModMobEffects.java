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
import net.xxxjk.TYPE_MOON_WORLD.effect.PetrifiedEffect;
import net.xxxjk.TYPE_MOON_WORLD.effect.ReinforcementEffect;
import net.xxxjk.TYPE_MOON_WORLD.effect.BindingEffect;
import net.xxxjk.TYPE_MOON_WORLD.effect.SuggestionEffect;
import net.xxxjk.TYPE_MOON_WORLD.effect.ReverseMovementEffect;
import net.xxxjk.TYPE_MOON_WORLD.effect.MartialControlEffect;
import net.xxxjk.TYPE_MOON_WORLD.effect.PaleRiderFearEffect;
import net.xxxjk.TYPE_MOON_WORLD.effect.PaleRiderInfectionEffect;
import net.xxxjk.TYPE_MOON_WORLD.effect.FanaticCircuitDisruptionEffect;
import net.xxxjk.TYPE_MOON_WORLD.effect.FanaticToxinEffect;
import net.xxxjk.TYPE_MOON_WORLD.effect.FanaticWoundedEffect;
import net.xxxjk.TYPE_MOON_WORLD.effect.MonstrousStrengthEffect;
import net.xxxjk.TYPE_MOON_WORLD.effect.CremationRiteEffect;

public class ModMobEffects {
   public static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, "typemoonworld");
   public static final DeferredHolder<MobEffect, MobEffect> NINE_LIVES = MOB_EFFECTS.register(
      "nine_lives", () -> new NineLivesEffect(MobEffectCategory.BENEFICIAL, 9109504)
   );
   public static final DeferredHolder<MobEffect, MobEffect> MONSTROUS_STRENGTH = MOB_EFFECTS.register(
      "monstrous_strength",
      () -> new MonstrousStrengthEffect().addAttributeModifier(
         Attributes.ATTACK_DAMAGE,
         ResourceLocation.fromNamespaceAndPath("typemoonworld", "monstrous_strength_damage"),
         3.0,
         Operation.ADD_VALUE
      )
   );
   public static final DeferredHolder<MobEffect, MobEffect> PETRIFIED = MOB_EFFECTS.register(
      "petrified", () -> new PetrifiedEffect(MobEffectCategory.HARMFUL, 0x8E8E8E)
   );
   public static final DeferredHolder<MobEffect, MobEffect> BINDING = MOB_EFFECTS.register(
      "binding", () -> new BindingEffect(MobEffectCategory.HARMFUL, 0xFFD45A)
   );
   public static final DeferredHolder<MobEffect, MobEffect> SUGGESTION = MOB_EFFECTS.register(
      "suggestion", () -> new SuggestionEffect(MobEffectCategory.HARMFUL, 0xB65CFF)
   );
   public static final DeferredHolder<MobEffect, MobEffect> REVERSE_MOVEMENT = MOB_EFFECTS.register(
      "reverse_movement", () -> new ReverseMovementEffect(MobEffectCategory.HARMFUL, 0x7D5CFF)
   );
   public static final DeferredHolder<MobEffect, MobEffect> STAGGER = MOB_EFFECTS.register(
      "stagger", () -> new MartialControlEffect(MobEffectCategory.HARMFUL, 0xD5B868, true)
   );
   public static final DeferredHolder<MobEffect, MobEffect> OFF_BALANCE = MOB_EFFECTS.register(
      "off_balance", () -> new MartialControlEffect(MobEffectCategory.HARMFUL, 0x6F8F62, false)
   );
   public static final DeferredHolder<MobEffect, MobEffect> PALE_RIDER_INFECTION = MOB_EFFECTS.register(
      "pale_rider_infection", PaleRiderInfectionEffect::new
   );
   public static final DeferredHolder<MobEffect, MobEffect> PALE_RIDER_FEAR = MOB_EFFECTS.register(
      "pale_rider_fear", PaleRiderFearEffect::new
   );
   public static final DeferredHolder<MobEffect, MobEffect> FANATIC_WOUNDED = MOB_EFFECTS.register(
      "fanatic_wounded", FanaticWoundedEffect::new
   );
   public static final DeferredHolder<MobEffect, MobEffect> FANATIC_CIRCUIT_DISRUPTION = MOB_EFFECTS.register(
      "fanatic_circuit_disruption", FanaticCircuitDisruptionEffect::new
   );
   public static final DeferredHolder<MobEffect, MobEffect> FANATIC_TOXIN = MOB_EFFECTS.register(
      "fanatic_toxin", FanaticToxinEffect::new
   );
   public static final DeferredHolder<MobEffect, MobEffect> CREMATION_RITE = MOB_EFFECTS.register(
      "cremation_rite", CremationRiteEffect::new
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
