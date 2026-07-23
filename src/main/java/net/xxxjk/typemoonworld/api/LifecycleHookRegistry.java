package net.xxxjk.typemoonworld.api;

import net.minecraft.resources.ResourceLocation;

public interface LifecycleHookRegistry {
   boolean register(ResourceLocation id, LifecycleHook hook);
}
