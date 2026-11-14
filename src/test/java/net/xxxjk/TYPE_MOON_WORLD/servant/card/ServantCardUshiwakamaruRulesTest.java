package net.xxxjk.TYPE_MOON_WORLD.servant.card;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import net.minecraft.world.phys.Vec3;

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
      assertEquals(200.0F, ServantCardUshiwakamaruRules.spiderSlayerDamage(true));
   }

   @Test
   void spiderSlayerDamageFallsOffWithDistanceBeforeApplyingTraitMultiplier() {
      assertEquals(100.0F, ServantCardUshiwakamaruRules.spiderSlayerDamage(false, 0.0));
      assertEquals(75.0F, ServantCardUshiwakamaruRules.spiderSlayerDamage(false, 7.5));
      assertEquals(50.0F, ServantCardUshiwakamaruRules.spiderSlayerDamage(false, 15.0));
      assertEquals(100.0F, ServantCardUshiwakamaruRules.spiderSlayerDamage(true, 15.0));
   }

   @Test
   void eightBoatLeapIsBoundedAndRequiresAValidLanding() {
      assertEquals(32.0, ServantCardUshiwakamaruRules.eightBoatLeapDistance(40.0, true, true));
      assertEquals(0.0, ServantCardUshiwakamaruRules.eightBoatLeapDistance(32.0, false, true));
      assertEquals(0.0, ServantCardUshiwakamaruRules.eightBoatLeapDistance(32.0, true, false));
      assertEquals(new Vec3(32.0, 0.0, 0.0), ServantCardUshiwakamaruRules.eightBoatLeapVector(
         Vec3.ZERO, new Vec3(40.0, 0.0, 0.0), 1.0));
   }

}
