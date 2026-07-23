package net.xxxjk.typemoonworld.api;

import java.util.Collection;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

/** Read-only view of the magic attributes currently owned by an entity. */
public interface MagicAttributeAccess {
   boolean has(ResourceLocation attribute);

   Set<ResourceLocation> attributes();

   default boolean hasAll(Collection<ResourceLocation> required) {
      return required == null || required.stream().allMatch(this::has);
   }
}
