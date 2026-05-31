package net.xxxjk.TYPE_MOON_WORLD.servant.personality;

public enum CombatDisposition {
   CAUTIOUS(0, "cautious"),
   BALANCED(1, "balanced"),
   FRENZIED(2, "frenzied");

   private final int id;
   private final String key;

   CombatDisposition(int id, String key) {
      this.id = id;
      this.key = key;
   }

   public int id() {
      return this.id;
   }

   public String key() {
      return this.key;
   }

   public static CombatDisposition fromKey(String key) {
      if (key == null) {
         return BALANCED;
      }

      for (CombatDisposition disposition : values()) {
         if (disposition.key.equals(key.toLowerCase())) {
            return disposition;
         }
      }

      return BALANCED;
   }

   public static CombatDisposition fromId(int id) {
      for (CombatDisposition disposition : values()) {
         if (disposition.id == id) {
            return disposition;
         }
      }

      return BALANCED;
   }
}
