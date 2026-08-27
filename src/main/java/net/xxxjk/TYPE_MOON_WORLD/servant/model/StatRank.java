package net.xxxjk.TYPE_MOON_WORLD.servant.model;

public enum StatRank {
   E(10),
   D(20),
   C(30),
   B(40),
   A(50),
   A_PLUS_PLUS(150, "A++");

   private final int coefficient;
   private final String displayKey;

   StatRank(int coefficient) {
      this(coefficient, null);
   }

   StatRank(int coefficient, String displayKey) {
      this.coefficient = coefficient;
      this.displayKey = displayKey;
   }

   public int coefficient() {
      return this.coefficient;
   }

   public int plusCoefficient() {
      if (this == A_PLUS_PLUS) {
         // A++ is a single three-times rank, not A+ applied twice (four times).
         return this.coefficient;
      }
      return this.coefficient * 2;
   }

   /** Multiplier represented by this rank when it is used as a base parameter. */
   public double parameterMultiplier() {
      return this == A_PLUS_PLUS ? 3.0 : 1.0;
   }

   public String displayKey(boolean plus) {
      if (this.displayKey != null) {
         return this.displayKey;
      }
      return plus ? this.name() + "+" : this.name();
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

      String normalized = key.trim().toUpperCase().replace('＋', '+');
      if ("A++".equals(normalized) || "A_PLUS_PLUS".equals(normalized) || "A PLUS PLUS".equals(normalized)) {
         return A_PLUS_PLUS;
      }
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
