package net.xxxjk.TYPE_MOON_WORLD.passive;

import java.util.Locale;

public enum PassiveRank {
   E(20.0, 4.0, 10.0, 2.0, 0.10),
   D(40.0, 6.0, 20.0, 4.0, 0.175),
   C(60.0, 8.0, 30.0, 6.0, 0.25),
   B(80.0, 10.0, 40.0, 8.0, 0.325),
   A(100.0, 12.0, 50.0, 10.0, 0.40);

   private final double clairvoyanceProficiency;
   private final double maxZoom;
   private final double healthBonus;
   private final double attackBonus;
   private final double dodgeChance;

   PassiveRank(double clairvoyanceProficiency, double maxZoom, double healthBonus, double attackBonus, double dodgeChance) {
      this.clairvoyanceProficiency = clairvoyanceProficiency;
      this.maxZoom = maxZoom;
      this.healthBonus = healthBonus;
      this.attackBonus = attackBonus;
      this.dodgeChance = dodgeChance;
   }

   public double clairvoyanceProficiency() {
      return this.clairvoyanceProficiency;
   }

   public double maxZoom() {
      return this.maxZoom;
   }

   public double healthBonus() {
      return this.healthBonus;
   }

   public double attackBonus() {
      return this.attackBonus;
   }

   public double dodgeChance() {
      return this.dodgeChance;
   }

   public PassiveRank next() {
      int next = this.ordinal() + 1;
      return next < values().length ? values()[next] : this;
   }

   public static PassiveRank parse(String value) {
      if (value == null || value.isBlank()) return null;
      try {
         return valueOf(value.trim().toUpperCase(Locale.ROOT));
      } catch (IllegalArgumentException ignored) {
         return null;
      }
   }
}
