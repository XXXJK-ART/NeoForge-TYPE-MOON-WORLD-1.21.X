package net.xxxjk.TYPE_MOON_WORLD.gametest;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.xxxjk.TYPE_MOON_WORLD.entity.GaeBulgArmyProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.GaeBulgProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.world.terrain.DeferredTerrainDestruction;

@GameTestHolder("typemoonworld_cu")
@PrefixGameTestTemplate(false)
public final class CuChulainnTerrainGameTests {
   private CuChulainnTerrainGameTests() { }

   @GameTest(template = "ancient_temple", timeoutTicks = 80, batch = "cu_radial_queue")
   public static void sphereClearsCenterOutwardAndPreservesProtectedBlocks(GameTestHelper helper) {
      var level = helper.getLevel();
      BlockPos center = helper.absolutePos(new BlockPos(7, 8, 7));
      for (int x = -4; x <= 4; x++) {
         for (int y = -4; y <= 4; y++) {
            for (int z = -4; z <= 4; z++) {
               level.setBlockAndUpdate(center.offset(x, y, z), Blocks.STONE.defaultBlockState());
            }
         }
      }
      BlockPos bedrock = center.offset(1, 0, 0), chest = center.offset(-1, 0, 0);
      level.setBlockAndUpdate(bedrock, Blocks.BEDROCK.defaultBlockState());
      level.setBlockAndUpdate(chest, Blocks.CHEST.defaultBlockState());
      List<BlockPos> removed = new ArrayList<>();
      DeferredTerrainDestruction.queueRadialSphere(level, Vec3.atCenterOf(center), 4,
         (serverLevel, pos, count) -> removed.add(pos.immutable()));
      helper.runAfterDelay(30, () -> {
         helper.assertTrue(!removed.isEmpty() && removed.getFirst().equals(center), "Blast did not start at its center");
         double previousDistance = -1;
         for (BlockPos pos : removed) {
            double distance = pos.distSqr(center);
            helper.assertTrue(distance >= previousDistance, "Blast removed an outer voxel before an inner voxel");
            previousDistance = distance;
         }
         for (int x = -4; x <= 4; x++) {
            for (int y = -4; y <= 4; y++) {
               for (int z = -4; z <= 4; z++) {
                  BlockPos pos = center.offset(x, y, z);
                  if (pos.equals(bedrock) || pos.equals(chest)) continue;
                  helper.assertTrue(x*x + y*y + z*z <= 16 ? level.getBlockState(pos).isAir()
                     : level.getBlockState(pos).is(Blocks.STONE), "Blast left a hole or exceeded its sphere at " + pos);
               }
            }
         }
         helper.assertTrue(level.getBlockState(bedrock).is(Blocks.BEDROCK), "Blast removed bedrock");
         helper.assertTrue(level.getBlockState(chest).is(Blocks.CHEST) && level.getBlockEntity(chest) != null,
            "Blast removed a block entity");
         helper.succeed();
      });
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 140, batch = "cu_radial_burst")
   public static void actualFlightBurstDoesNotSweepTheRimAheadOfTheCenter(GameTestHelper helper) {
      var level = helper.getLevel();
      BlockPos center = helper.absolutePos(new BlockPos(7, 40, 7));
      var owner = helper.spawn(EntityType.ZOMBIE, new BlockPos(7, 65, 7));
      owner.setNoAi(true); owner.setNoGravity(true);
      var target = helper.spawn(EntityType.ZOMBIE, center.offset(1, 0, 0));
      target.setNoAi(true); target.setNoGravity(true); target.setInvulnerable(true);
      List<BlockPos> inner = List.of(center, center.offset(10, 0, 0), center.offset(0, 10, 0), center.offset(0, 0, 10));
      List<BlockPos> outer = List.of(center.offset(-17, 0, 0), center.offset(0, -17, 0), center.offset(0, 0, -17));
      for (BlockPos pos : inner) level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
      for (BlockPos pos : outer) level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
      BlockPos outside = center.offset(19, 0, 0);
      level.setBlockAndUpdate(outside, Blocks.STONE.defaultBlockState());
      var spear = new GaeBulgArmyProjectileEntity(level, owner);
      spear.setMode(GaeBulgProjectileEntity.Mode.SPLIT);
      spear.setArmyDamage(200);
      spear.setTrackedTarget(target);
      spear.setPos(Vec3.atCenterOf(center));
      spear.setDeltaMovement(Vec3.ZERO);
      spear.tick();
      helper.assertTrue(spear.isRemoved(), "Fixture did not trigger the actual flight burst");
      helper.assertTrue(target.getDeltaMovement().lengthSqr() < 1.0E-6,
         "Flight burst launched a target whose damage was rejected");
      for (int tick = 1; tick <= 100; tick++) helper.runAfterDelay(tick, () -> {
         for (BlockPos rim : outer) {
            if (level.getBlockState(rim).isAir()) {
               for (BlockPos near : inner) helper.assertTrue(level.getBlockState(near).isAir(),
                  "Flight burst swept the rim before clearing its inner sphere");
            }
         }
      });
      helper.runAfterDelay(110, () -> {
         for (BlockPos pos : inner) helper.assertTrue(level.getBlockState(pos).isAir(), "Flight burst missed an inner voxel");
         for (BlockPos pos : outer) helper.assertTrue(level.getBlockState(pos).isAir(), "Flight burst missed its original radius");
         helper.assertTrue(level.getBlockState(outside).is(Blocks.STONE), "Flight burst exceeded its original radius");
         helper.succeed();
      });
   }
}
