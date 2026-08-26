package net.xxxjk.typemoonworld.api;

import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

/** Supplies addon-owned cast availability for both client presentation and server validation. */
public interface MagicAvailabilityProvider {
   Set<ResourceLocation> magicIds();

   boolean isAvailable(LivingEntity entity, ResourceLocation magicId);
}
