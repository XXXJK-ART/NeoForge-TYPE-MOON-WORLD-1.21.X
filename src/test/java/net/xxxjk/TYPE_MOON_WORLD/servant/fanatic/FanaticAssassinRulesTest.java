package net.xxxjk.TYPE_MOON_WORLD.servant.fanatic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantParams;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.StatRank;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.MagicResistanceRank;
import org.junit.jupiter.api.Test;

class FanaticAssassinRulesTest {
   @Test
   void ranksProduceTheApprovedNpcStats() {
      ServantParams params = ServantParams.of("B", false, "C", false, "A", false, "C", false, "D", false);
      assertEquals(400.0, params.maxHealth());
      assertEquals(15.0, params.attackDamage());
      assertEquals(0.36, params.movementSpeed());
      assertEquals(12.0, params.armor());
      assertEquals(600.0, params.manaPool());
      assertEquals(4.0, params.critRatePercent());
   }

   @Test
   void mpCostsAndCooldownsMatchTheDesign() {
      assertEquals(50, FanaticAssassinRules.HEARTBEAT_MP);
      assertEquals(40, FanaticAssassinRules.MARROW_MP);
      assertEquals(30, FanaticAssassinRules.HAIR_MP);
      assertEquals(30, FanaticAssassinRules.TEMPERATURE_MP);
      assertEquals(20, FanaticAssassinRules.NERVES_MP);
      assertEquals(40, FanaticAssassinRules.COMPUTER_MP);
      assertEquals(30, FanaticAssassinRules.TOXIN_MP);
      assertEquals(60, FanaticAssassinRules.JINN_MP);
      assertEquals(600, FanaticAssassinRules.HEARTBEAT_COOLDOWN);
      assertEquals(600, FanaticAssassinRules.MARROW_COOLDOWN);
      assertEquals(600, FanaticAssassinRules.HAIR_COOLDOWN);
      assertEquals(600, FanaticAssassinRules.TEMPERATURE_COOLDOWN);
      assertEquals(600, FanaticAssassinRules.NERVES_COOLDOWN);
      assertEquals(600, FanaticAssassinRules.COMPUTER_COOLDOWN);
      assertEquals(600, FanaticAssassinRules.TOXIN_COOLDOWN);
      assertEquals(600, FanaticAssassinRules.JINN_COOLDOWN);
   }

   @Test
   void heartbeatUsesMagicResistanceAndLuckInsteadOfTheMagicParameter() {
      assertEquals(250.0F, FanaticAssassinRules.heartbeatDamage(MagicResistanceRank.C, StatRank.D));
      assertEquals(125.0F, FanaticAssassinRules.heartbeatDamage(MagicResistanceRank.B, StatRank.D));
      assertEquals(125.0F, FanaticAssassinRules.heartbeatDamage(MagicResistanceRank.C, StatRank.B));
      assertEquals(125.0F, FanaticAssassinRules.heartbeatDamage(MagicResistanceRank.A, StatRank.E));
      assertEquals(250.0F, FanaticAssassinRules.heartbeatDamage(MagicResistanceRank.NONE, null));
   }

   @Test
   void areaAndContactBoundariesAreExact() {
      assertEquals(15.0, FanaticAssassinRules.MARROW_RADIUS);
      assertEquals(150.0F, FanaticAssassinRules.MARROW_DAMAGE);
      assertEquals(100.0F, FanaticAssassinRules.HAIR_DAMAGE);
      assertEquals(1.8, FanaticAssassinRules.COMPUTER_RANGE);
      assertEquals(150.0F, FanaticAssassinRules.computerSplashDamage(0.0));
      assertEquals(75.0F, FanaticAssassinRules.computerSplashDamage(1.0));
      assertEquals(0.0F, FanaticAssassinRules.computerSplashDamage(2.0));
      assertEquals(0.0F, FanaticAssassinRules.computerSplashDamage(2.01));
   }

   @Test
   void retreatUsesHysteresis() {
      assertTrue(FanaticAssassinRules.shouldRetreat(119.99, 600.0));
      assertFalse(FanaticAssassinRules.shouldRetreat(120.0, 600.0));
      assertFalse(FanaticAssassinRules.recoveredFromRetreat(179.99, 600.0));
      assertTrue(FanaticAssassinRules.recoveredFromRetreat(180.0, 600.0));
   }

   @Test
   void toxinJinnAndFanaticismTimingsAreStable() {
      assertEquals(160, FanaticAssassinRules.TOXIN_DURATION);
      assertEquals(10.0F, FanaticAssassinRules.TOXIN_DAMAGE_PER_SECOND);
      assertEquals(200, FanaticAssassinRules.JINN_MAX_HEALTH);
      assertEquals(30.0F, FanaticAssassinRules.JINN_DAMAGE);
      assertEquals(600, FanaticAssassinRules.JINN_LIFETIME);
      assertEquals(0, FanaticAssassinRules.jinnForm(4.0));
      assertEquals(1, FanaticAssassinRules.jinnForm(4.01));
      assertEquals(0.70F, FanaticAssassinRules.MENTAL_ATTACK_CANCEL_CHANCE);
      assertEquals(1200, FanaticAssassinRules.MENTAL_CLEANSE_INTERVAL);
      assertTrue(FanaticAssassinRules.shouldCancelMentalAttack(0.6999F));
      assertFalse(FanaticAssassinRules.shouldCancelMentalAttack(0.70F));
      assertTrue(FanaticAssassinRules.shouldAttemptCombo(0.2999F));
      assertFalse(FanaticAssassinRules.shouldAttemptCombo(0.30F));
      assertEquals(6, FanaticAssassinRules.MAX_COMBO_TECHNIQUES);
      assertTrue(FanaticAssassinRules.canContinueCombo(1));
      assertTrue(FanaticAssassinRules.canContinueCombo(5));
      assertFalse(FanaticAssassinRules.canContinueCombo(6));
   }

   @Test
   void cooldownAtWorldTickZeroAndSelectionPriorityAreStable() {
      assertTrue(FanaticAssassinRules.cooldownReady(0L, false, 0L, 400));
      assertFalse(FanaticAssassinRules.cooldownReady(1L, true, 0L, 400));
      assertTrue(FanaticAssassinRules.cooldownReady(400L, true, 0L, 400));
      assertEquals(FanaticAssassinRules.TechniqueDecision.COMPUTER,
         FanaticAssassinRules.chooseTechnique(true, true, true, true, true, true, true, true));
      assertEquals(FanaticAssassinRules.TechniqueDecision.MARROW,
         FanaticAssassinRules.chooseTechnique(false, false, true, true, true, true, true, true));
      assertEquals(FanaticAssassinRules.TechniqueDecision.NERVES,
         FanaticAssassinRules.chooseTechnique(false, false, false, false, true, true, true, true));
      assertEquals(FanaticAssassinRules.TechniqueDecision.TOXIN,
         FanaticAssassinRules.chooseTechnique(false, false, false, false, false, false, false, true));
      assertEquals(FanaticAssassinRules.TechniqueDecision.BASIC,
         FanaticAssassinRules.chooseTechnique(false, false, false, false, false, false, false, false));
   }
}
