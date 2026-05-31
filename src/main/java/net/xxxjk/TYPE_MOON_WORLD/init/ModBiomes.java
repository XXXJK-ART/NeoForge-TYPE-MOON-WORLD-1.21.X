package net.xxxjk.TYPE_MOON_WORLD.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.biome.Biome.BiomeBuilder;
import net.minecraft.world.level.biome.BiomeGenerationSettings.PlainBuilder;
import net.minecraft.world.level.biome.BiomeSpecialEffects.GrassColorModifier;
import net.minecraft.world.level.biome.MobSpawnSettings.Builder;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBiomes {
   public static final DeferredRegister<Biome> BIOMES = DeferredRegister.create(Registries.BIOME, "typemoonworld");
   public static final DeferredHolder<Biome, Biome> GEM_BIOME = BIOMES.register("gem_biome", ModBiomes::createGemBiome);

   private static Biome createGemBiome() {
      MobSpawnSettings mobSpawns = new Builder().build();
      BiomeGenerationSettings generation = new PlainBuilder().build();
      BiomeSpecialEffects effects = new net.minecraft.world.level.biome.BiomeSpecialEffects.Builder()
         .fogColor(12638463)
         .skyColor(7907327)
         .waterColor(4159204)
         .waterFogColor(329011)
         .foliageColorOverride(7923300)
         .grassColorOverride(8947848)
         .grassColorModifier(GrassColorModifier.NONE)
         .build();
      return new BiomeBuilder()
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
