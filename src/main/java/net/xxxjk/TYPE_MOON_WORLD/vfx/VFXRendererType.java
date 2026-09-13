package net.xxxjk.TYPE_MOON_WORLD.vfx;

/** Declarative renderer hint; unsupported hints safely fall back to billboards. */
public enum VFXRendererType {
   BILLBOARD, RIBBON, BEAM, RING, DECAL, SHADER_QUAD, VOLUME;

   public static VFXRendererType parse(String value) {
      if (value == null || value.isBlank()) return BILLBOARD;
      try { return valueOf(value.toUpperCase(java.util.Locale.ROOT)); }
      catch (IllegalArgumentException ignored) { return BILLBOARD; }
   }
}
