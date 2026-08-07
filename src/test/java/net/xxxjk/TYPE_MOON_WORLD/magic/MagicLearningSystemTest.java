package net.xxxjk.TYPE_MOON_WORLD.magic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MagicLearningSystemTest {
   @Test
   void projectionAndStructuralAnalysisAreOrdinaryMagics() {
      for (String id : new String[]{"projection", "structural_analysis"}) {
         assertTrue(MagicLearningStrategy.canAnalyze(id));
         assertTrue(MagicLearningStrategy.canLearnFromMaterial(id));
         assertTrue(MagicLearningStrategy.canResearch(id));
         assertTrue(MagicLearningStrategy.canCopy(id));
         assertFalse(MagicLearningStrategy.requiresSword(id));
         assertTrue(MagicLearningStrategy.materialAllowed(false, id));
      }
   }

   @Test
   void unlimitedBladeWorksFamilyCannotBeAnalyzedAndRequiresSwordForMaterials() {
      for (String id : new String[]{"unlimited_blade_works", "sword_barrel_full_open", "broken_phantasm"}) {
         assertFalse(MagicLearningStrategy.canAnalyze(id));
         assertTrue(MagicLearningStrategy.requiresSword(id));
         assertFalse(MagicLearningStrategy.materialAllowed(false, id));
         assertTrue(MagicLearningStrategy.materialAllowed(true, id));
      }
   }

   @Test
   void timeAlterIsMaterialOnlyForInitialLearning() {
      assertFalse(MagicLearningStrategy.canAnalyze("time_alter"));
      assertTrue(MagicLearningStrategy.materialAllowed(false, "time_alter"));
      assertFalse(MagicLearningStrategy.requiresSword("time_alter"));
   }

   @Test
   void legacyScrollMaterialsRemainUsableByResearchAndCopying() {
      assertEquals("magic_scroll_gravity_broken", MagicLearningStrategy.pageItemPath("gravity_magic"));
      assertEquals("magic_scroll_gander_broken", MagicLearningStrategy.pageItemPath("gander"));
      assertEquals("magic_scroll_broken_phantasm_broken", MagicLearningStrategy.pageItemPath("broken_phantasm"));
      for (String id : new String[]{"jewel_magic_shoot", "jewel_magic_release", "jewel_random_shoot", "jewel_machine_gun"}) {
         assertTrue(MagicLearningStrategy.canLearnFromMaterial(id));
         assertTrue(MagicLearningStrategy.canResearch(id));
         assertTrue(MagicLearningStrategy.canCopy(id));
      }
   }

   @Test
   void learningChanceUsesAnalysisProficiency() {
      assertEquals(0.65, MagicLearningStrategy.learningChance("projection", 0.0), 1.0E-9);
      assertEquals(1.0, MagicLearningStrategy.learningChance("projection", 35.0), 1.0E-9);
      assertEquals(0.30, MagicLearningStrategy.learningChance("time_alter", 0.0), 1.0E-9);
   }

   @Test
   void analysisVerseLimitsFollowProficiencyThresholds() {
      assertEquals(1, MagicLearningStrategy.analysisVerseLimit(30.0));
      assertEquals(2, MagicLearningStrategy.analysisVerseLimit(31.0));
      assertEquals(3, MagicLearningStrategy.analysisVerseLimit(50.0));
      assertEquals(3, MagicLearningStrategy.analysisVerseLimit(74.0));
      assertEquals(Integer.MAX_VALUE, MagicLearningStrategy.analysisVerseLimit(75.0));
   }

   @Test
   void ordinaryProficiencyGrowthDiminishesAtHighValues() {
      assertEquals(55.0, MagicProficiencyService.calculateValue("projection", 50.0, 10.0), 1.0E-9);
      assertEquals(77.5, MagicProficiencyService.calculateValue("projection", 75.0, 10.0), 1.0E-9);
      assertEquals(91.0, MagicProficiencyService.calculateValue("projection", 90.0, 10.0), 1.0E-9);
   }

   @Test
   void analysisGrowthDropsSharplyAtFiftyAndSeventyFive() {
      assertEquals(51.25, MagicProficiencyService.calculateValue("magic_analysis", 50.0, 10.0), 1.0E-9);
      assertEquals(75.25, MagicProficiencyService.calculateValue("magic_analysis", 75.0, 10.0), 1.0E-9);
      assertEquals(90.10, MagicProficiencyService.calculateValue("magic_analysis", 90.0, 10.0), 1.0E-9);
   }

   @Test
   void analysisResearchCostsDependOnAnalysisProficiency() {
      assertEquals(600, MagicLearningStrategy.researchTicks("magic_analysis", 0.0));
      assertEquals(200, MagicLearningStrategy.researchTicks("magic_analysis", 100.0));
      assertEquals(300.0, MagicLearningStrategy.researchManaCost("magic_analysis", 0.0), 1.0E-9);
      assertEquals(1100.0, MagicLearningStrategy.researchManaCost("magic_analysis", 100.0), 1.0E-9);
   }
}
