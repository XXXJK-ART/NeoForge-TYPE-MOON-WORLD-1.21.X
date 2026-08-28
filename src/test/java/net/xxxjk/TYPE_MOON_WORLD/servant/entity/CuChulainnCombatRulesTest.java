package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CuChulainnCombatRulesTest {
   @Test
   void singleTargetGaeBolgPrefersMeleeAcrossTheClosingBand() {
      assertEquals(CuChulainnCombatRules.SingleGaeBolgPlan.MELEE,
         CuChulainnCombatRules.singleGaeBolgPlan(3.75));
      assertEquals(CuChulainnCombatRules.SingleGaeBolgPlan.CLOSE_FOR_MELEE,
         CuChulainnCombatRules.singleGaeBolgPlan(5.0));
      assertEquals(CuChulainnCombatRules.SingleGaeBolgPlan.NONE,
         CuChulainnCombatRules.singleGaeBolgPlan(9.0));
      assertEquals(CuChulainnCombatRules.SingleGaeBolgPlan.NONE,
         CuChulainnCombatRules.singleGaeBolgPlan(12.1));
   }

   @Test
   void meleeGaeBolgReleaseUsesFiveBlockLockAndTwoSecondPursuit() {
      assertTrue(CuChulainnCombatRules.canReleaseMeleeGaeBolg(5.0));
      assertFalse(CuChulainnCombatRules.canReleaseMeleeGaeBolg(5.01));
      assertFalse(CuChulainnCombatRules.canReleaseMeleeGaeBolg(Double.NaN));
      assertEquals(40, CuChulainnCombatRules.MELEE_GAE_BOLG_PURSUIT_TICKS);
   }

   @Test
   void armyGaeBolgRequiresARealFinalWindow() {
      assertFalse(CuChulainnCombatRules.isArmyFinalWindow(0.50, true, 4, 300.0, 1.0));
      assertFalse(CuChulainnCombatRules.isArmyFinalWindow(0.10, false, 4, 300.0, 1.0));
      assertFalse(CuChulainnCombatRules.isArmyFinalWindow(0.10, true, 4, 300.0, 0.50));
      assertFalse(CuChulainnCombatRules.isArmyFinalWindow(0.10, true, 1, 100.0, 1.0));
      assertTrue(CuChulainnCombatRules.isArmyFinalWindow(0.10, true, 3, 100.0, 1.0));
      assertTrue(CuChulainnCombatRules.isArmyFinalWindow(0.10, true, 1, 160.0, 1.0));
   }

   @Test
   void armyDecisionIsThrottledAndNeverReturnsOldHighChances() {
      assertTrue(CuChulainnCombatRules.isArmyDecisionDue(100L, 0L));
      assertFalse(CuChulainnCombatRules.isArmyDecisionDue(139L, 100L));
      assertTrue(CuChulainnCombatRules.isArmyDecisionDue(140L, 100L));
      assertEquals(8, CuChulainnCombatRules.armyUseChance(0.10, 1));
      assertEquals(18, CuChulainnCombatRules.armyUseChance(0.10, 3));
      assertEquals(30, CuChulainnCombatRules.armyUseChance(0.08, 1));
   }
}
