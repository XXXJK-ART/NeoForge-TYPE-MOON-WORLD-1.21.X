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
   void personaAttackAndSpeedDoNotDecayWithCount() {
      for (int liveCount : new int[] {1, 10, 80}) {
         assertEquals(7.0, HundredFacesHassanRules.PERSONA_ATTACK_DAMAGE);
         assertEquals(0.23, HundredFacesHassanRules.PERSONA_MOVEMENT_SPEED);
         assertTrue(HundredFacesHassanRules.personaHealthForCount(liveCount) >= HundredFacesHassanRules.PERSONA_MIN_HEALTH);
         assertTrue(HundredFacesHassanRules.personaArmorForCount(liveCount) >= HundredFacesHassanRules.PERSONA_MIN_ARMOR);
      }
   }

   @Test
   void personaDurabilityDecaysButStaysThreateningAtFullSwarm() {
      assertEquals(100.0, HundredFacesHassanRules.personaHealthForCount(1), 1.0E-9);
      assertEquals(4.0, HundredFacesHassanRules.personaArmorForCount(1), 1.0E-9);
      assertEquals(100.0, HundredFacesHassanRules.personaHealthForCount(10), 1.0E-9);
      assertTrue(HundredFacesHassanRules.personaArmorForCount(10) < HundredFacesHassanRules.personaArmorForCount(1));
      assertEquals(100.0, HundredFacesHassanRules.personaHealthForCount(80), 1.0E-9);
      assertEquals(3.0, HundredFacesHassanRules.personaArmorForCount(80), 1.0E-9);
   }
}
