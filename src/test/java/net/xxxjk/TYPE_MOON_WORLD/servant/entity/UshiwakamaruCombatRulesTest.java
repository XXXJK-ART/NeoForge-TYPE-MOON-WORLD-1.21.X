package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.SpiderCutterItem;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardRegistry;

class UshiwakamaruCombatRulesTest {
   @Test
   void phasesAdvanceMonotonicallyAtHealthThresholds() {
      assertEquals(1, UshiwakamaruCombatRules.phaseFor(67.0F, 100.0F, 1));
      assertEquals(2, UshiwakamaruCombatRules.phaseFor(66.0F, 100.0F, 1));
      assertEquals(3, UshiwakamaruCombatRules.phaseFor(33.0F, 100.0F, 2));
      assertEquals(3, UshiwakamaruCombatRules.phaseFor(90.0F, 100.0F, 3));
   }

   @Test
   void swallowAndRidingValuesMatchDesign() {
      assertEquals(0.30F, UshiwakamaruCombatRules.swallowDodgeChance(false));
      assertEquals(0.45F, UshiwakamaruCombatRules.swallowDodgeChance(true));
      assertEquals(85.0F, UshiwakamaruCombatRules.applyRidingDefense(100.0F));
   }

   @Test
   void shieldAbsorbsOverflowWithoutReturningIt() {
      assertEquals(1000.0F, UshiwakamaruCombatRules.SHIELD_MAX_HP);
      var hit = UshiwakamaruCombatRules.absorbShieldHit(UshiwakamaruCombatRules.SHIELD_MAX_HP, 1250.0F);
      assertEquals(0.0F, hit.remainingShieldHp());
      assertTrue(hit.broken());
   }

   @Test
   void spiderCutterMatchesHasebeCombatValues() {
      assertEquals(1800, SpiderCutterItem.DURABILITY);
      assertEquals(10.0, SpiderCutterItem.ATTACK_DAMAGE);
      assertEquals(1.5, SpiderCutterItem.ATTACK_SPEED);
   }

   @Test
   void riderIsNpcOnlyAndNotExposedAsServantCard() {
      assertTrue(ServantCardRegistry.byId("ushiwakamaru_rider") == null);
      assertTrue(ServantCardRegistry.byId("typemoonworld:ushiwakamaru_rider") == null);
   }
}
