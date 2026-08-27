package net.xxxjk.TYPE_MOON_WORLD.magic.rune;

public enum RuneReleaseMode {
   DIRECT_AIR,
   RUNE_STONE,
   WEAPON,
   ARMOR,
   TOOL,
   BLOCK_TRAP,
   BODY;

   public static RuneReleaseMode byName(String value) {
      if (value != null) {
         for (RuneReleaseMode mode : values()) {
            if (mode.name().equalsIgnoreCase(value)) return mode;
         }
      }
      return DIRECT_AIR;
   }
}
