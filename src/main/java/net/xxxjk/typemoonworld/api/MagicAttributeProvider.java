package net.xxxjk.typemoonworld.api;

import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

/** Supplies addon-owned magic attribute truth without exposing core attachments. */
public interface MagicAttributeProvider {
   boolean has(LivingEntity entity, ResourceLocation attribute);

   Set<ResourceLocation> attributes(LivingEntity entity);
}
