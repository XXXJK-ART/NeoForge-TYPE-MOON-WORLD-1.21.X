package net.xxxjk.TYPE_MOON_WORLD.magic.special;

public final class TimeAlterRateMath {
   public static final double MAX_ACTION_RATE = 10.0;
   private static final double MIN_ACTION_RATE = 0.05;

   private TimeAlterRateMath() {
   }

   public static double effectiveActionRate(int mode, double configuredMultiplier) {
      if (!Double.isFinite(configuredMultiplier)) {
         return 1.0;
      }
      if (mode == MagicTimeAlter.MODE_ACCEL) {
         return Math.max(1.0, Math.min(MAX_ACTION_RATE, configuredMultiplier));
      }
      return Math.max(MIN_ACTION_RATE, Math.min(1.0, configuredMultiplier));
   }

   public static double linearModifierAmount(double actionRate) {
      return sanitizeRate(actionRate) - 1.0;
   }

   public static double gravityModifierAmount(double actionRate) {
      double rate = sanitizeRate(actionRate);
      return rate * rate - 1.0;
   }

   public static UseAdvance advanceItemUse(double accumulatedProgress, double actionRate) {
      double progress = Math.max(0.0, accumulatedProgress) + sanitizeRate(actionRate);
      int elapsedUseTicks = Math.max(0, (int)Math.floor(progress + 1.0E-9));
      return new UseAdvance(elapsedUseTicks, Math.max(0.0, progress - elapsedUseTicks));
   }

   public static int durationBeforeVanillaDecrement(int currentDuration, int elapsedUseTicks) {
      return currentDuration + 1 - Math.max(0, elapsedUseTicks);
   }

   private static double sanitizeRate(double actionRate) {
      return Double.isFinite(actionRate) ? Math.max(MIN_ACTION_RATE, Math.min(MAX_ACTION_RATE, actionRate)) : 1.0;
   }

   public record UseAdvance(int elapsedUseTicks, double remainingProgress) {
   }
}
