package net.xxxjk.TYPE_MOON_WORLD.performance;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PerformanceMonitorTest {
   @Test
   void classifiesConfiguredBoundaries() {
      assertEquals(PerformanceMonitor.Pressure.NORMAL, PerformanceMonitor.classify(39.99, 40, 50));
      assertEquals(PerformanceMonitor.Pressure.PRESSURED, PerformanceMonitor.classify(40.0, 40, 50));
      assertEquals(PerformanceMonitor.Pressure.PRESSURED, PerformanceMonitor.classify(49.99, 40, 50));
      assertEquals(PerformanceMonitor.Pressure.CRITICAL, PerformanceMonitor.classify(50.0, 40, 50));
   }

   @Test
   void protectsAgainstCriticalThresholdBelowPressureThreshold() {
      assertEquals(PerformanceMonitor.Pressure.NORMAL, PerformanceMonitor.classify(39.99, 40, 30));
      assertEquals(PerformanceMonitor.Pressure.CRITICAL, PerformanceMonitor.classify(40.0, 40, 30));
   }

   @Test
   void backgroundScaleOnlyReducesDeferrableWork() {
      assertEquals(1.0, PerformanceMonitor.scaleFor(PerformanceMonitor.Pressure.NORMAL));
      assertEquals(0.5, PerformanceMonitor.scaleFor(PerformanceMonitor.Pressure.PRESSURED));
      assertEquals(0.25, PerformanceMonitor.scaleFor(PerformanceMonitor.Pressure.CRITICAL));
   }
}
