package net.xxxjk.TYPE_MOON_WORLD.servant.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
