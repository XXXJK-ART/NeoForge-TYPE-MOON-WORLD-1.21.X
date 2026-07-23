package net.xxxjk.typemoonworld.api;

import net.minecraft.resources.ResourceLocation;

public interface ServantCardRegistry {
   boolean registerAction(ResourceLocation id, CardActionExecutor executor);
   boolean bindSlot(ResourceLocation servantId, int slot, ResourceLocation actionId);
   ResourceLocation actionForSlot(ResourceLocation servantId, int slot);
}
