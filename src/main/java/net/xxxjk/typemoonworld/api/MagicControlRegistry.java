package net.xxxjk.typemoonworld.api;

import net.minecraft.resources.ResourceLocation;

public interface MagicControlRegistry {
   boolean register(ResourceLocation magicId, MagicOption option);
}
