package net.xxxjk.TYPE_MOON_WORLD.combat.deadapostle;

public final class NeroChaosRules {
   public static final int MAX_LIVES = 666;
   public static final int MAX_ACTIVE_BEASTS = 50;
   public static final int INITIAL_SCOUT_BEAST_MIN = 2;
   public static final int INITIAL_SCOUT_BEAST_MAX = 5;
   public static final int FULL_COMBAT_BEAST_COUNT = 20;
   public static final int CHAOS_FORM_THRESHOLD = 100;
   public static final int DEFAULT_CHAOS_ENERGY = 100;

   private NeroChaosRules() {
   }

   public static int clampLives(int lives) {
      return Math.max(0, Math.min(MAX_LIVES, lives));
   }

   public static int consumeLife(int lives) {
      return Math.max(0, lives - 1);
   }

   public static boolean shouldReviveAfterLethal(int lives, boolean forcedDeath) {
      return !forcedDeath && lives > 1;
   }

   public static int combatBeastTarget(int remainingLives) {
      if (remainingLives > 300) return FULL_COMBAT_BEAST_COUNT;
      if (remainingLives >= CHAOS_FORM_THRESHOLD) {
         return 30 + Math.min(10, (300 - remainingLives) / 20);
      }
      return MAX_ACTIVE_BEASTS;
   }

   public static boolean shouldEnterChaosForm(int remainingLives, int activeBeasts) {
      return remainingLives < CHAOS_FORM_THRESHOLD && activeBeasts >= MAX_ACTIVE_BEASTS;
   }
}
