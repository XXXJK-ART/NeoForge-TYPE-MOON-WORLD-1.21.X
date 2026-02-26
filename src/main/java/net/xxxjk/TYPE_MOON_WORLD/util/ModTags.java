package net.xxxjk.TYPE_MOON_WORLD.util;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;

public final class ModTags {
    private ModTags() {}

    public static final class Blocks {
        public static final TagKey<Block> GEM_ORES = TagKey.create(Registries.BLOCK, id("gem_ores"));
        public static final TagKey<Block> GEM_GEODE_BLOCKS = TagKey.create(Registries.BLOCK, id("gem_geode_blocks"));
    }

    public static final class Biomes {
        public static final TagKey<Biome> IS_GEM_TERRAIN = TagKey.create(Registries.BIOME, id("is_gem_terrain"));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, path);
    }
}

