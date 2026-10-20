package net.xxxjk.TYPE_MOON_WORLD.servant.sasaki;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SasakiKojiroRulesTest {
   @Test
   void souwaDefenseBypassUsesTheConfirmedRankChances() {
      assertEquals(0.01, SasakiKojiroRules.souwaDefenseBypassChance("E"), 1.0E-9);
      assertEquals(0.05, SasakiKojiroRules.souwaDefenseBypassChance("D"), 1.0E-9);
      assertEquals(0.10, SasakiKojiroRules.souwaDefenseBypassChance("C"), 1.0E-9);
      assertEquals(0.15, SasakiKojiroRules.souwaDefenseBypassChance("B"), 1.0E-9);
      assertEquals(0.20, SasakiKojiroRules.souwaDefenseBypassChance("A"), 1.0E-9);
      assertEquals(0.0, SasakiKojiroRules.souwaDefenseBypassChance(""), 1.0E-9);
   }

   @Test
   void souwaRanksAddFivePercentPerRankAndPerfectParryStaysFree() {
      assertEquals(0.05, SasakiKojiroRules.souwaBonus("E"), 1.0E-9);
      assertEquals(0.10, SasakiKojiroRules.souwaBonus("D"), 1.0E-9);
      assertEquals(0.15, SasakiKojiroRules.souwaBonus("C"), 1.0E-9);
      assertEquals(0.20, SasakiKojiroRules.souwaBonus("B"), 1.0E-9);
      assertEquals(0.25, SasakiKojiroRules.souwaBonus("A"), 1.0E-9);
      assertEquals(0.0, SasakiKojiroRules.guardStaminaCost(10.0, 0.0, true, true, 0.25), 1.0E-9);
   }

   @Test
   void guardCostAppliesDefenderReductionBeforeSouwa() {
      assertEquals(10.8, SasakiKojiroRules.guardStaminaCost(10.0, 0.10, true, false, 0.20), 1.0E-9);
      assertEquals(15.75, SasakiKojiroRules.guardStaminaCost(10.0, 0.25, false, false, 0.05), 1.0E-9);
   }

   @Test
   void concealmentParticleCadenceUsesActualSkillRank() {
      assertEquals(25, SasakiKojiroRules.concealmentParticles("E"));
      assertEquals(6, SasakiKojiroRules.concealmentParticles("D"));
      assertEquals(15, SasakiKojiroRules.concealmentParticles("C"));
      assertEquals(10, SasakiKojiroRules.concealmentParticles("B"));
      assertEquals(5, SasakiKojiroRules.concealmentParticles("A"));
      assertEquals(0, SasakiKojiroRules.concealmentParticles("A+"));
      assertEquals(0, SasakiKojiroRules.concealmentParticles("EX"));
      assertEquals(5, SasakiKojiroRules.concealmentParticleInterval("D"));
      assertEquals(10, SasakiKojiroRules.concealmentParticleInterval("C"));
   }

   @Test
   void techniqueSlotsMatchTheCardTableAndKeepTsubameAsAnOrdinarySkill() {
      double[] costs = {8.0, 12.0, 8.0, 10.0, 12.0, 14.0, 16.0, 18.0, 20.0};
      int[] cooldowns = {20, 80, 60, 80, 80, 100, 140, 180, 600};
      int[] slots = {-1, 1, 2, 3, 4, 6, 7, 8, 9};
      for (int i = 0; i < costs.length; i++) {
         SasakiKojiroRules.Technique technique = SasakiKojiroRules.Technique.forSlot(slots[i]);
         assertEquals(costs[i], technique.mpCost());
         assertEquals(cooldowns[i], technique.cooldownTicks());
      }
      assertEquals(450.0F, SasakiKojiroRules.Technique.TSUBAME_GAESHI.damage());
      assertEquals(8.0, SasakiKojiroRules.Technique.TSUBAME_GAESHI.range());
   }
}
