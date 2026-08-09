package net.xxxjk.TYPE_MOON_WORLD.combat;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class GilgameshEaBeamResourcesTest {
   private static final Path JAVA = Path.of("src/main/java/net/xxxjk/TYPE_MOON_WORLD/entity");

   @Test
   void beamDamageSweepsBetweenDirectionSamples() throws IOException {
      String source = Files.readString(JAVA.resolve("GilgameshEaBeamEntity.java"));

      assertTrue(source.contains("BEAM_SWEEP_STEP_RADIANS"));
      assertTrue(source.contains("sweepDirections(this.lastDamageDirection, this.direction)"));
      assertTrue(source.contains("this.lastDamageDirection = this.direction"));
      assertTrue(source.contains("for (Vec3 forward : sweepDirections)"));
   }

   @Test
   void beamDamageUsesTargetBoundsForPitchedShots() throws IOException {
      String source = Files.readString(JAVA.resolve("GilgameshEaBeamEntity.java"));

      assertTrue(source.contains("intersectsBeam(target.getBoundingBox(), start, forward)"));
      assertTrue(source.contains("projectedRadius(direction, halfX, halfY, halfZ)"));
      assertTrue(source.contains("projectedRadius(right, halfX, halfY, halfZ)"));
      assertTrue(source.contains("projectedRadius(up, halfX, halfY, halfZ)"));
   }
}
