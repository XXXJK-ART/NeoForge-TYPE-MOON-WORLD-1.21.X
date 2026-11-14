package net.xxxjk.TYPE_MOON_WORLD.servant.jeanne;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.CommonSkillRules;

class JeanneAlterRulesTest {
   @Test
   void avengerGradesUseAttackFocusedSharedTable() {
      assertEquals(50, JeanneAlterRules.avenger("A").health());
      assertEquals(20, JeanneAlterRules.avenger("A").attack());
      assertEquals(0.05, JeanneAlterRules.avenger("A").movement(), 1.0E-9);
      assertEquals(40, JeanneAlterRules.avenger("B").health());
      assertEquals(16, JeanneAlterRules.avenger("B").attack());
      var shared = CommonSkillRules.avenger(CommonSkillRules.rank("B"));
      assertEquals(40, shared.health());
      assertEquals(16, shared.attack());
      assertEquals(0.04, shared.speed(), 1.0E-9);
   }

   @Test
   void oblivionAndReplenishmentUseConfirmedAValues() {
      assertEquals(0.50, JeanneAlterRules.oblivionCritBonus("A"), 1.0E-9);
      assertEquals(8.0, JeanneAlterRules.replenishmentPerSecond("A+"), 1.0E-9);
   }

   @Test
   void damageFormulaAppliesEachJeanneModifierOnce() {
      assertEquals(103.140625, JeanneAlterRules.damage(41.0, 5, 0.5, false, true, true, false), 1.0E-9);
      assertEquals(285.69953125, JeanneAlterRules.damage(41.0, 5, 0.5, false, true, true, true), 1.0E-9);
      assertEquals(378.75, JeanneAlterRules.damage(202.0, 5, 0.5, true, false, false, false), 1.0E-9);
   }

   @Test
   void deathLockIsFiveSecondsAndOneUsePerLife() {
      assertEquals(100, JeanneAlterRules.DEATH_LOCK_TICKS);
      assertEquals(5, JeanneAlterRules.MAX_GRUDGE);
   }
}
