package net.xxxjk.TYPE_MOON_WORLD.servant.combat;

import static org.junit.jupiter.api.Assertions.*;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantParams;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.SkillRank;
import net.xxxjk.TYPE_MOON_WORLD.utils.ManaRegenRules;
import org.junit.jupiter.api.Test;

class DefenseRulesTest {
   @Test void npcGuardsCoverMartialSkillsAndLightRangedHits() {
      assertTrue(DefenseRules.npcGuardCandidate(150, true, false, false, false));
      assertTrue(DefenseRules.npcGuardCandidate(40, false, false, false, false));
      assertFalse(DefenseRules.npcGuardCandidate(40.01F, false, false, false, false));
      assertFalse(DefenseRules.npcGuardCandidate(20, true, true, false, false));
      assertFalse(DefenseRules.npcGuardCandidate(20, true, false, true, false));
      assertFalse(DefenseRules.npcGuardCandidate(20, false, false, false, true));
      assertFalse(DefenseRules.npcGuardCandidate(0, true, false, false, false));
      assertFalse(DefenseRules.npcGuardCandidate(Float.NaN, true, false, false, false));
      assertFalse(DefenseRules.npcGuardCandidate(Float.POSITIVE_INFINITY, true, false, false, false));
   }
   @Test void ordinaryAttacksLeaveRoomToGuardDodgeOrKeepAttacking() {
      var chances = DefenseRules.npcReactionChances(4, 4, 30, 500, 1, true, true);
      assertEquals(0.14, chances.guard(), 1e-9);
      assertEquals(0.26, chances.dodge(), 1e-9);
      assertEquals(DefenseRules.NpcReaction.DODGE, chances.choose(0.10, 0.10));
      assertEquals(DefenseRules.NpcReaction.GUARD, chances.choose(0.90, 0.10));
      assertEquals(DefenseRules.NpcReaction.NONE, chances.choose(0.90, 0.90));
   }
   @Test void damageAndDangerShiftPreferenceTowardDodging() {
      var light = DefenseRules.npcReactionChances(4, 4, 30, 500, 1, true, true);
      for (var strong : new DefenseRules.NpcReactionChances[]{
         DefenseRules.npcReactionChances(4, 4, 160, 500, 1, true, true),
         DefenseRules.npcReactionChances(4, 4, 30, 100, 1, true, true),
         DefenseRules.npcReactionChances(4, 4, 0, 500, 5, true, true)
      }) {
         assertTrue(strong.dodge() > strong.guard());
         assertTrue(strong.dodge() > light.dodge());
         assertTrue(strong.guard() < light.guard());
         assertTrue(strong.guard() + strong.dodge() < 1.0);
      }
   }
   @Test void unavailableDefensesDoNotForceTheOtherChoice() {
      var both = DefenseRules.npcReactionChances(4, 4, 30, 500, 1, true, true);
      var noDodge = DefenseRules.npcReactionChances(4, 4, 30, 500, 1, true, false);
      var noGuard = DefenseRules.npcReactionChances(4, 4, 30, 500, 1, false, true);
      assertEquals(both.guard(), noDodge.guard());
      assertEquals(both.dodge(), noGuard.dodge());
      assertEquals(0, noDodge.dodge());
      assertEquals(0, noGuard.guard());
      assertEquals(DefenseRules.NpcReaction.NONE,
         DefenseRules.npcReactionChances(4, 4, 30, 500, 1, false, false).choose(0, 0));
      assertEquals(DefenseRules.NpcReaction.GUARD, noDodge.choose(0, 0));
      assertEquals(DefenseRules.NpcReaction.NONE, noDodge.choose(0, 0.90));
   }
   @Test void everyRankAndThreatKeepsAnOffensiveOpening() {
      for (int defense = 0; defense <= 6; defense++)
         for (int agility = 0; agility <= 6; agility++)
            for (int danger = 0; danger <= 5; danger++) {
               var chances = DefenseRules.npcReactionChances(defense, agility, 1000, 50, danger, true, true);
               assertTrue(chances.guard() > 0 && chances.dodge() > 0);
               assertTrue(chances.guard() + chances.dodge() <= 0.85);
            }
   }
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
