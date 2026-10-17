package net.xxxjk.TYPE_MOON_WORLD.utils;

/** Shared tick-quantized natural regeneration calculation for the server and UI. */
public final class ManaRegenRules {
   private ManaRegenRules() { }

   public static int intervalTicks(double baseTicks, boolean circuitOpen) {
      if (!Double.isFinite(baseTicks)) return 1;
      return Math.max(1, (int)Math.round(baseTicks / (circuitOpen ? 2.0 : 1.0)));
   }

   public static double perSecond(double amount, double concentration, double baseTicks, boolean circuitOpen) {
      return amount * Math.max(0.0, concentration) * 20.0 / intervalTicks(baseTicks, circuitOpen);
   }
}
