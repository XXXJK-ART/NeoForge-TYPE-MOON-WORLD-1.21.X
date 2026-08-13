package net.xxxjk.TYPE_MOON_WORLD.vfx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class VFXPerformanceBudgetTest {
   @Test
   void classifiesSmoothedFpsBoundaries() {
      assertEquals(VFXPerformanceBudget.Pressure.NORMAL, VFXPerformanceBudget.classify(45.0));
      assertEquals(VFXPerformanceBudget.Pressure.PRESSURED, VFXPerformanceBudget.classify(44.99));
      assertEquals(VFXPerformanceBudget.Pressure.PRESSURED, VFXPerformanceBudget.classify(30.0));
      assertEquals(VFXPerformanceBudget.Pressure.CRITICAL, VFXPerformanceBudget.classify(29.99));
      assertEquals(VFXPerformanceBudget.Pressure.NORMAL, VFXPerformanceBudget.classify(0.0));
   }

   @Test
   void budgetsDecreaseMonotonicallyWithPressure() {
      var normal = VFXPerformanceBudget.forPressure(VFXPerformanceBudget.Pressure.NORMAL);
      var pressured = VFXPerformanceBudget.forPressure(VFXPerformanceBudget.Pressure.PRESSURED);
      var critical = VFXPerformanceBudget.forPressure(VFXPerformanceBudget.Pressure.CRITICAL);
      assertTrue(normal.particles() > pressured.particles());
      assertTrue(pressured.particles() > critical.particles());
      assertTrue(normal.vanillaParticlesPerTick() > pressured.vanillaParticlesPerTick());
      assertTrue(pressured.vanillaParticlesPerTick() > critical.vanillaParticlesPerTick());
   }

   @Test
   void reusesBudgetInstancesOnTheRenderHotPath() {
      assertSame(
         VFXPerformanceBudget.forPressure(VFXPerformanceBudget.Pressure.NORMAL),
         VFXPerformanceBudget.forPressure(VFXPerformanceBudget.Pressure.NORMAL)
      );
   }
}
