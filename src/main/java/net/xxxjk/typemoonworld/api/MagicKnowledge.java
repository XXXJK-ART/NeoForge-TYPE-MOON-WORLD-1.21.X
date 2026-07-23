package net.xxxjk.typemoonworld.api;

import net.minecraft.resources.ResourceLocation;

public interface MagicKnowledge {
   boolean isLearned(ResourceLocation magicId);
   boolean learn(ResourceLocation magicId);
   double proficiency(ResourceLocation magicId);
   void setProficiency(ResourceLocation magicId, double value);

   default void addProficiency(ResourceLocation magicId, double amount) {
      setProficiency(magicId, proficiency(magicId) + amount);
   }
}
