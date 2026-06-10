package net.xxxjk.TYPE_MOON_WORLD.servant.model;

public enum StatRank {
   E(10),
   D(20),
   C(30),
   B(40),
   A(50);

   private final int coefficient;

   StatRank(int coefficient) {
      this.coefficient = coefficient;
   }

   public int coefficient() {
      return this.coefficient;
   }

   public int plusCoefficient() {
      return this.coefficient * 2;
   }

   public double toMaxHealth() {
      return this.coefficient * 10.0;
   }

   public double toAttackDamage() {
      return this.coefficient * 0.5;
   }

   public double toMovementSpeed() {
      return 0.16 + this.coefficient * 0.004;
   }

   public double toArmor() {
      return this.coefficient * 0.4;
   }

   public double toManaPool() {
      return this.coefficient * 20.0;
   }

   public double toCritRatePercent() {
      return this.coefficient * 0.2;
   }

   public static StatRank fromKey(String key) {
      if (key == null) {
         return E;
      }

      String normalized = key.trim().toUpperCase();
      if (normalized.endsWith("+")) {
         normalized = normalized.substring(0, normalized.length() - 1);
      }

      for (StatRank rank : values()) {
         if (rank.name().equals(normalized)) {
            return rank;
         }
      }

      return E;
   }
}
