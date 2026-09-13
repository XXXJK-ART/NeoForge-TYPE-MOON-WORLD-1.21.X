package net.xxxjk.TYPE_MOON_WORLD.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SeaBeastSizeTest {
   @Test
   void seaBeastVisualVariantsUseThreeAndFourTimesWolfScale() {
      assertEquals(3.0F, SeaBeastEntity.SMALL_VISUAL_SCALE);
      assertEquals(4.0F, SeaBeastEntity.LARGE_VISUAL_SCALE);
   }
}
