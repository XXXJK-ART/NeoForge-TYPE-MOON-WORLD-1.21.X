package net.xxxjk.TYPE_MOON_WORLD.servant.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class ServantNavigationHelperTest {
   @Test
   void meleePressureConvertsHighSpeedOrbitIntoForwardContact() {
      Vec3 adjusted = ServantNavigationHelper.stabilizePressureMotion(
         new Vec3(0.0, 0.22, 1.4), new Vec3(1.0, 0.0, 0.0), 4.0, 0.32);

      assertTrue(adjusted.x >= 0.10, "outside weapon range must retain forward pressure");
      assertTrue(Math.abs(adjusted.z) <= 0.0351, "excessive tangential speed must be removed");
      assertEquals(0.22, adjusted.y, 1.0E-6, "melee steering must not overwrite vertical motion");
   }

   @Test
   void closePressureAllowsFootworkWithoutFastRetreat() {
      Vec3 adjusted = ServantNavigationHelper.stabilizePressureMotion(
         new Vec3(-0.8, 0.0, 0.8), new Vec3(1.0, 0.0, 0.0), 2.2, 0.32);

      assertTrue(adjusted.x >= -0.0301);
      assertTrue(Math.abs(adjusted.z) <= 0.0901);
   }

   @Test
   void normalApproachDoesNotReintroduceClampedRadialSpeedAsLateralMotion() {
      Vec3 adjusted = ServantNavigationHelper.stabilizeApproachMotion(
         new Vec3(-0.8, 0.12, 0.8), new Vec3(1.0, 0.0, 0.0), 8.0, 0.32);

      assertEquals(-0.12, adjusted.x, 1.0E-6);
      assertTrue(Math.abs(adjusted.z) <= 0.1761);
      assertEquals(0.12, adjusted.y, 1.0E-6);
   }

   @Test
   void mutualHighSpeedApproachBrakesBeforeTheServantsPassThroughEachOther() {
      Vec3 adjusted = ServantNavigationHelper.stabilizePressureMotion(
         new Vec3(1.2, 0.0, 0.0), new Vec3(1.0, 0.0, 0.0), 3.8, 0.32,
         new Vec3(-1.0, 0.0, 0.0));

      assertTrue(adjusted.x <= 0.021, "relative closing speed must be capped near contact");
      assertTrue(adjusted.x >= -0.031, "the brake must not turn into a retreat");
   }
}
