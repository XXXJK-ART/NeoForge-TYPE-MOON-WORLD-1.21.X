package net.xxxjk.TYPE_MOON_WORLD.servant.heracles;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.CommonSkillRules;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.DamageParts;

class HeraclesRulesTest {
   @Test void allLetterRanksUseFixedAddition() {
      for(int r=1;r<=5;r++) {
         var madness=CommonSkillRules.bonus(r,0);var divinity=CommonSkillRules.bonus(0,r);
         assertEquals(20*r,madness.health());assertEquals(3*r,madness.attack());assertEquals(.02*r,madness.speed(),1e-9);
         assertEquals(20*r,divinity.health());assertEquals(2*r,divinity.attack());assertEquals(.01*r,divinity.speed(),1e-9);
      }
      var heracles=CommonSkillRules.bonus(4,5);
      assertEquals(680,500+heracles.health());assertEquals(72,50+heracles.attack());assertEquals(.13,heracles.speed(),1e-9);
      assertEquals(5,CommonSkillRules.highest(List.of("divinity_b_plus","divinity_a_minus","typemoonworld:divinity_c"),"divinity_"));
      assertEquals(0,CommonSkillRules.rank("EX"));assertEquals(4,CommonSkillRules.rank("B+"));
   }
   @Test void continuationHasNoConstantBonusAndUsesDoubledCooldown() {
      double[] ratios={.01,.03,.05,.07,.10};
      for(int r=1;r<=5;r++) {
         var rule=CommonSkillRules.continuation(r);
         assertEquals(ratios[r-1],rule.healthRatio());assertEquals(20*r,rule.protectionTicks());
         assertEquals(2*r,rule.heal());assertEquals((20-2*r)*1200,rule.cooldownTicks());
      }
   }
   @Test void exactThresholdAndDivineIncrementAreIndependent() {
      assertEquals(0,new DamageParts(14.99F,0).godHand(false));
      assertEquals(12,new DamageParts(15,0).godHand(false));assertEquals(3,new DamageParts(15,0).godHand(true));
      assertEquals(80,new DamageParts(100,0).godHand(false));assertEquals(20,new DamageParts(100,0).godHand(true));
      assertEquals(50,new DamageParts(10,50).godHand(false));assertEquals(50,new DamageParts(10,50).godHand(true));
      assertEquals(82,new DamageParts(40,50).godHand(false));assertEquals(58,new DamageParts(40,50).godHand(true));
   }
   @Test void adaptationCompletesOnlyAtTick100() {
      var s=GodHandState.fresh().revive("sword",false);
      for(int i=0;i<99;i++)s=s.tick();
      assertEquals(1,s.remainingTicks());assertFalse(s.adapted().contains("sword"));
      s=s.tick();assertTrue(s.adapted().contains("sword"));assertEquals(0,s.remainingTicks());
   }
   @Test void newDeathReplacesOnlyTheUnfinishedCandidate() {
      var s=GodHandState.fresh().revive("old",false);
      for(int i=0;i<100;i++)s=s.tick();
      s=s.revive("sword",false);for(int i=0;i<99;i++)s=s.tick();
      s=s.revive("axe",false);assertEquals(100,s.remainingTicks());assertEquals("axe",s.pending());
      assertTrue(s.adapted().contains("old"));assertFalse(s.adapted().contains("sword"));assertEquals(8,s.lives());
   }
   @Test void poisonAndSpecialImmunityDoNotGrantGeneralAdaptation() {
      var s=GodHandState.fresh().revive("poison",true);
      assertEquals(10,s.lives());assertEquals(0,s.remainingTicks());assertTrue(s.adapted().isEmpty());
      s=s.special("medusa:cybele");assertTrue(s.specialImmunities().contains("medusa:cybele"));
      assertFalse(s.specialImmunities().contains("cursed_arm_hassan:zabaniya"));assertEquals(9,s.lives());
      assertThrows(UnsupportedOperationException.class,()->GodHandState.fresh().adapted().add("mutable"));
   }
}
