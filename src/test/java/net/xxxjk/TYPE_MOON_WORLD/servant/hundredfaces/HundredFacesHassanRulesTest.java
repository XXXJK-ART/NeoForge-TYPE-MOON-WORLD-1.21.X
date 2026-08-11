package net.xxxjk.TYPE_MOON_WORLD.servant.hundredfaces;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HundredFacesHassanRulesTest {
   @Test
   void summonLimitsMatchDelusionalIllusionDesign() {
      assertEquals(80, HundredFacesHassanRules.MAX_PERSONAS);
      assertEquals(10, HundredFacesHassanRules.MP_PER_PERSONA);
      assertEquals(10, HundredFacesHassanRules.MAX_SUMMON_BATCH);
      assertEquals(10, HundredFacesHassanRules.affordableSummonCount(1000.0, 0, 80));
      assertEquals(5, HundredFacesHassanRules.affordableSummonCount(50.0, 0, 10));
      assertEquals(2, HundredFacesHassanRules.affordableSummonCount(1000.0, 78, 10));
      assertEquals(0, HundredFacesHassanRules.affordableSummonCount(1000.0, 80, 10));
   }

   @Test
   void mainBodyAlsoWeakensByTotalSplitCount() {
      assertEquals(200.0, HundredFacesHassanRules.mainHealthForSplitCount(0), 1.0E-9);
      assertEquals(15.0, HundredFacesHassanRules.mainAttackDamageForSplitCount(0), 1.0E-9);
      assertEquals(6.0, HundredFacesHassanRules.mainArmorForSplitCount(0), 1.0E-9);
      assertEquals(600.0, HundredFacesHassanRules.mainManaForSplitCount(0), 1.0E-9);
      assertTrue(HundredFacesHassanRules.mainHealthForSplitCount(40) < HundredFacesHassanRules.mainHealthForSplitCount(10));
      assertTrue(HundredFacesHassanRules.mainAttackDamageForSplitCount(40) < HundredFacesHassanRules.mainAttackDamageForSplitCount(10));
      assertEquals(HundredFacesHassanRules.personaHealthForCount(80), HundredFacesHassanRules.mainHealthForSplitCount(80), 1.0E-9);
      assertEquals(HundredFacesHassanRules.personaAttackDamageForCount(80), HundredFacesHassanRules.mainAttackDamageForSplitCount(80), 1.0E-9);
      assertEquals(HundredFacesHassanRules.personaArmorForCount(80), HundredFacesHassanRules.mainArmorForSplitCount(80), 1.0E-9);
      assertEquals(200.0, HundredFacesHassanRules.mainManaForSplitCount(80), 1.0E-9);
   }

   @Test
   void personaAttackDecaysButSpeedDoesNotDecayWithCount() {
      assertEquals(15.0, HundredFacesHassanRules.personaAttackDamageForCount(1), 1.0E-9);
      assertTrue(HundredFacesHassanRules.personaAttackDamageForCount(10) < HundredFacesHassanRules.personaAttackDamageForCount(1));
      assertEquals(5.0, HundredFacesHassanRules.personaAttackDamageForCount(80), 1.0E-9);
      for (int liveCount : new int[] {1, 10, 80}) {
         assertEquals(HundredFacesHassanRules.MAIN_MOVEMENT_SPEED, HundredFacesHassanRules.PERSONA_MOVEMENT_SPEED);
      }
   }

   @Test
   void personaDurabilityDecaysButStaysThreateningAtFullSwarm() {
      assertEquals(100.0, HundredFacesHassanRules.personaHealthForCount(1), 1.0E-9);
      assertEquals(3.0, HundredFacesHassanRules.personaArmorForCount(1), 1.0E-9);
      assertTrue(HundredFacesHassanRules.personaHealthForCount(10) < HundredFacesHassanRules.personaHealthForCount(1));
      assertEquals(20.0, HundredFacesHassanRules.personaHealthForCount(80), 1.0E-9);
      assertEquals(3.0, HundredFacesHassanRules.personaArmorForCount(80), 1.0E-9);
   }

   @Test
   void personaNonSpeedRanksUseELevelBaseline() {
      assertEquals(5.0F, HundredFacesHassanRules.PERSONA_DIRK_DAMAGE, 1.0E-6F);
      assertEquals(5.0, HundredFacesHassanRules.PERSONA_ATTACK_DAMAGE, 1.0E-9);
      assertEquals(200.0, HundredFacesHassanRules.PERSONA_E_RANK_PARAMS.manaPool(), 1.0E-9);
      assertEquals(2.0, HundredFacesHassanRules.PERSONA_E_RANK_PARAMS.critRatePercent(), 1.0E-9);
      assertEquals(0.5, HundredFacesHassanRules.PERSONA_DEFENSE_RECOVERY_MULTIPLIER, 1.0E-9);
   }

   @Test
   void visualHeightIsRelativeToSteveScale() {
      assertEquals(0.8F, HundredFacesHassanRules.visualScaleForHeight(0.8F), 1.0E-6F);
      assertEquals(1.0F, HundredFacesHassanRules.visualScaleForHeight(1.0F), 1.0E-6F);
      assertEquals(1.44F, HundredFacesHassanRules.collisionHeightForVisualHeight(0.8F), 1.0E-6F);
      assertEquals(1.8F, HundredFacesHassanRules.collisionHeightForVisualHeight(1.0F), 1.0E-6F);
   }
}
