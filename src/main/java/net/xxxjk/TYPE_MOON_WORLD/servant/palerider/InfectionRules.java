package net.xxxjk.TYPE_MOON_WORLD.servant.palerider;

public final class InfectionRules {
   public static final int MAX_LEVEL = 5;
   public static final int DURATION_TICKS = 30 * 20;
   public static final int IMMUNITY_TICKS = 30 * 20;
   private static final double[] SPREAD = {0.20, 0.30, 0.40, 0.50, 0.60};
   private static final double[] CONTROL = {0.10, 0.25, 0.40, 0.60, 0.80};
   private static final double[] ORDINARY_CONTROL = {0.60, 0.85, 0.95, 1.00, 1.00};
   private static final double[] VANILLA_CONTROL = {0.80, 0.95, 1.00, 1.00, 1.00};

   private InfectionRules() {
   }

   public static int clampLevel(int level) {
      return Math.max(1, Math.min(MAX_LEVEL, level));
   }

   public static double spreadChance(int level) {
      return SPREAD[clampLevel(level) - 1];
   }

   public static double controlChance(int level) {
      return CONTROL[clampLevel(level) - 1];
   }

   public static double ordinaryControlChance(int level, boolean vanillaEntity) {
      double[] chances = vanillaEntity ? VANILLA_CONTROL : ORDINARY_CONTROL;
      return chances[clampLevel(level) - 1];
   }
}
