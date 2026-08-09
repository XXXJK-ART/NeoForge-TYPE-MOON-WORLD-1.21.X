package net.xxxjk.TYPE_MOON_WORLD.servant.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.xxxjk.TYPE_MOON_WORLD.world.terrain.TerrainImpactProfile;
import org.junit.jupiter.api.Test;

class ServantCombatMotionServiceTest {
   @Test
   void repeatedControlHasExplicitDiminishingReturns() {
      assertEquals(1.0, ServantCombatMotionService.controlScale(1));
      assertEquals(0.72, ServantCombatMotionService.controlScale(2));
      assertEquals(0.48, ServantCombatMotionService.controlScale(3));
      assertEquals(0.28, ServantCombatMotionService.controlScale(4));
      assertEquals(0.28, ServantCombatMotionService.controlScale(99));
      assertTrue(ServantCombatMotionService.controlScale(1) > ServantCombatMotionService.controlScale(4));
   }

   @Test
   void wallImpactBreaksAWiderVolumeThanGroundImpact() {
      double wall = ServantCombatMotionService.impactRadius(TerrainImpactProfile.Tier.HEAVY, 4.0, true);
      double ground = ServantCombatMotionService.impactRadius(TerrainImpactProfile.Tier.HEAVY, 4.0, false);
      assertEquals(6.4, wall, 1.0E-6);
      assertEquals(4.5, ground, 1.0E-6);
      assertTrue(wall > ground);
   }

   @Test
   void spectacleStrengthAndBreakerBonusesIncreaseImpact() {
      double cRank = ServantCombatSpectacleService.strengthScaleForStep(2, false, false, 1.35);
      double aPlusRank = ServantCombatSpectacleService.strengthScaleForStep(5, false, false, 1.35);
      double heraclesLike = ServantCombatSpectacleService.strengthScaleForStep(5, true, false, 1.35);
      double sunBlessed = ServantCombatSpectacleService.strengthScaleForStep(5, false, true, 1.35);

      assertTrue(aPlusRank > cRank);
      assertTrue(heraclesLike > aPlusRank);
      assertTrue(sunBlessed > aPlusRank);
   }

   @Test
   void enduranceAndKnockbackResistanceReduceLaunchSpectacle() {
      double lowEndurance = ServantCombatSpectacleService.enduranceResistanceScaleForStep(0, 0.0);
      double highEndurance = ServantCombatSpectacleService.enduranceResistanceScaleForStep(5, 0.0);
      double armored = ServantCombatSpectacleService.enduranceResistanceScaleForStep(5, 0.8);

      assertEquals(1.0, lowEndurance, 1.0E-6);
      assertTrue(highEndurance > lowEndurance);
      assertTrue(armored > highEndurance);
   }

   @Test
   void wallTunnelScalesBeyondRoutineGroundWithoutDeepeningGroundCrater() {
      double tunnelLength = ServantCombatSpectacleService.wallTunnelLength(null, null, 2.5);
      int tunnelWidth = ServantCombatSpectacleService.wallTunnelWidth(null, null, 2.5);
      TerrainImpactProfile ground = ServantCombatSpectacleService.routineGroundProfile(null, null, 0.7, false);

      assertTrue(tunnelLength >= 4.0);
      assertTrue(tunnelWidth >= 2);
      assertTrue(ground.radius() <= 4.75);
      assertTrue(ground.limitsSelfFootDepth());
   }
}
