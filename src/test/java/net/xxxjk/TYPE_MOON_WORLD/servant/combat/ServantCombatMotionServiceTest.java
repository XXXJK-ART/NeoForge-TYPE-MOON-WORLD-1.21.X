package net.xxxjk.TYPE_MOON_WORLD.servant.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ServantCombatMotionServiceTest {
   @Test
   void repeatedControlHasExplicitDiminishingReturns() {
      assertEquals(1.0, ServantCombatMotionService.controlScale(1));
      assertEquals(0.72, ServantCombatMotionService.controlScale(2));
      assertEquals(0.48, ServantCombatMotionService.controlScale(3));
      assertEquals(0.28, ServantCombatMotionService.controlScale(4));
      assertEquals(0.28, ServantCombatMotionService.controlScale(99));
      assertTrue(ServantCombatMotionService.controlScale(1) > ServantCombatMotionService.controlScale(4));
   }
}
