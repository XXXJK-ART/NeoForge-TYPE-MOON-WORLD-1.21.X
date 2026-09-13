package net.xxxjk.TYPE_MOON_WORLD.vfx;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.xxxjk.TYPE_MOON_WORLD.vfx.client.VFXQualityController;
import org.junit.jupiter.api.Test;

class VFXQualityControllerTest {
   @Test
   void entersPressuredAfterTwoSecondsBelowFiftyFps() {
      VFXQualityController controller = new VFXQualityController();
      for (int i = 0; i < 39; i++) controller.tick(45.0);
      assertEquals(VFXPerformanceBudget.Pressure.NORMAL, controller.pressure());
      controller.tick(45.0);
      assertEquals(VFXPerformanceBudget.Pressure.PRESSURED, controller.pressure());
   }

   @Test
   void entersCriticalBelowThirtyFiveAndRecoversInSteps() {
      VFXQualityController controller = new VFXQualityController();
      for (int i = 0; i < 40; i++) controller.tick(32.0);
      assertEquals(VFXPerformanceBudget.Pressure.CRITICAL, controller.pressure());
      for (int i = 0; i < 60; i++) controller.tick(60.0);
      assertEquals(VFXPerformanceBudget.Pressure.PRESSURED, controller.pressure());
      for (int i = 0; i < 60; i++) controller.tick(60.0);
      assertEquals(VFXPerformanceBudget.Pressure.NORMAL, controller.pressure());
   }
}
