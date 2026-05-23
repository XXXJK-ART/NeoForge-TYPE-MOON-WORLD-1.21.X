package net.xxxjk.TYPE_MOON_WORLD.servant.personality;

public enum PrincipleAxis {
   ORDERLY(0, "orderly"),
   NEUTRAL(1, "neutral"),
   CHAOTIC(2, "chaotic");

   private final int id;
   private final String key;

   PrincipleAxis(int id, String key) {
      this.id = id;
      this.key = key;
   }

   public int id() {
      return this.id;
   }

   public String key() {
      return this.key;
   }

   public static PrincipleAxis fromKey(String key) {
      if (key == null) {
         return NEUTRAL;
      }

      for (PrincipleAxis axis : values()) {
         if (axis.key.equals(key.toLowerCase())) {
            return axis;
         }
      }

      return NEUTRAL;
   }

   public static PrincipleAxis fromId(int id) {
      for (PrincipleAxis axis : values()) {
         if (axis.id == id) {
            return axis;
         }
      }

      return NEUTRAL;
   }
}
