package net.xxxjk.TYPE_MOON_WORLD.servant.palerider;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class InfectionRulesTest {
   @Test
   void infectionClampsAtFiveLevels() {
      assertEquals(1, InfectionRules.clampLevel(-1));
      assertEquals(3, InfectionRules.clampLevel(3));
      assertEquals(5, InfectionRules.clampLevel(20));
      assertEquals(600, InfectionRules.DURATION_TICKS);
      assertEquals(600, InfectionRules.IMMUNITY_TICKS);
   }

   @Test
   void probabilitiesMatchFastSpreadDesign() {
      double[] spread = {0.20, 0.30, 0.40, 0.50, 0.60};
      double[] control = {0.10, 0.25, 0.40, 0.60, 0.80};
      for (int level = 1; level <= 5; level++) {
         assertEquals(spread[level - 1], InfectionRules.spreadChance(level), 1.0E-9);
         assertEquals(control[level - 1], InfectionRules.controlChance(level), 1.0E-9);
      }
   }
}
