package net.xxxjk.TYPE_MOON_WORLD.servant.personality;

public enum ObedienceAxis {
   COMPLIANT(0, "compliant", 1.0),
   COOPERATIVE(1, "cooperative", 0.8),
   REBELLIOUS(2, "rebellious", 0.4);

   private final int id;
   private final String key;
   private final double baseRate;

   ObedienceAxis(int id, String key, double baseRate) {
      this.id = id;
      this.key = key;
      this.baseRate = baseRate;
   }

   public int id() {
      return this.id;
   }

   public String key() {
      return this.key;
   }

   public double baseRate() {
      return this.baseRate;
   }

   public static ObedienceAxis fromKey(String key) {
      if (key == null) {
         return COOPERATIVE;
      }

      for (ObedienceAxis axis : values()) {
         if (axis.key.equals(key.toLowerCase())) {
            return axis;
         }
      }

      return COOPERATIVE;
   }

   public static ObedienceAxis fromId(int id) {
      for (ObedienceAxis axis : values()) {
         if (axis.id == id) {
            return axis;
         }
      }

      return COOPERATIVE;
   }
}
