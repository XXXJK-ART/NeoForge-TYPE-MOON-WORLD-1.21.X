package net.xxxjk.TYPE_MOON_WORLD.vfx.data;

import net.minecraft.resources.ResourceLocation;

/** Safe material metadata; shader ids resolve through a client registry. */
public record VFXMaterialDefinition(String shader, ResourceLocation texture, float bloom, boolean softParticles, boolean distortion) {
   public static final VFXMaterialDefinition DEFAULT = new VFXMaterialDefinition("vanilla", null, 0.0F, false, false);
   public VFXMaterialDefinition {
      shader = shader == null || shader.isBlank() ? "vanilla" : shader;
      bloom = Math.max(0.0F, Math.min(4.0F, bloom));
   }
}
