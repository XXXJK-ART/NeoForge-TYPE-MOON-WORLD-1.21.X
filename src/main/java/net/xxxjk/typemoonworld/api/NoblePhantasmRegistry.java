package net.xxxjk.typemoonworld.api;

import net.minecraft.resources.ResourceLocation;

public interface NoblePhantasmRegistry {
   boolean register(ResourceLocation id, NoblePhantasmExecutor executor);
}
