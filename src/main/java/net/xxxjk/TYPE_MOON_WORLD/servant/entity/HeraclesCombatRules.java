package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

public final class HeraclesCombatRules {
   private static final double BASIC_SWEEP_DOT = -0.35;

   private HeraclesCombatRules() { }

   public static boolean isInsideBasicSweepArc(double forwardX, double forwardZ, double offsetX, double offsetZ) {
      double forwardLength = Math.sqrt(forwardX * forwardX + forwardZ * forwardZ);
      double offsetLength = Math.sqrt(offsetX * offsetX + offsetZ * offsetZ);
      if (forwardLength < 1.0E-6 || offsetLength < 1.0E-6) return true;
      double dot = (forwardX * offsetX + forwardZ * offsetZ) / (forwardLength * offsetLength);
      return dot >= BASIC_SWEEP_DOT;
   }
}
