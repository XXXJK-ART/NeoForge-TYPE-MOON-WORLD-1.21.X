package net.xxxjk.TYPE_MOON_WORLD.item.custom;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MuramasaItemTest {
   @Test
   void regularMuramasaOnlyConsumesManaDuringChargeWindow() {
      assertTrue(MuramasaItem.shouldConsumeMana(1));
      assertTrue(MuramasaItem.shouldConsumeMana(100));
      assertFalse(MuramasaItem.shouldConsumeMana(0));
      assertFalse(MuramasaItem.shouldConsumeMana(101));
   }
}
