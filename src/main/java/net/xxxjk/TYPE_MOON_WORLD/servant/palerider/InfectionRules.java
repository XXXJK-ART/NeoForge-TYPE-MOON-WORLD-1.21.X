package net.xxxjk.TYPE_MOON_WORLD.servant.palerider;

public final class InfectionRules {
   public static final int MAX_LEVEL = 5;
   public static final int DURATION_TICKS = 5 * 20;
   public static final int IMMUNITY_TICKS = 30 * 20;
   public static final int DAMAGE_INTERVAL_TICKS = 20;
   public static final int SPREAD_INTERVAL_TICKS = 40;
   public static final int CONTROLLED_AI_INTERVAL_TICKS = 8;
   public static final int CONTROLLED_PARTICLE_INTERVAL_TICKS = 40;
   public static final int EFFECT_REFRESH_THRESHOLD_TICKS = 2 * 20;
   private static final double[] SPREAD = {0.20, 0.30, 0.40, 0.50, 0.60};
   private static final double[] CONTROL = {0.10, 0.25, 0.40, 0.60, 0.80};
   private static final double[] ORDINARY_CONTROL = {0.60, 0.85, 0.95, 1.00, 1.00};
   private static final double[] VANILLA_CONTROL = {0.80, 0.95, 1.00, 1.00, 1.00};
   private static final float[] DAMAGE_PER_SECOND = {1.0F, 5.0F, 10.0F, 15.0F, 20.0F};
   private static final float[] CONCEPT_DEATH_CHANCE = {0.01F, 0.05F, 0.10F, 0.15F, 0.20F};

   private InfectionRules() {
   }

   public static int clampLevel(int level) {
      return Math.max(1, Math.min(MAX_LEVEL, level));
   }

   public static double spreadChance(int level) {
      return SPREAD[clampLevel(level) - 1];
   }

   public static double spreadChanceForInterval(int level, int damageIntervals) {
      double missChance = 1.0 - spreadChance(level);
      return 1.0 - Math.pow(missChance, Math.max(1, damageIntervals));
   }

   public static boolean isScheduled(int entityId, long gameTime, int interval) {
      return Math.floorMod(gameTime + entityId, Math.max(1, interval)) == 0;
   }

   public static double controlChance(int level) {
      return CONTROL[clampLevel(level) - 1];
   }

   public static double ordinaryControlChance(int level, boolean vanillaEntity) {
      double[] chances = vanillaEntity ? VANILLA_CONTROL : ORDINARY_CONTROL;
      return chances[clampLevel(level) - 1];
   }

   public static float damagePerSecond(int level) {
      return DAMAGE_PER_SECOND[clampLevel(level) - 1];
   }

   public static float conceptDeathChance(int level) {
      return CONCEPT_DEATH_CHANCE[clampLevel(level) - 1];
   }

   public static int calamityExitCleanseTicks(int level) {
      return clampLevel(level) * 20;
   }
}
