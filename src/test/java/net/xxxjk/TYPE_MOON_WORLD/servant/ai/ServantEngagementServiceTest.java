package net.xxxjk.TYPE_MOON_WORLD.servant.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
   void genericMagicBoltsAreLimitedToMagicServants() {
      assertTrue(ServantDuelPacing.usesGenericMagicBoltBasic("medea"));
      assertTrue(ServantDuelPacing.usesGenericMagicBoltBasic("paracelsus"));
      assertFalse(ServantDuelPacing.usesGenericMagicBoltBasic("jeanne_alter"));
      assertFalse(ServantDuelPacing.usesGenericMagicBoltBasic("baobhan_sith"));
   }

   @Test
   void baobhanSithUsesAFasterBasicArrowCadence() {
      assertEquals(8, ServantDuelPacing.rangedBasicRecoveryTicks("baobhan_sith"));
      assertEquals(16, ServantDuelPacing.rangedBasicRecoveryTicks("arash"));
   }

   @Test
   void jeanneAlterUsesMeleeRoleDespiteHerProjectileSkill() {
      assertEquals(ServantEngagementService.CombatRole.MELEE,
         ServantEngagementService.role("jeanne_alter", ServantClassType.AVENGER, 1, 0));
      assertEquals(ServantEngagementService.CombatRole.RANGED,
         ServantEngagementService.role("baobhan_sith", ServantClassType.ARCHER, 4, 2));
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

   @Test
   void rangedFighterHoldsAStableFiringWindow() {
      var band = new ServantEngagementService.RangeBand(8.0, 14.0, 20.0);

      assertTrue(ServantEngagementService.shouldHoldFiringPosition(14.0, band, true, false));
      assertFalse(ServantEngagementService.shouldHoldFiringPosition(7.0, band, true, false));
      assertFalse(ServantEngagementService.shouldHoldFiringPosition(14.0, band, false, false));
      assertFalse(ServantEngagementService.shouldHoldFiringPosition(14.0, band, true, true));
   }
}
