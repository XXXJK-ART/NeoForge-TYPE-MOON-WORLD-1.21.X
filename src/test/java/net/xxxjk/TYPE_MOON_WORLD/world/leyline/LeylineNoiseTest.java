package net.xxxjk.TYPE_MOON_WORLD.world.leyline;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LeylineNoiseTest {
   @Test
   void regenMultiplierIsLinearAndClamped() {
      assertEquals(0.0, LeylineNoise.regenMultiplier(0), 1.0E-9);
      assertEquals(0.1, LeylineNoise.regenMultiplier(10), 1.0E-9);
      assertEquals(1.0, LeylineNoise.regenMultiplier(100), 1.0E-9);
      assertEquals(1.0, LeylineNoise.regenMultiplier(180), 1.0E-9);
   }
}
