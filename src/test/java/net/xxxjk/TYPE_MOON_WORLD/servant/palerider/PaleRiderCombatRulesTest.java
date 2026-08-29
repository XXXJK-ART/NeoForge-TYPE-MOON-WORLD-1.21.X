package net.xxxjk.TYPE_MOON_WORLD.servant.palerider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PaleRiderCombatRulesTest {
   @Test
   void crowLimitStopsAtFiftyEntities() {
      assertTrue(PaleRiderCombatRules.canSpawnCrow(49));
      assertFalse(PaleRiderCombatRules.canSpawnCrow(50));
      assertFalse(PaleRiderCombatRules.canSpawnCrow(51));
      assertEquals(50, PaleRiderCombatRules.MAX_CROWS);
   }

   @Test
   void deathJudgmentRunsEveryTenSeconds() {
      assertEquals(200, PaleRiderCombatRules.DEATH_JUDGMENT_INTERVAL_TICKS);
   }

   @Test
   void minorSkillsHaveDistinctCadenceAndCosts() {
      assertEquals(100, PaleRiderCombatRules.ASH_STEP_COOLDOWN_TICKS);
      assertEquals(140, PaleRiderCombatRules.PLAGUE_RUSH_COOLDOWN_TICKS);
      assertEquals(200, PaleRiderCombatRules.DEATH_PULSE_COOLDOWN_TICKS);
      assertEquals(30, PaleRiderCombatRules.MINOR_SKILL_LOCK_TICKS);
      assertEquals(8.0, PaleRiderCombatRules.ASH_STEP_MP_COST);
      assertEquals(12.0, PaleRiderCombatRules.PLAGUE_RUSH_MP_COST);
      assertEquals(18.0, PaleRiderCombatRules.DEATH_PULSE_MP_COST);
   }

   @Test
   void plagueDealsFourfoldDamageToEnkiduOnly() {
      assertEquals(4.0F, PaleRiderCombatRules.plagueSpecialAttackMultiplier("enkidu"));
      assertEquals(1.0F, PaleRiderCombatRules.plagueSpecialAttackMultiplier("heracles"));
      assertEquals(1.0F, PaleRiderCombatRules.plagueSpecialAttackMultiplier(""));
      assertEquals(1.0F, PaleRiderCombatRules.plagueSpecialAttackMultiplier(null));
   }

   @Test
   void domainsUseConfiguredManaCosts() {
      assertEquals(150.0, PaleRiderCombatRules.UNDERWORLD_CAST_MP_COST);
      assertEquals(300.0, PaleRiderCombatRules.CALAMITY_CAST_MP_COST);
      assertEquals(10.0, PaleRiderCombatRules.CALAMITY_UPKEEP_MP_PER_SECOND);
   }
}
