package net.xxxjk.TYPE_MOON_WORLD.servant.card;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ServantCardUshiwakamaruRulesTest {
   @Test
   void eightBoatTemporarilyRaisesTheJumpLimit() {
      assertEquals(4, ServantCardUshiwakamaruRules.jumpLimit(false));
      assertEquals(8, ServantCardUshiwakamaruRules.jumpLimit(true));
      assertEquals(4, ServantCardUshiwakamaruRules.clampJumpCharges(8, false));
      assertEquals(8, ServantCardUshiwakamaruRules.clampJumpCharges(8, true));
      assertEquals(2, ServantCardUshiwakamaruRules.clampJumpCharges(2, false));
   }

   @Test
   void onlyEightBoatAllowsAirborneJumpRecovery() {
      assertFalse(ServantCardUshiwakamaruRules.canRecoverJump(false, false));
      assertTrue(ServantCardUshiwakamaruRules.canRecoverJump(false, true));
      assertTrue(ServantCardUshiwakamaruRules.canRecoverJump(true, false));
   }

   @Test
   void spiderSlayerUsesTheDemonicBonus() {
      assertEquals(100.0F, ServantCardUshiwakamaruRules.spiderSlayerDamage(false));
      assertEquals(150.0F, ServantCardUshiwakamaruRules.spiderSlayerDamage(true));
   }
}
