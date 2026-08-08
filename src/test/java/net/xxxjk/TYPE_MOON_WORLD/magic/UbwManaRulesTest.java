package net.xxxjk.TYPE_MOON_WORLD.magic;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.xxxjk.TYPE_MOON_WORLD.magic.unlimited_blade_works.UbwManaRules;
import org.junit.jupiter.api.Test;

class UbwManaRulesTest {
   @Test
   void playerOpeningPaymentsTotalTwoHundredMana() {
      assertEquals(200.0D, UbwManaRules.PLAYER_OPENING_TOTAL);
      assertEquals(20.0D, UbwManaRules.openingPayment(false));
      assertEquals(50.0D, UbwManaRules.openingPayment(true));
   }

   @Test
   void playerUpkeepStartsAfterThirtySecondsAtTwoManaPerSecond() {
      assertEquals(0.0D, UbwManaRules.upkeepCost(false, 0L));
      assertEquals(0.0D, UbwManaRules.upkeepCost(false, 599L));
      assertEquals(2.0D, UbwManaRules.upkeepCost(false, 600L));
      assertEquals(2.0D, UbwManaRules.upkeepCost(false, 1200L));
      assertEquals(10.0D, UbwManaRules.upkeepCost(true, 0L));
   }
}
