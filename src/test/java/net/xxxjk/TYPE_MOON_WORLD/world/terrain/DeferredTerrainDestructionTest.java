package net.xxxjk.TYPE_MOON_WORLD.world.terrain;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class DeferredTerrainDestructionTest {
   @Test
   void recentLargerOverlappingImpactSuppressesSmallerDuplicate() {
      assertTrue(DeferredTerrainDestruction.overlappingImpactSupersedes(
         Vec3.ZERO, 6.0, 20.0F, 100L, new Vec3(2.0, 0.0, 0.0), 4.0, 10.0F, 103L));
      assertFalse(DeferredTerrainDestruction.overlappingImpactSupersedes(
         Vec3.ZERO, 4.0, 20.0F, 100L, Vec3.ZERO, 6.0, 10.0F, 103L));
      assertFalse(DeferredTerrainDestruction.overlappingImpactSupersedes(
         Vec3.ZERO, 6.0, 20.0F, 100L, Vec3.ZERO, 4.0, 10.0F, 106L));
      assertFalse(DeferredTerrainDestruction.overlappingImpactSupersedes(
         Vec3.ZERO, 6.0, 20.0F, 100L, new Vec3(4.0, 0.0, 0.0), 4.0, 10.0F, 103L));
   }
}
