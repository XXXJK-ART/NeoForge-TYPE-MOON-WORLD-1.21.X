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
      assertEquals(20, InfectionRules.DAMAGE_INTERVAL_TICKS);
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

   @Test
   void ordinaryCreaturesAreControlledQuickly() {
      double[] modded = {0.60, 0.85, 0.95, 1.00, 1.00};
      double[] vanilla = {0.80, 0.95, 1.00, 1.00, 1.00};
      for (int level = 1; level <= 5; level++) {
         assertEquals(modded[level - 1], InfectionRules.ordinaryControlChance(level, false), 1.0E-9);
         assertEquals(vanilla[level - 1], InfectionRules.ordinaryControlChance(level, true), 1.0E-9);
      }
      assertEquals(1.0, InfectionRules.ordinaryControlChance(3, true), 1.0E-9);
      assertEquals(1.0, InfectionRules.ordinaryControlChance(4, false), 1.0E-9);
   }

   @Test
   void damageScalesWithInfectionLevel() {
      float[] damage = {5.0F, 10.0F, 15.0F, 20.0F, 25.0F};
      for (int level = 1; level <= 5; level++) {
         assertEquals(damage[level - 1], InfectionRules.damagePerSecond(level), 0.0F);
      }
   }

   @Test
   void conceptDeathChanceScalesWithInfectionLevel() {
      float[] chances = {0.01F, 0.05F, 0.10F, 0.15F, 0.20F};
      for (int level = 1; level <= 5; level++) {
         assertEquals(chances[level - 1], InfectionRules.conceptDeathChance(level), 0.0F);
      }
   }

   @Test
   void calamityExitCleanseDelayMatchesInfectionLevel() {
      for (int level = 1; level <= 5; level++) {
         assertEquals(level * 20, InfectionRules.calamityExitCleanseTicks(level));
      }
   }
}
