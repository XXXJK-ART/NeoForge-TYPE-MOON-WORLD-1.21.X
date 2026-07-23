package net.xxxjk.typemoonworld.api;

import java.util.Set;
import net.minecraft.resources.ResourceLocation;

public interface GemRegistry {
   boolean registerMagic(ResourceLocation magicId, GemAffinity affinity);
   boolean isCompatible(ResourceLocation magicId);
   int calculateSuccess(ResourceLocation magicId, GemType gemType, GemQuality quality, double proficiency);
   Set<ResourceLocation> registeredMagics();
}
