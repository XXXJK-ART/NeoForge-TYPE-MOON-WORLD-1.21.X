package net.xxxjk.TYPE_MOON_WORLD.world.dimension;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;

public class ModDimensions {
   public static final ResourceKey<Level> UBW_KEY = ResourceKey.create(
      Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath("typemoonworld", "unlimited_blade_works")
   );
   public static final ResourceKey<Level> EMIYA_UBW_KEY = ResourceKey.create(
      Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath("typemoonworld", "unlimited_blade_works_emiya")
   );
   public static final ResourceKey<DimensionType> UBW_TYPE = ResourceKey.create(
      Registries.DIMENSION_TYPE, ResourceLocation.fromNamespaceAndPath("typemoonworld", "unlimited_blade_works")
   );

   public static void register() {
      TYPE_MOON_WORLD.LOGGER.debug("Registering ModDimensions for {}", "typemoonworld");
   }

   public static boolean isPlayerUbwDimension(ResourceLocation location) {
      return UBW_KEY.location().equals(location);
   }

   public static boolean isEmiyaUbwDimension(ResourceLocation location) {
      return EMIYA_UBW_KEY.location().equals(location);
   }

   public static boolean isUbwDimension(ResourceLocation location) {
      return isPlayerUbwDimension(location) || isEmiyaUbwDimension(location);
   }
}
