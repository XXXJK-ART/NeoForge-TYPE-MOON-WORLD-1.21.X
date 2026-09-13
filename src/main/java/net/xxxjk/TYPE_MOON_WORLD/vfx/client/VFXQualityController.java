package net.xxxjk.TYPE_MOON_WORLD.vfx.client;

import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXPerformanceBudget;

/** FPS hysteresis controller: avoids quality thrashing around a threshold. */
public final class VFXQualityController {
   private VFXPerformanceBudget.Pressure pressure = VFXPerformanceBudget.Pressure.NORMAL;
   private int lowTicks;
   private int recoveryTicks;

   public void tick(double fps) {
      if (fps > 0.0 && fps < 35.0) {
         lowTicks++;
         recoveryTicks = 0;
         if (lowTicks >= 40) pressure = VFXPerformanceBudget.Pressure.CRITICAL;
      } else if (fps > 0.0 && fps < 50.0) {
         lowTicks++;
         recoveryTicks = 0;
         if (lowTicks >= 40 && pressure == VFXPerformanceBudget.Pressure.NORMAL) pressure = VFXPerformanceBudget.Pressure.PRESSURED;
      } else if (fps >= 55.0) {
         lowTicks = 0;
         recoveryTicks++;
         if (recoveryTicks >= 60) {
            pressure = pressure == VFXPerformanceBudget.Pressure.CRITICAL
               ? VFXPerformanceBudget.Pressure.PRESSURED : VFXPerformanceBudget.Pressure.NORMAL;
            recoveryTicks = 0;
         }
      } else {
         lowTicks = Math.max(0, lowTicks - 1);
         recoveryTicks = 0;
      }
   }

   public VFXPerformanceBudget.Pressure pressure() { return pressure; }
   public void reset() { pressure = VFXPerformanceBudget.Pressure.NORMAL; lowTicks = 0; recoveryTicks = 0; }
}
