package net.xxxjk.TYPE_MOON_WORLD.world.city;

import com.mojang.datafixers.util.Pair;
import java.util.function.Consumer;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import terrablender.api.Region;
import terrablender.api.RegionType;

public class CityRegion extends Region {
   public CityRegion(ResourceLocation name, int weight) { super(name, RegionType.OVERWORLD, weight); }

   @Override
   public void addBiomes(Registry<Biome> registry, Consumer<Pair<net.minecraft.world.level.biome.Climate.ParameterPoint, ResourceKey<Biome>>> mapper) {
      ResourceKey<Biome> city = ResourceKey.create(registry.key(), ResourceLocation.fromNamespaceAndPath("typemoonworld", "city"));
      addModifiedVanillaOverworldBiomes(mapper, builder -> builder.replaceBiome(Biomes.OLD_GROWTH_SPRUCE_TAIGA, city));
   }
}
