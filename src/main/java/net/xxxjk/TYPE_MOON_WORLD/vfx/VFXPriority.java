package net.xxxjk.TYPE_MOON_WORLD.vfx;

/** Rendering importance used by the adaptive client budget. */
public enum VFXPriority {
   CRITICAL, NORMAL, DECORATIVE;

   public static VFXPriority parse(String value) {
      if (value == null || value.isBlank()) return NORMAL;
      try { return valueOf(value.toUpperCase(java.util.Locale.ROOT)); }
      catch (IllegalArgumentException ignored) { return NORMAL; }
   }
}
