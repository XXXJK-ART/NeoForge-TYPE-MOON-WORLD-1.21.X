package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HeraclesBasicSweepTest {
   @Test
   void basicSweepCoversFrontAndSidesButNotDirectlyBehind() {
      assertTrue(HeraclesCombatRules.isInsideBasicSweepArc(1.0, 0.0, 4.0, 0.0));
      assertTrue(HeraclesCombatRules.isInsideBasicSweepArc(1.0, 0.0, 0.0, 4.0));
      assertFalse(HeraclesCombatRules.isInsideBasicSweepArc(1.0, 0.0, -4.0, 0.0));
      assertFalse(HeraclesCombatRules.isInsideBasicSweepArc(1.0, 0.0, -3.0, 3.0));
   }
}
