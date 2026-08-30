package net.xxxjk.typemoonworld.api;

import net.minecraft.resources.ResourceLocation;

public interface MagicKnowledge {
   boolean isLearned(ResourceLocation magicId);
   boolean learn(ResourceLocation magicId);
   /** Forgets one learned magic owned by the backing player state. */
   default boolean forget(ResourceLocation magicId) { return false; }
   /** Replaces one learned magic and all of its wheel references. */
   default boolean migrate(ResourceLocation fromMagicId, ResourceLocation toMagicId) { return false; }
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
