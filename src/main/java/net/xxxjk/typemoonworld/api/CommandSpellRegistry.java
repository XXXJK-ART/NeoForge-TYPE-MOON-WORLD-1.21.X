package net.xxxjk.typemoonworld.api;

import net.minecraft.resources.ResourceLocation;

public interface CommandSpellRegistry {
   boolean register(ResourceLocation id, CommandSpellExecutor executor);
   ExecutionResult execute(ResourceLocation id, CommandSpellContext context);
}
