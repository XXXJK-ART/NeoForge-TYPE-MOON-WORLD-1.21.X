package net.xxxjk.TYPE_MOON_WORLD.servant.sasaki;

import java.util.Locale;

public final class SasakiKojiroRules {
   public enum Technique {
      CROUCH_CUTBACK(-1, "sasaki_crouch_cutback", 8.0, 20, 3.0, 8.0F),
      VERTICAL_SLASH(1, "sasaki_vertical_slash", 12.0, 80, 3.5, 20.0F),
      DOWNWARD_SLASH(2, "sasaki_downward_slash", 8.0, 60, 3.0, 16.0F),
      UPWARD_SLASH(3, "sasaki_upward_slash", 10.0, 80, 3.0, 18.0F),
      HORIZONTAL_SLASH(4, "sasaki_horizontal_slash", 12.0, 80, 3.5, 20.0F),
      THRUST(6, "sasaki_thrust", 14.0, 100, 4.5, 24.0F),
      SPINNING_SLASH(7, "sasaki_spinning_slash", 16.0, 140, 4.0, 14.0F),
      COUNTER_STANCE(8, "sasaki_counter_stance", 18.0, 180, 3.0, 24.0F),
      TSUBAME_GAESHI(9, "sasaki_tsubame_gaeshi", 20.0, 600, 8.0, 450.0F);

      private final int slot;
      private final String effectId;
      private final double mpCost;
      private final int cooldownTicks;
      private final double range;
      private final float damage;

      Technique(int slot, String effectId, double mpCost, int cooldownTicks, double range, float damage) {
         this.slot = slot;
         this.effectId = effectId;
         this.mpCost = mpCost;
         this.cooldownTicks = cooldownTicks;
         this.range = range;
         this.damage = damage;
      }

      public int slot() { return slot; }
      public String effectId() { return effectId; }
      public double mpCost() { return mpCost; }
      public int cooldownTicks() { return cooldownTicks; }
      public double range() { return range; }
      public double cardRange() { return this == TSUBAME_GAESHI ? range : 4.0; }
      public float damage() { return damage; }

      public static Technique forSlot(int slot) {
         for (Technique technique : values()) if (technique.slot == slot) return technique;
         return null;
      }

      public static Technique forEffect(String effectId) {
         for (Technique technique : values()) if (technique.effectId.equals(effectId)) return technique;
         return null;
      }
   }

   private SasakiKojiroRules() { }

   public static double souwaDefenseBypassChance(String rank) {
      if (rank == null || rank.isBlank()) return 0.0;
      return switch (rank.trim().toUpperCase(Locale.ROOT)) {
         case "E" -> 0.01;
         case "D" -> 0.05;
         case "C" -> 0.10;
         case "B" -> 0.15;
         case "A", "A+", "EX" -> 0.20;
         default -> 0.0;
      };
   }

   public static double souwaBonus(String rank) {
      if (rank == null || rank.isBlank()) return 0.0;
      return switch (rank.trim().toUpperCase(Locale.ROOT)) {
         case "E" -> 0.05;
         case "D" -> 0.10;
         case "C" -> 0.15;
         case "B" -> 0.20;
         case "A", "A+", "EX" -> 0.25;
         default -> 0.0;
      };
   }

   public static int concealmentParticles(String rank) {
      if (rank == null) return 0;
      return switch (rank.trim().toUpperCase(Locale.ROOT)) {
         case "E" -> 25;
         case "D" -> 6;
         case "C" -> 15;
         case "B" -> 10;
         case "A" -> 5;
         case "A+", "EX" -> 0;
         default -> 0;
      };
   }

   public static int concealmentParticleInterval(String rank) {
      return "D".equals(rank == null ? "" : rank.trim().toUpperCase(Locale.ROOT)) ? 5 : 10;
   }

   public static double guardStaminaCost(double baseLoss, double defenderReduction,
                                         boolean activeGuard, boolean perfectParry,
                                         double attackerSouwaBonus) {
      if (perfectParry || baseLoss <= 0.0) return 0.0;
      double reduced = baseLoss * (1.0 - Math.min(0.25, Math.max(0.0, defenderReduction)));
      return reduced * (activeGuard ? 1.0 : 2.0) * (1.0 + Math.max(0.0, attackerSouwaBonus));
   }
}
