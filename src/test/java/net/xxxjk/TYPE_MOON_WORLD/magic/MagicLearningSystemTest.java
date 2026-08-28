package net.xxxjk.TYPE_MOON_WORLD.magic;

import net.minecraft.nbt.CompoundTag;
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
   void addonMagicsFollowTheNewDifficultyCurve() {
      assertEquals(10, MagicLearningStrategy.complexity("detection"));
      assertEquals(20, MagicLearningStrategy.complexity("airflow_blade"));
      assertEquals(55, MagicLearningStrategy.complexity("imaginary_dive"));
      assertEquals(70, MagicLearningStrategy.complexity("absorption"));
      assertEquals(85, MagicLearningStrategy.complexity("imaginary_space"));
      assertEquals(90, MagicLearningStrategy.complexity("antores"));
      assertEquals(95, MagicLearningStrategy.complexity("nega_summon"));

      assertEquals(1, MagicLearningStrategy.verses("detection"));
      assertEquals(1, MagicLearningStrategy.verses("airflow_blade"));
      assertEquals(3, MagicLearningStrategy.verses("imaginary_dive"));
      assertEquals(4, MagicLearningStrategy.verses("absorption"));
      assertEquals(5, MagicLearningStrategy.verses("antores"));
      assertEquals(5, MagicLearningStrategy.verses("nega_summon"));

      assertTrue(MagicLearningStrategy.canAnalyze("detection"));
      assertTrue(MagicLearningStrategy.canAnalyze("nega_summon"));
      assertFalse(MagicLearningStrategy.canLearnFromMaterial("detection"));
      assertFalse(MagicLearningStrategy.canResearch("imaginary_space"));
      assertFalse(MagicLearningStrategy.canCopy("storm"));
      assertTrue(MagicLearningStrategy.isDivine("antores"));
      assertTrue(MagicLearningStrategy.isDivine("nega_summon"));
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
   void knowledgeCatalogOnlyExposesBasicAndAdvancedJewelKnowledge() {
      assertTrue(MagicLearningStrategy.isKnowledgeVisible("jewel_magic_shoot"));
      assertTrue(MagicLearningStrategy.isKnowledgeVisible("typemoonworld:jewel_magic_release"));
      assertTrue(MagicLearningStrategy.isKnowledgeVisible("jewel_machine_gun"));
      for (String id : new String[]{
         "jewel_random_shoot",
         "ruby_throw", "sapphire_throw", "emerald_use", "topaz_throw", "cyan_throw",
         "ruby_flame_sword", "sapphire_winter_frost", "emerald_winter_river", "topaz_reinforcement", "cyan_wind",
         "monstrous_strength", "typemoonworld:clairvoyance"
      }) {
         assertFalse(MagicLearningStrategy.isKnowledgeVisible(id), id);
      }
      assertTrue(MagicLearningStrategy.isKnowledgeVisible("gae_bolg_throw"));
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
   void onlyPresetMagicsRequireTabConfiguration() {
      for (String id : new String[]{
         "reinforcement", "gravity_magic", "gandr_machine_gun", "projection",
         "healing_magic", "time_alter", "mana_burst", "fire_magic", "touko_travel"
      }) {
         assertTrue(PlayerMagicSelectionService.requiresPresetConfiguration(id), id);
      }

      assertFalse(PlayerMagicSelectionService.requiresPresetConfiguration("magic_bullet"));
      assertTrue(PlayerMagicSelectionService.normalizePresetPayload("projection", new CompoundTag()).isEmpty());
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
       assertEquals(150.0, MagicLearningStrategy.researchManaCost("magic_analysis", 0.0), 1.0E-9);
       assertEquals(550.0, MagicLearningStrategy.researchManaCost("magic_analysis", 100.0), 1.0E-9);
   }
}
