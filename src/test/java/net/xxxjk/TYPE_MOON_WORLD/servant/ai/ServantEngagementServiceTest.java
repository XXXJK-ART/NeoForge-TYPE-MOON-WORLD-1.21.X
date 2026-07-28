package net.xxxjk.TYPE_MOON_WORLD.servant.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantClassType;
import org.junit.jupiter.api.Test;

class ServantEngagementServiceTest {
   @Test
   void classAndActionProfileDetermineCombatRole() {
      assertEquals(ServantEngagementService.CombatRole.RANGED,
         ServantEngagementService.role(ServantClassType.ARCHER, 0, 4));
      assertEquals(ServantEngagementService.CombatRole.RANGED,
         ServantEngagementService.role(ServantClassType.CASTER, 1, 3));
      assertEquals(ServantEngagementService.CombatRole.RANGED,
         ServantEngagementService.role(ServantClassType.LANCER, 4, 1));
      assertEquals(ServantEngagementService.CombatRole.MELEE,
         ServantEngagementService.role(ServantClassType.SABER, 1, 3));
   }

   @Test
   void rangedFightersUseDifferentBandsAgainstMeleeAndRangedTargets() {
      var rangedDuel = ServantEngagementService.rangedBand(true, 10.0, 14.0, 18.0);
      var meleePressure = ServantEngagementService.rangedBand(false, 10.0, 14.0, 18.0);

      assertEquals(8.0, rangedDuel.minimum());
      assertEquals(12.0, rangedDuel.preferred());
      assertEquals(18.0, rangedDuel.maximum());
      assertEquals(10.0, meleePressure.minimum());
      assertEquals(16.0, meleePressure.preferred());
      assertEquals(21.0, meleePressure.maximum());
      assertTrue(meleePressure.preferred() > rangedDuel.preferred());
   }
}
