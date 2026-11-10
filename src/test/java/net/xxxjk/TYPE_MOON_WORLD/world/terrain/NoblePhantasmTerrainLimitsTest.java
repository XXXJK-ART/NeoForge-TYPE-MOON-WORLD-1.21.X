package net.xxxjk.TYPE_MOON_WORLD.world.terrain;

import static org.junit.jupiter.api.Assertions.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class NoblePhantasmTerrainLimitsTest {
   @Test void blastRadiusIsNonnegativeAndUncapped() {
      assertEquals(60.0, NoblePhantasmTerrainLimits.radius(60.0));
      assertEquals(0.0, NoblePhantasmTerrainLimits.radius(-3.0));
   }

   @Test void endpointVoxelMustRemainInsideConfiguredBlastRadius() {
      Vec3 center = new Vec3(0.5, 0.5, 0.5);
      assertTrue(NoblePhantasmTerrainLimits.contains(center, new BlockPos(17, 0, 0), 18.0));
      assertFalse(NoblePhantasmTerrainLimits.contains(center, new BlockPos(20, 0, 0), 18.0));
   }

   @Test void downwardVoxelsAreExcluded() {
      Vec3 center = new Vec3(0.5, 5.5, 0.5);
      assertTrue(NoblePhantasmTerrainLimits.isAtOrAboveCenter(center, new BlockPos(0, 6, 0)));
      assertFalse(NoblePhantasmTerrainLimits.isAtOrAboveCenter(center, new BlockPos(0, 5, 0)));
   }
}
