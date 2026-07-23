package net.xxxjk.typemoonworld.api;

import net.minecraft.resources.ResourceLocation;

/** Client-only extension points. Implementations must not be loaded on a dedicated server. */
public interface ClientExtensionRegistry {
   boolean registerMagicOptions(ResourceLocation id, MagicOptionsExtension extension);
   boolean registerControl(ResourceLocation magicId, MagicOption option);
}
