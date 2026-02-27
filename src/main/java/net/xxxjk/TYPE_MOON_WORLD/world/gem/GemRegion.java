package net.xxxjk.TYPE_MOON_WORLD.world.gem;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import terrablender.api.Region;
import terrablender.api.RegionType;

import java.util.function.Consumer;

public class GemRegion extends Region {
    public GemRegion(ResourceLocation name, int weight) {
        super(name, RegionType.OVERWORLD, weight);
    }

    @Override
    public void addBiomes(Registry<Biome> registry, Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> mapper) {
        ResourceKey<Biome> gemBiome = ResourceKey.create(registry.key(), ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "gem_biome"));

        this.addModifiedVanillaOverworldBiomes(mapper, builder -> {
            // Replace selected mountain biomes with gem biome
            builder.replaceBiome(net.minecraft.world.level.biome.Biomes.WINDSWEPT_HILLS, gemBiome);
            builder.replaceBiome(net.minecraft.world.level.biome.Biomes.STONY_PEAKS, gemBiome);
        });
    }
}
