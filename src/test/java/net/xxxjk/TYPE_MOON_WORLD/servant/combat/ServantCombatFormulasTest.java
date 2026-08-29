package net.xxxjk.TYPE_MOON_WORLD.servant.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantParams;
import org.junit.jupiter.api.Test;

class ServantCombatFormulasTest {
   @Test
   void sprintRampStartsAtEThenReachesDAndMaxSpeed() {
      ServantParams fast = ServantParams.of("C", false, "C", false, "B", false, "C", false, "C", false);

      assertEquals(ServantCombatFormulas.SERVANT_SPEED_E,
         ServantCombatFormulas.rampedMovementSpeed(fast, 0, fast.movementSpeed()), 1.0E-9);
      assertEquals(ServantCombatFormulas.SERVANT_SPEED_D,
         ServantCombatFormulas.rampedMovementSpeed(fast, 20, fast.movementSpeed()), 1.0E-9);
      assertEquals(fast.movementSpeed(),
         ServantCombatFormulas.rampedMovementSpeed(fast, 40, fast.movementSpeed()), 1.0E-9);
   }

   @Test
   void eRankSpeedNeverClimbsPastE() {
      ServantParams slow = ServantParams.of("E", false, "E", false, "E", false, "E", false, "E", false);

      assertEquals(ServantCombatFormulas.SERVANT_SPEED_E,
         ServantCombatFormulas.rampedMovementSpeed(slow, 40, 0.5), 1.0E-9);
   }

   @Test
   void defensiveResourcesFollowTheRequestedParameterRanks() {
      ServantParams e = ServantParams.of("E", false, "E", false, "E", false, "E", false, "E", false);
      ServantParams c = ServantParams.of("C", false, "C", false, "C", false, "C", false, "C", false);
      ServantParams a = ServantParams.of("A", false, "A", false, "A", false, "A", false, "A", false);

      assertTrue(ServantCombatFormulas.baseDodgeChance(e, false)
         < ServantCombatFormulas.baseDodgeChance(c, false));
      assertTrue(ServantCombatFormulas.baseDodgeChance(c, false)
         < ServantCombatFormulas.baseDodgeChance(a, false));
      assertTrue(ServantCombatFormulas.blockReduction(e) < ServantCombatFormulas.blockReduction(a));
      assertTrue(ServantCombatFormulas.poiseMax(e) < ServantCombatFormulas.poiseMax(a));
      assertEquals(ServantCombatFormulas.staminaRegenPerSecond(e), ServantCombatFormulas.staminaRegenPerSecond(a));
      assertEquals(ServantCombatFormulas.poiseRegenPerSecond(e), ServantCombatFormulas.poiseRegenPerSecond(a));
   }

   @Test
   void plusRanksUseTwoTimesAndAPlusPlusUsesThreeTimes() {
      ServantParams plus = ServantParams.of("A", true, "A", true, "A", true, "A", true, "A", true);
      ServantParams triple = ServantParams.of("A++", false, "A++", false, "A++", false, "A++", false, "A++", false);

      assertEquals(1000.0, plus.maxHealth(), 1.0E-9);
      assertEquals(1500.0, triple.maxHealth(), 1.0E-9);
      assertTrue(ServantCombatFormulas.baseDodgeChance(plus, false)
         < ServantCombatFormulas.baseDodgeChance(triple, false));
      assertTrue(ServantCombatFormulas.poiseMax(plus) < ServantCombatFormulas.poiseMax(triple));
   }
}
