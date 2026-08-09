package net.xxxjk.TYPE_MOON_WORLD.passive;

import net.xxxjk.TYPE_MOON_WORLD.magic.MagicAnalysisService;
import net.xxxjk.TYPE_MOON_WORLD.magic.MagicPassiveProgressionService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AdvancedPassiveRulesTest {
   @Test
   void highSpeedPassivesUseBestMultiplierAndAnalysisInterpolation() {
      assertEquals(0.80, AdvancedPassiveService.enhancedIncantationChantMultiplier(PassiveRank.E, 0.0), 1.0E-9);
      assertEquals(0.90, AdvancedPassiveService.enhancedIncantationManaMultiplier(PassiveRank.E, 0.0), 1.0E-9);

      assertEquals(0.65, AdvancedPassiveService.enhancedIncantationChantMultiplier(PassiveRank.E, 75.0), 1.0E-9);
      assertEquals(0.80, AdvancedPassiveService.enhancedIncantationManaMultiplier(PassiveRank.E, 75.0), 1.0E-9);

      assertEquals(0.50, AdvancedPassiveService.enhancedIncantationChantMultiplier(PassiveRank.E, 100.0), 1.0E-9);
      assertEquals(0.70, AdvancedPassiveService.enhancedIncantationManaMultiplier(PassiveRank.E, 100.0), 1.0E-9);

      assertEquals(0.30, AdvancedPassiveService.divineWordsChantMultiplier(PassiveRank.C), 1.0E-9);
      assertEquals(0.50, AdvancedPassiveService.divineWordsManaMultiplier(PassiveRank.C), 1.0E-9);
   }

   @Test
   void partitionedThoughtSpeedAndEffectiveAnalysisFollowRank() {
      assertEquals(3, AdvancedPassiveService.partitionN(PassiveRank.E));
      assertEquals(27, AdvancedPassiveService.analysisWorkPerTick(PassiveRank.E));
      assertEquals(57.5, AdvancedPassiveService.effectiveMagicAnalysisProficiency(50.0, PassiveRank.E), 1.0E-9);

      assertEquals(7, AdvancedPassiveService.partitionN(PassiveRank.A));
      assertEquals(823543, AdvancedPassiveService.analysisWorkPerTick(PassiveRank.A));
      assertEquals(67.5, AdvancedPassiveService.effectiveMagicAnalysisProficiency(50.0, PassiveRank.A), 1.0E-9);

      assertEquals(74.99, AdvancedPassiveService.effectiveMagicAnalysisProficiency(74.0, PassiveRank.A), 1.0E-9);
      assertEquals(80.0, AdvancedPassiveService.effectiveMagicAnalysisProficiency(80.0, PassiveRank.A), 1.0E-9);
   }

   @Test
   void goldenRuleAndMagicPassiveProgressionTablesAreStable() {
      assertEquals(5, AdvancedPassiveService.lootingBonus(PassiveRank.A));
      assertEquals(0.10, AdvancedPassiveService.goldenDropChance(PassiveRank.A), 1.0E-9);

      assertEquals(30.0, MagicPassiveProgressionService.chancePercent(300.0), 1.0E-9);
      assertEquals(100.0, MagicPassiveProgressionService.chancePercent(1200.0), 1.0E-9);
   }

   @Test
   void magicAnalysisBaseWorkUsesExponentialComplexityCurve() {
      assertEquals(20, MagicAnalysisService.baseWork(10));
      assertEquals(83, MagicAnalysisService.baseWork(30));
      assertEquals(344, MagicAnalysisService.baseWork(50));
      assertEquals(1423, MagicAnalysisService.baseWork(70));
      assertEquals(5896, MagicAnalysisService.baseWork(90));
      assertEquals(12000, MagicAnalysisService.baseWork(100));
   }
}
