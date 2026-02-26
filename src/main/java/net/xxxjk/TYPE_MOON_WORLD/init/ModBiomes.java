package net.xxxjk.TYPE_MOON_WORLD.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;

public class ModBiomes {
    public static final DeferredRegister<Biome> BIOMES = DeferredRegister.create(Registries.BIOME, TYPE_MOON_WORLD.MOD_ID);

    public static final DeferredHolder<Biome, Biome> GEM_BIOME = BIOMES.register("gem_biome", ModBiomes::createGemBiome);

    private static Biome createGemBiome() {
        MobSpawnSettings mobSpawns = new MobSpawnSettings.Builder().build();
        BiomeGenerationSettings generation = new BiomeGenerationSettings.PlainBuilder().build();

        BiomeSpecialEffects effects = new BiomeSpecialEffects.Builder()
                .fogColor(12638463)
                .skyColor(7907327)
                .waterColor(4159204)
                .waterFogColor(329011)
                .foliageColorOverride(7923300)
                .grassColorOverride(8947848)
                .grassColorModifier(BiomeSpecialEffects.GrassColorModifier.NONE)
                .build();

        return new Biome.BiomeBuilder()
                .hasPrecipitation(true)
                .temperature(0.35F)
                .downfall(0.15F)
                .specialEffects(effects)
                .mobSpawnSettings(mobSpawns)
                .generationSettings(generation)
                .build();
    }

    public static void register(IEventBus eventBus) {
        BIOMES.register(eventBus);
    }
}

