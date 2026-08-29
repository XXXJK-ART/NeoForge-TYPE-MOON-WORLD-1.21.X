package net.xxxjk.typemoonworld.api;

import net.minecraft.resources.ResourceLocation;

public interface MagicKnowledge {
   boolean isLearned(ResourceLocation magicId);
   boolean learn(ResourceLocation magicId);
   /** Learns every magic definition owned by the registrar namespace. */
   default int learnAll() { return 0; }
   /** Forgets every learned magic owned by the registrar namespace. */
   default int forgetAll() { return 0; }
   double proficiency(ResourceLocation magicId);
   void setProficiency(ResourceLocation magicId, double value);

   default void addProficiency(ResourceLocation magicId, double amount) {
      setProficiency(magicId, proficiency(magicId) + amount);
   }
}
