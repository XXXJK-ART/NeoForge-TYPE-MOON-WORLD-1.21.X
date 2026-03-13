package net.xxxjk.TYPE_MOON_WORLD.world.gem;

import com.mojang.datafixers.util.Pair;
import java.util.function.Consumer;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate.ParameterPoint;
import terrablender.api.Region;
import terrablender.api.RegionType;

public class GemRegion extends Region {
   public GemRegion(ResourceLocation name, int weight) {
      super(name, RegionType.OVERWORLD, weight);
   }

   public void addBiomes(Registry<Biome> registry, Consumer<Pair<ParameterPoint, ResourceKey<Biome>>> mapper) {
      ResourceKey<Biome> gemBiome = ResourceKey.create(registry.key(), ResourceLocation.fromNamespaceAndPath("typemoonworld", "gem_biome"));
      this.addModifiedVanillaOverworldBiomes(mapper, builder -> {
         builder.replaceBiome(Biomes.WINDSWEPT_HILLS, gemBiome);
         builder.replaceBiome(Biomes.STONY_PEAKS, gemBiome);
      });
   }
}
