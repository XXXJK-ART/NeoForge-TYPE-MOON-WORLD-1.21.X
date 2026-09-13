package net.xxxjk.TYPE_MOON_WORLD.survival;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

class LakeRitualRulesTest {
   @Test
   void connectedWaterRequiresSixSourceBlocks() {
      Set<BlockPos> five = new HashSet<>();
      for (int x = 0; x < 5; x++) five.add(new BlockPos(x, 0, 0));
      assertFalse(LakeRitualRules.connectedWater(BlockPos.ZERO, five::contains));

      five.add(new BlockPos(5, 0, 0));
      assertTrue(LakeRitualRules.connectedWater(BlockPos.ZERO, five::contains));
   }

   @Test
   void connectedWaterUsesAllAdjacentDirections() {
      Set<BlockPos> water = Set.of(
         new BlockPos(0, 0, 0), new BlockPos(1, 0, 0), new BlockPos(-1, 0, 0),
         new BlockPos(0, 1, 0), new BlockPos(0, -1, 0), new BlockPos(0, 0, 1),
         new BlockPos(0, 0, -1));
      assertTrue(LakeRitualRules.connectedWater(BlockPos.ZERO, water::contains));
   }
}
