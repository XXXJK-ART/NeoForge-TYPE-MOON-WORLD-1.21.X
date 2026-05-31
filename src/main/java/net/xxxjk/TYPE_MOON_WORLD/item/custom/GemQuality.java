package net.xxxjk.TYPE_MOON_WORLD.item.custom;

public enum GemQuality {
   POOR(50, 0.8F, "poor"),
   NORMAL(100, 1.0F, "normal"),
   HIGH(200, 1.5F, "high");

   private final int capacity;
   private final float effectMultiplier;
   private final String name;

   private GemQuality(int capacity, float effectMultiplier, String name) {
      this.capacity = capacity;
      this.effectMultiplier = effectMultiplier;
      this.name = name;
   }

   public int getCapacity() {
      return this.capacity;
   }

   public int getCapacity(GemType type) {
      if (type == GemType.WHITE_GEMSTONE) {
         return switch (this) {
            case POOR -> 100;
            case NORMAL -> 200;
            case HIGH -> 300;
         };
      } else {
         return this.capacity;
      }
   }

   public float getEffectMultiplier() {
      return this.effectMultiplier;
   }

   public String getName() {
      return this.name;
   }
}
