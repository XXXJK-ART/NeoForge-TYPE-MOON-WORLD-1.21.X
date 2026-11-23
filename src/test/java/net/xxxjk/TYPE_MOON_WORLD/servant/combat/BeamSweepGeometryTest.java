package net.xxxjk.TYPE_MOON_WORLD.servant.combat;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BeamSweepGeometryTest {
   @Test void exactHalfTurnRetainsIntermediateSweptDirections() {
      var directions = BeamSweepGeometry.directions(new Vec3(0, 0, 1), new Vec3(0, 0, -1));
      assertTrue(directions.size() >= 90);
      assertTrue(directions.stream().anyMatch(v -> Math.abs(v.x) > .99));
      for (var direction : directions) assertEquals(1, direction.length(), 1E-8);
   }
   @Test void verticalCrossSectionsIncludeWholeTargetBounds() {
      AABB target = new AABB(-.3, 75, -.3, .3, 77, .3);
      assertTrue(BeamSweepGeometry.intersects(target, Vec3.ZERO, new Vec3(0, 1, 0), 150, 12, 7));
      assertTrue(BeamSweepGeometry.intersects(target.move(0, -150, 0), Vec3.ZERO, new Vec3(0, -1, 0), 150, 12, 7));
      assertFalse(BeamSweepGeometry.intersects(target.move(30, 0, 0), Vec3.ZERO, new Vec3(0, 1, 0), 150, 12, 7));
   }
   @Test void diagonalAndHorizontalSamplesKeepTheSameRangeAndHeight() {
      Vec3 direction = new Vec3(1, 1, 1).normalize();
      Vec3 center = direction.scale(100);
      assertTrue(BeamSweepGeometry.intersects(new AABB(center, center).inflate(.3), Vec3.ZERO, direction, 150, 12, 7));
      assertFalse(BeamSweepGeometry.intersects(new AABB(direction.scale(160), direction.scale(161)), Vec3.ZERO, direction, 150, 12, 7));
   }
}
