package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ParacelsusBalanceRulesTest {
   @Test
   void allCurrentDamageIsReducedByAboutOneThird() {
      assertEquals(2.0F / 3.0F, ParacelsusBalanceRules.DAMAGE_MULTIPLIER);
      assertEquals(200.0F / 3.0F, ParacelsusBalanceRules.reduceDamage(100.0F), 1.0E-5F);
      assertEquals(100.0 / 3.0, ParacelsusBalanceRules.reduceDamage(50.0), 1.0E-6);
   }

   @Test
   void continuousFireIsCappedAtThreeSeconds() {
      assertEquals(60, ParacelsusBalanceRules.capFireTicks(1000));
      assertEquals(40, ParacelsusBalanceRules.capFireTicks(40));
   }
}
