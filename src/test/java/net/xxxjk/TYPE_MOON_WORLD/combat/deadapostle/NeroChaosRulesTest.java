package net.xxxjk.TYPE_MOON_WORLD.combat.deadapostle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class NeroChaosRulesTest {
   @Test
   void fullBRankProfileMatchesSpecification() {
      DeadApostleCombatProfile profile = DeadApostleCombatProfile.neroChaos();

      assertEquals(400.0, profile.maxHealth());
      assertEquals(20.0, profile.attackDamage());
      assertEquals(0.32, profile.movementSpeed());
      assertEquals(12.0, profile.armor());
      assertEquals(48.0, profile.followRange());
      assertEquals(170.0, profile.poiseMax());
      assertEquals(11.0, profile.poiseRegenPerSecond());
      assertEquals(0.35, profile.blockReduction());
      assertEquals(0.72, profile.dodgeChance());
      assertEquals(0.90, profile.urgentDodgeChance());
      assertEquals(13, profile.dodgeCooldownTicks());
      assertEquals(9, profile.dodgeInvulnerabilityTicks());
   }

   @Test
   void lifeAndPhaseBoundariesAreExact() {
      assertEquals(666, NeroChaosRules.clampLives(999));
      assertEquals(0, NeroChaosRules.clampLives(-1));
      assertEquals(665, NeroChaosRules.consumeLife(666));
      assertTrue(NeroChaosRules.shouldReviveAfterLethal(2, false));
      assertFalse(NeroChaosRules.shouldReviveAfterLethal(1, false));
      assertFalse(NeroChaosRules.shouldReviveAfterLethal(666, true));

      assertEquals(20, NeroChaosRules.combatBeastTarget(301));
      assertEquals(30, NeroChaosRules.combatBeastTarget(300));
      assertEquals(40, NeroChaosRules.combatBeastTarget(100));
      assertEquals(50, NeroChaosRules.combatBeastTarget(99));
      assertFalse(NeroChaosRules.shouldEnterChaosForm(100, 50));
      assertTrue(NeroChaosRules.shouldEnterChaosForm(99, 50));
   }
}
