package net.xxxjk.TYPE_MOON_WORLD.vfx.data;

import org.joml.Vector3f;

/** Optional motion hint for CPU-compatible ballistic/orbit/noise modules. */
public record VFXMotionDefinition(String type, float amount, float frequency, Vector3f axis) {
   public static final VFXMotionDefinition NONE = new VFXMotionDefinition("none", 0.0F, 0.0F, new Vector3f(0.0F, 1.0F, 0.0F));
   public VFXMotionDefinition {
      type = type == null || type.isBlank() ? "none" : type;
      axis = axis == null ? new Vector3f(0.0F, 1.0F, 0.0F) : new Vector3f(axis);
   }
}
