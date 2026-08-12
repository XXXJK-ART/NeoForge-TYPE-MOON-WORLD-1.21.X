package net.xxxjk.TYPE_MOON_WORLD.entity;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class GilgameshEaBoundaryBreakRemovalTest {
   @Test
   void eaBeamNoLongerBreaksOutOfRealityMarbleDimensionsOnRelease() throws IOException {
      String source = Files.readString(Path.of("src/main/java/net/xxxjk/TYPE_MOON_WORLD/entity/GilgameshEaBeamEntity.java"));

      assertFalse(source.contains("collapseContainingBoundary"));
      assertFalse(source.contains("EaWorldBoundaryBreaker"));
      assertFalse(source.contains("DimensionTransition"));
      assertFalse(source.contains("BoundaryBroken"));
   }
}
