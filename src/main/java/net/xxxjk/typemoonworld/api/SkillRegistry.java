package net.xxxjk.typemoonworld.api;

import net.minecraft.resources.ResourceLocation;

public interface SkillRegistry {
   boolean register(ResourceLocation id, SkillExecutor executor);
}
