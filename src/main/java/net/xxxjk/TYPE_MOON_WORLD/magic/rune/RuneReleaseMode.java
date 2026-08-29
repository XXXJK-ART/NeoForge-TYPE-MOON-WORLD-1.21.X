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
         switch (value.toLowerCase(java.util.Locale.ROOT)) {
            case "full_release", "complete_release" -> { return DIRECT_AIR; }
            case "enchantment", "enchant" -> { return WEAPON; }
            case "reinforcement", "reinforce" -> { return ARMOR; }
            case "inscription", "engraving" -> { return RUNE_STONE; }
            default -> { }
         }
         for (RuneReleaseMode mode : values()) {
            if (mode.name().equalsIgnoreCase(value)) return mode;
         }
      }
      return DIRECT_AIR;
   }
}
