package net.xxxjk.TYPE_MOON_WORLD.servant.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantClassType;
import org.junit.jupiter.api.Test;

class ServantPursuitServiceTest {
   @Test
   void meleeClassesStartCatchUpBeforeRangedClasses() {
      assertEquals(36.0, ServantPursuitService.pursuitStartDistance(ServantClassType.SABER));
      assertEquals(56.0, ServantPursuitService.pursuitStartDistance(ServantClassType.CASTER));
      assertEquals(88.0, ServantPursuitService.pursuitStartDistance(ServantClassType.ARCHER));
   }

   @Test
   void pursuitSpeedAndLeadScaleWithLaunchDistance() {
      assertTrue(ServantPursuitService.pursuitSpeed(100.0) > ServantPursuitService.pursuitSpeed(40.0));
      assertEquals(2.0, ServantPursuitService.leadTicks(8.0));
      assertEquals(10.0, ServantPursuitService.leadTicks(160.0));
   }

   @Test
   void cinematicManeuverPredictionIsBounded() {
      assertEquals(2.0, ServantManeuverService.leadTicks(0.0));
      assertTrue(ServantManeuverService.leadTicks(32.0) > 2.0);
      assertEquals(10.0, ServantManeuverService.leadTicks(128.0));
      assertEquals(48.0, ServantManeuverService.MAX_NORMAL_ENGAGEMENT_DISTANCE);
   }
}
