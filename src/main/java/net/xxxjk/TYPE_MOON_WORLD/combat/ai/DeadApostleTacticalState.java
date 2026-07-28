package net.xxxjk.TYPE_MOON_WORLD.combat.ai;

public final class DeadApostleTacticalState {
   private DeadApostleTacticalState() { }

   public static String determinePhase(boolean daylight, float healthRatio, float hungerRatio, float targetStrengthRatio) {
      if (healthRatio <= 0.35F || hungerRatio >= 0.70F) return "FERAL";
      if (daylight || targetStrengthRatio >= 1.5F) return "CAUTIOUS";
      if (hungerRatio >= 0.40F) return "HUNTING";
      return "NORMAL";
   }
}
