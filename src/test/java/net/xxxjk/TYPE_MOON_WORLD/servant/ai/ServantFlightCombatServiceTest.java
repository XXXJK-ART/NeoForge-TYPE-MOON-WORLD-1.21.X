package net.xxxjk.TYPE_MOON_WORLD.servant.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ServantFlightCombatServiceTest {
   @Test
   void disconnectedCombatLowersFlightBandBeforeForcingContact() {
      assertEquals(67.8, ServantFlightHelper.desiredCombatY(64.0, 66.0, 1.8, 0L), 1.0E-6);
      assertEquals(66.9, ServantFlightHelper.desiredCombatY(64.0, 66.0, 1.8, 40L), 1.0E-6);
      assertEquals(66.35, ServantFlightHelper.desiredCombatY(64.0, 66.0, 1.8, 100L), 1.0E-6);
      assertTrue(ServantFlightCombatService.shouldDescend(40L));
      assertTrue(ServantFlightCombatService.shouldForceContact(100L));
   }

   @Test
   void anchorRebasesGraduallyInsteadOfFollowingAHeightJump() {
      assertEquals(64.0, ServantFlightHelper.advanceAnchor(64.0, 66.5), 1.0E-6);
      assertEquals(64.24, ServantFlightHelper.advanceAnchor(64.0, 76.0), 1.0E-6);
      assertEquals(63.76, ServantFlightHelper.advanceAnchor(64.0, 50.0), 1.0E-6);
   }

   @Test
   void verticalSafetyCapsOldMomentumAndForcesDescent() {
      assertEquals(ServantFlightHelper.MAX_VERTICAL_SPEED_UP,
         ServantFlightCombatService.safeVerticalVelocity(70.0, 70.0, 1.2, 0.0, 0L, true), 1.0E-6);
      double descending = ServantFlightCombatService.safeVerticalVelocity(
         74.0, 70.0, 0.8, -0.2, ServantFlightCombatService.DESCENT_TICKS, false);
      assertTrue(descending <= -0.06);
      assertTrue(descending >= -ServantFlightHelper.MAX_VERTICAL_SPEED_DOWN);
   }
}
