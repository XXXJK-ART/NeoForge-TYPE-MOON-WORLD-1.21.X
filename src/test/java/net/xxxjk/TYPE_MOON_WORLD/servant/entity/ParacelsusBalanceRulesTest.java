package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ParacelsusBalanceRulesTest {
   @Test
   void allCurrentDamageIsReducedByOneFifth() {
      assertEquals(0.8F, ParacelsusBalanceRules.DAMAGE_MULTIPLIER);
      assertEquals(80.0F, ParacelsusBalanceRules.reduceDamage(100.0F));
      assertEquals(40.0, ParacelsusBalanceRules.reduceDamage(50.0), 1.0E-6);
   }
}
