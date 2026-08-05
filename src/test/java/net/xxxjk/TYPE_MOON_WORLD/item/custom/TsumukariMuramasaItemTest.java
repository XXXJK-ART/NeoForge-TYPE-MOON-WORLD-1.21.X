package net.xxxjk.TYPE_MOON_WORLD.item.custom;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TsumukariMuramasaItemTest {
   @Test
   void muramasaCardStopsPayingManaAtTenPercent() {
      assertEquals(10, TsumukariMuramasaItem.SPECIAL_CHARGE_PERCENT);
      assertTrue(TsumukariMuramasaItem.shouldConsumeMuramasaCardMana(9));
      assertFalse(TsumukariMuramasaItem.shouldConsumeMuramasaCardMana(10));
      assertFalse(TsumukariMuramasaItem.shouldConsumeMuramasaCardMana(100));
   }
}
