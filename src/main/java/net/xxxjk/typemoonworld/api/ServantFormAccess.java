package net.xxxjk.typemoonworld.api;

import net.minecraft.resources.ResourceLocation;

/** Stable access to a player's servant-card transformation state. */
public interface ServantFormAccess {
   boolean transformed();
   ResourceLocation servantId();
   double mana();
   double maximumMana();
   boolean transform(ResourceLocation servantId);
   boolean release();
   boolean triggerAction(int slot);
}
