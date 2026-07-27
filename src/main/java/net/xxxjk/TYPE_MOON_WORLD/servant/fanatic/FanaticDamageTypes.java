package net.xxxjk.TYPE_MOON_WORLD.servant.fanatic;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;

public final class FanaticDamageTypes {
   public static final ResourceKey<DamageType> HEARTBEAT = key("fanatic_heartbeat");
   public static final ResourceKey<DamageType> MARROW = key("fanatic_marrow");
   public static final ResourceKey<DamageType> COMPUTER = key("fanatic_computer");
   public static final ResourceKey<DamageType> COMPUTER_SPLASH = key("fanatic_computer_splash");
   public static final ResourceKey<DamageType> TOXIN = key("fanatic_toxin");
   public static final ResourceKey<DamageType> JINN = key("fanatic_jinn");
   public static final TagKey<DamageType> MENTAL_ATTACKS = TagKey.create(
      Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "mental_attacks"));
   public static final TagKey<DamageType> GUARANTEED_HITS = TagKey.create(
      Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "fanatic_guaranteed_hits"));
   public static final TagKey<DamageType> BYPASSES_DEFENSES = TagKey.create(
      Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "fanatic_bypasses_defenses"));

   private FanaticDamageTypes() {
   }

   private static ResourceKey<DamageType> key(String path) {
      return ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, path));
   }
}
