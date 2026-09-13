package net.xxxjk.TYPE_MOON_WORLD.vfx.data;

/** Declarative screen-effect request. Rendering is gated by client quality settings. */
public record VFXScreenEffectDefinition(String type, float intensity, float duration, float radius) {
   public VFXScreenEffectDefinition {
      type = type == null ? "" : type;
      intensity = Math.max(0.0F, Math.min(1.0F, intensity));
      duration = Math.max(0.0F, duration);
      radius = Math.max(0.0F, radius);
   }
}
