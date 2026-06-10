package net.xxxjk.TYPE_MOON_WORLD.world.dimension;

import java.util.UUID;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;

public class ModDimensions {
   public static final String UBW_INSTANCE_PATH_PREFIX = "ubw_instance/";
   public static final ResourceKey<Level> UBW_KEY = ResourceKey.create(
      Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath("typemoonworld", "unlimited_blade_works")
   );
   public static final ResourceKey<DimensionType> UBW_TYPE = ResourceKey.create(
      Registries.DIMENSION_TYPE, ResourceLocation.fromNamespaceAndPath("typemoonworld", "unlimited_blade_works")
   );

   public static void register() {
      TYPE_MOON_WORLD.LOGGER.debug("Registering ModDimensions for {}", "typemoonworld");
   }

   public static ResourceLocation ubwInstanceId(UUID ownerId) {
      return ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, UBW_INSTANCE_PATH_PREFIX + ownerId.toString().replace("-", "_"));
   }

   public static ResourceLocation ubwInstanceId(UUID ownerId, int generation) {
      return ResourceLocation.fromNamespaceAndPath(
         TYPE_MOON_WORLD.MOD_ID, UBW_INSTANCE_PATH_PREFIX + ownerId.toString().replace("-", "_") + "_" + generation
      );
   }

   public static ResourceKey<Level> ubwInstanceKey(UUID ownerId) {
      return ResourceKey.create(Registries.DIMENSION, ubwInstanceId(ownerId));
   }

   public static ResourceKey<Level> ubwInstanceKey(UUID ownerId, int generation) {
      return ResourceKey.create(Registries.DIMENSION, ubwInstanceId(ownerId, generation));
   }

   public static boolean isUbwInstance(ResourceLocation location) {
      return TYPE_MOON_WORLD.MOD_ID.equals(location.getNamespace()) && location.getPath().startsWith(UBW_INSTANCE_PATH_PREFIX);
   }

   public static boolean isUbwDimension(ResourceLocation location) {
      return UBW_KEY.location().equals(location) || isUbwInstance(location);
   }
}
