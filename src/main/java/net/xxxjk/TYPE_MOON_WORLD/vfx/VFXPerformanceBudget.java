package net.xxxjk.TYPE_MOON_WORLD.vfx;

/** Pure budget policy kept separate from client classes so its boundaries are unit-testable. */
public final class VFXPerformanceBudget {
   public enum Pressure { NORMAL, PRESSURED, CRITICAL }

   public static final int MAX_EMITTERS = 192;
   public static final int MAX_BEAMS = 256;
   public static final int MAX_RINGS = 128;
   public static final double MAX_RENDER_DISTANCE_SQR = 160.0 * 160.0;
   private static final Budget NORMAL_BUDGET = new Budget(12_000, 256);
   private static final Budget PRESSURED_BUDGET = new Budget(7_000, 128);
   private static final Budget CRITICAL_BUDGET = new Budget(3_500, 64);

   private VFXPerformanceBudget() { }

   public static Pressure classify(double smoothedFps) {
      if (smoothedFps > 0.0 && smoothedFps < 30.0) return Pressure.CRITICAL;
      if (smoothedFps > 0.0 && smoothedFps < 45.0) return Pressure.PRESSURED;
      return Pressure.NORMAL;
   }

   public static Budget forPressure(Pressure pressure) {
      return switch (pressure) {
         case NORMAL -> NORMAL_BUDGET;
         case PRESSURED -> PRESSURED_BUDGET;
         case CRITICAL -> CRITICAL_BUDGET;
      };
   }

   public record Budget(int particles, int vanillaParticlesPerTick) { }
}
