package net.xxxjk.TYPE_MOON_WORLD.servant.palerider;

public final class InfectionRules {
   public static final int MAX_LEVEL = 5;
   public static final int DURATION_TICKS = 30 * 20;
   public static final int IMMUNITY_TICKS = 30 * 20;
   private static final double[] SPREAD = {0.20, 0.30, 0.40, 0.50, 0.60};
   private static final double[] CONTROL = {0.10, 0.25, 0.40, 0.60, 0.80};

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
}
