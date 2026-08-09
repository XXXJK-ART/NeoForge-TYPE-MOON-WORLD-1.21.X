package net.xxxjk.TYPE_MOON_WORLD.talent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.xxxjk.TYPE_MOON_WORLD.magic.MagicLearningStrategy;
import net.xxxjk.TYPE_MOON_WORLD.martial.MartialPassiveProgressionService;
import net.xxxjk.TYPE_MOON_WORLD.passive.PassiveRank;
import net.xxxjk.TYPE_MOON_WORLD.passive.PassiveService;
import org.junit.jupiter.api.Test;

class TalentPassiveRulesTest {
   @Test
   void passiveRanksExposeApprovedValues() {
      assertEquals(20.0, PassiveRank.E.clairvoyanceProficiency());
      assertEquals(12.0, PassiveRank.A.maxZoom());
      assertEquals(50.0, PassiveRank.A.healthBonus());
      assertEquals(10.0, PassiveRank.A.attackBonus());
      assertEquals(0.325, PassiveRank.B.dodgeChance());
      assertEquals(PassiveRank.D, PassiveRank.E.next());
      assertEquals(PassiveRank.A, PassiveRank.A.next());
   }

   @Test
   void talentLearningAndPracticeEntryPointsAlwaysRejectTalents() {
      for (String id : TalentService.IDS) {
         assertFalse(MagicLearningStrategy.canAnalyze(id));
         assertFalse(MagicLearningStrategy.canLearnFromMaterial(id));
         assertFalse(MagicLearningStrategy.canResearch(id));
         assertFalse(MagicLearningStrategy.canCopy(id));
      }
   }

   @Test
   void monstrousStrengthMapsProficiencyToRankAndDuration() {
      assertEquals(1, TalentService.monstrousStrengthLevel(0.0));
      assertEquals(2, TalentService.monstrousStrengthLevel(20.0));
      assertEquals(6, TalentService.monstrousStrengthLevel(100.0));
      assertEquals(1_200, TalentService.monstrousStrengthDurationTicks(0.0));
      assertEquals(6_000, TalentService.monstrousStrengthDurationTicks(100.0));
   }

   @Test
   void clairvoyanceZoomUsesTwoStepRankBands() {
      assertEquals(2, TalentService.maxZoom(0.0));
      assertEquals(4, TalentService.maxZoom(20.0));
      assertEquals(8, TalentService.maxZoom(60.0));
      assertEquals(12, TalentService.maxZoom(100.0));
   }

   @Test
   void mindEyesUseHigherRankAndInstinctStacks() {
      assertEquals(0.325, PassiveService.dodgeChance(PassiveRank.D, PassiveRank.B, null), 1.0E-9);
      assertEquals(0.575, PassiveService.dodgeChance(PassiveRank.D, PassiveRank.B, PassiveRank.C), 1.0E-9);
      assertEquals(0.80, PassiveService.dodgeChance(PassiveRank.A, PassiveRank.B, PassiveRank.A), 1.0E-9);
   }

   @Test
   void servantAndMasterCardFormsBothSuppressPlayerEffects() {
      assertFalse(PassiveService.effectsSuppressed(false, false));
      assertTrue(PassiveService.effectsSuppressed(true, false));
      assertTrue(PassiveService.effectsSuppressed(false, true));
      assertTrue(PassiveService.effectsSuppressed(true, true));
   }

   @Test
   void clairvoyanceUsesHigherOfIndependentAndPassiveSource() {
      assertEquals(60.0, TalentService.effectiveClairvoyanceProficiency(30.0, PassiveRank.C));
      assertEquals(90.0, TalentService.effectiveClairvoyanceProficiency(90.0, PassiveRank.C));
   }

   @Test
   void martialAwakeningChanceUsesTotalAndHighest() {
      assertEquals(20.0, MartialPassiveProgressionService.chancePercent(150.0, 50.0));
      assertEquals(50.0, MartialPassiveProgressionService.chancePercent(400.0, 100.0));
   }
}
