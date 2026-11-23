package net.xxxjk.TYPE_MOON_WORLD.servant.jeanne;

import java.util.Locale;

/** Pure shared values for Jeanne Alter and reusable Avenger class skills. */
public final class JeanneAlterRules {
   public static final int MAX_GRUDGE = 5;
   public static final int DEATH_LOCK_TICKS = 5 * 20;

   private static final RankBonus[] AVENGER = {
      new RankBonus(10, 4, 0.01), new RankBonus(20, 8, 0.02), new RankBonus(30, 12, 0.03),
      new RankBonus(40, 16, 0.04), new RankBonus(50, 20, 0.05)
   };
   private static final double[] OBLIVION = {0.10, 0.20, 0.30, 0.40, 0.50};
   private static final double[] REPLENISHMENT = {1, 2, 4, 6, 8};

   private JeanneAlterRules() {}

   public static RankBonus avenger(String rank) {
      return AVENGER[rankIndex(rank)];
   }

   public static double oblivionCritBonus(String rank) {
      return OBLIVION[rankIndex(rank)];
   }

   public static double replenishmentPerSecond(String rank) {
      return REPLENISHMENT[rankIndex(rank)];
   }

   public static double damage(double base, int grudge, double missingHealth, boolean jeanneSkill,
                               boolean dragonWitch, boolean ephemeralDream, boolean critical) {
      double grudgePerStack = jeanneSkill ? 0.10 : 0.05;
      double result = base * (1.0 + Math.min(MAX_GRUDGE, Math.max(0, grudge)) * grudgePerStack)
         * (1.0 + Math.max(0.0, Math.min(1.0, missingHealth)) * 0.50);
      if (dragonWitch) result *= 1.15;
      if (ephemeralDream) result *= 1.40;
      if (critical) result *= 2.77;
      return result;
   }

   private static int rankIndex(String rank) {
      return switch (rank.trim().toUpperCase(Locale.ROOT).replace("+", "").replace("-", "")) {
         case "E" -> 0;
         case "D" -> 1;
         case "C" -> 2;
         case "B" -> 3;
         case "A" -> 4;
         default -> throw new IllegalArgumentException("Unsupported shared class-skill rank: " + rank);
      };
   }

   public record RankBonus(int health, int attack, double movement) {}
}
