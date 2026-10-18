package net.xxxjk.TYPE_MOON_WORLD.servant.combat;

import static org.junit.jupiter.api.Assertions.*;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantParams;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.SkillRank;
import net.xxxjk.TYPE_MOON_WORLD.utils.ManaRegenRules;
import org.junit.jupiter.api.Test;

class DefenseRulesTest {
   @Test void guardsUseFixedReductionAndOneResourceCost() {
      assertEquals(50, DefenseRules.guardedDamage(100, false, false));
      assertEquals(20, DefenseRules.guardedDamage(100, true, false));
      assertEquals(0, DefenseRules.guardedDamage(100, true, true));
      assertEquals(24, DefenseRules.guardCost(12, 0, false, false));
      assertEquals(12, DefenseRules.guardCost(12, 0, true, false));
      assertEquals(0, DefenseRules.guardCost(12, 0.25, true, true));
      assertEquals(10, DefenseRules.poiseLoss(12));
      assertEquals(30, DefenseRules.poiseLoss(60));
   }
   @Test void everyAgilityAndSkillRankLeavesHalfASecondWithoutInvulnerability() {
      for (String agility : new String[]{"E", "D", "C", "B", "A", "A++"})
         for (boolean plus : new boolean[]{false, true})
            for (String skill : new String[]{"E", "D", "C", "B", "A", "A+", "A+++", "EX"}) {
               var p = ServantParams.of("E", false, "E", false, agility, plus, "E", false, "E", false);
               int invulnerability = Math.max(ServantCombatFormulas.dodgeInvulnerabilityTicks(p) + 7,
                  ServantCombatFormulas.perfectDodgeInvulnerabilityTicks(p));
               int cooldown = DefenseRules.safeDodgeCooldown((int)(ServantCombatFormulas.dodgeCooldownTicks(p) * (1 - SkillRank.modifier(skill))), invulnerability);
               assertTrue(cooldown - invulnerability >= 10, agility + "/" + skill);
            }
   }
   @Test void exAndSignedRanksHaveBoundedDistinctSkillEffects() {
      assertEquals(0, SkillRank.autoDodges("E"));
      assertEquals(1, SkillRank.autoDodges("C"));
      assertEquals(2, SkillRank.autoDodges("B"));
      assertEquals(3, SkillRank.autoDodges("A-"));
      assertEquals(4, SkillRank.autoDodges("A+++"));
      assertEquals(4, SkillRank.autoDodges("EX"));
      assertEquals(0.25, SkillRank.modifier("EX"));
   }
   @Test void displayedRegenerationEqualsTickScheduler() {
      assertEquals(14.0, ManaRegenRules.perSecond(100, 0.07, 10, false), 1e-9);
      assertEquals(28.0, ManaRegenRules.perSecond(100, 0.07, 10, true), 1e-9);
      for (double interval : new double[]{0.1, 1, 2, 5, 10, 20, 40})
         for (boolean circuit : new boolean[]{false, true}) {
            int ticks = ManaRegenRules.intervalTicks(interval, circuit);
            assertEquals(100 * 0.07, ManaRegenRules.perSecond(100, 0.07, interval, circuit) * ticks / 20.0, 1e-9);
         }
   }
}
