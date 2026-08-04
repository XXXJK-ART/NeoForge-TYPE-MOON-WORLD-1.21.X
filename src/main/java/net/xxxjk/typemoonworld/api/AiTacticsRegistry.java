package net.xxxjk.typemoonworld.api;

import net.minecraft.resources.ResourceLocation;
import java.util.List;

public interface AiTacticsRegistry {
   boolean register(ResourceLocation id, AiTacticProfile profile);
   default boolean registerAdvanced(ResourceLocation id, AdvancedAiTacticProfile profile) {
      return profile != null && register(id, profile.base());
   }
   AiTacticProfile profile(ResourceLocation id);
   default AdvancedAiTacticProfile advancedProfile(ResourceLocation id) {
      AiTacticProfile base = profile(id);
      return base == null ? null : AdvancedAiTacticProfile.compatible(base);
   }
   List<ResourceLocation> profiles();
   AiTactic choose(ResourceLocation id, double distance, double healthRatio, java.util.Random random);
}
