package net.xxxjk.TYPE_MOON_WORLD.servant.nightingale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantParams;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardSkillAction;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardSkillLayout;
import org.junit.jupiter.api.Test;

class NightingaleRulesTest {
   @Test
   void currentPlusRankConversionProducesRequestedThousandHealthVersion() {
      ServantParams params = ServantParams.of("A", true, "B", true, "B", true, "D", true, "A", true);
      assertEquals(1000.0, params.maxHealth());
      assertEquals(40.0, params.attackDamage());
      assertEquals(15.0, params.armor());
      assertEquals(800.0, params.manaPool());
      assertEquals(20.0, params.critRatePercent());
   }

   @Test
   void servantCardUsesOnlyTheThreeApprovedSlots() {
      ServantCardSkillAction nursing = ServantCardSkillLayout.actionFor("nightingale", 0, false);
      ServantCardSkillAction cry = ServantCardSkillLayout.actionFor("nightingale", 1, false);
      ServantCardSkillAction pledge = ServantCardSkillLayout.actionFor("nightingale", 9, false);
      assertEquals("nightingale_steel_nursing", nursing.effectId());
      assertEquals(15.0, nursing.mpCost()); assertEquals(200, nursing.cooldownTicks());
      assertEquals("nightingale_angel_cry", cry.effectId());
      assertEquals(10.0, cry.mpCost()); assertEquals(300, cry.cooldownTicks());
      assertEquals("nightingale_pledge", pledge.effectId());
      assertEquals(50.0, pledge.mpCost()); assertEquals(600, pledge.cooldownTicks());
      for (int slot = 2; slot < 9; slot++) assertTrue(ServantCardSkillLayout.actionFor("nightingale", slot, false) == null);
   }

   @Test
   void costsDurationsAndDamageStackExactly() {
      assertEquals(15, NightingaleRules.STEEL_NURSING_COST);
      assertEquals(200, NightingaleRules.STEEL_NURSING_COOLDOWN);
      assertEquals(150.0F, NightingaleRules.STEEL_NURSING_AMOUNT);
      assertEquals(10, NightingaleRules.ANGEL_CRY_COST);
      assertEquals(300, NightingaleRules.ANGEL_CRY_COOLDOWN);
      assertEquals(400, NightingaleRules.ANGEL_CRY_DURATION);
      assertEquals(50, NightingaleRules.NOBLE_PHANTASM_COST);
      assertEquals(600, NightingaleRules.NOBLE_PHANTASM_COOLDOWN);
      assertEquals(20, NightingaleRules.NOBLE_PHANTASM_WINDUP);
      assertEquals(100, NightingaleRules.SAFETY_CIRCLE_DURATION);
      assertEquals(28.125F, 15.0F * NightingaleRules.outgoingMultiplier(true, false), 0.0001F);
      assertEquals(36.5625F, 15.0F * NightingaleRules.outgoingMultiplier(true, true), 0.0001F);
   }

   @Test
   void noblePhantasmThresholdsAreInclusive() {
      assertTrue(NightingaleRules.shouldUseNoblePhantasm(0.35F, 1.0F, 0));
      assertTrue(NightingaleRules.shouldUseNoblePhantasm(1.0F, 0.20F, 0));
      assertTrue(NightingaleRules.shouldUseNoblePhantasm(1.0F, 1.0F, 2));
      assertFalse(NightingaleRules.shouldUseNoblePhantasm(0.351F, 0.201F, 1));
   }

   @Test
   void innocentRetaliationAndNursingPriorityFollowContract() {
      assertFalse(NightingaleRules.canAttackNormallyInnocent(1000L, 0L));
      assertTrue(NightingaleRules.canAttackNormallyInnocent(1000L, 600L));
      assertFalse(NightingaleRules.canAttackNormallyInnocent(1001L, 600L));
      assertTrue(NightingaleRules.isNursingEligible(80.0F, 100.0F));
      assertFalse(NightingaleRules.isNursingEligible(80.01F, 100.0F));
      assertTrue(NightingaleRules.nursingPriority(25.0F, 100.0F) < NightingaleRules.nursingPriority(40.0F, 100.0F));
   }

   @Test
   void angelCryAcceleratesActionRecoveryByTwentyPercent() {
      assertEquals(20, NightingaleRules.adjustActionTicks(20, false));
      assertEquals(17, NightingaleRules.adjustActionTicks(20, true));
      assertEquals(1, NightingaleRules.adjustActionTicks(1, true));
   }
}
