package net.xxxjk.typemoonworld.api;

import net.minecraft.resources.ResourceLocation;
import java.util.List;

public interface AiTacticsRegistry {
   boolean register(ResourceLocation id, AiTacticProfile profile);
   AiTacticProfile profile(ResourceLocation id);
   List<ResourceLocation> profiles();
   AiTactic choose(ResourceLocation id, double distance, double healthRatio, java.util.Random random);
}
