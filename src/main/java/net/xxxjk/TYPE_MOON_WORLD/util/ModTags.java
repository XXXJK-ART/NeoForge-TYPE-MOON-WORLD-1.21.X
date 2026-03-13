package net.xxxjk.TYPE_MOON_WORLD.util;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;

public final class ModTags {
   private ModTags() {
   }

   private static ResourceLocation id(String path) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", path);
   }

   public static final class Biomes {
      public static final TagKey<Biome> IS_GEM_TERRAIN = TagKey.create(Registries.BIOME, ModTags.id("is_gem_terrain"));
   }

   public static final class Blocks {
      public static final TagKey<Block> GEM_ORES = TagKey.create(Registries.BLOCK, ModTags.id("gem_ores"));
      public static final TagKey<Block> GEM_GEODE_BLOCKS = TagKey.create(Registries.BLOCK, ModTags.id("gem_geode_blocks"));
   }
}
