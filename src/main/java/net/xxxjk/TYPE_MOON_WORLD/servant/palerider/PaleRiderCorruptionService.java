package net.xxxjk.TYPE_MOON_WORLD.servant.palerider;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ApocalypseHorseEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.PaleRiderEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.RatSwarmEntity;

public final class PaleRiderCorruptionService {
   public static final double CALAMITY_RADIUS = 25.0;
   public static final double CALAMITY_RADIUS_SQR = CALAMITY_RADIUS * CALAMITY_RADIUS;
   private static final String TAG_CORRUPTION_INDEX = "PaleRiderCorruptionIndex";
   private static final String TAG_CORRUPTION_CENTER = "PaleRiderCorruptionCenter";
   private static final int COLUMN_COUNT = 1964;
   private static final int COLUMNS_PER_TICK = 2;
   private static final int VERTICAL_RADIUS = 25;
   private static final double GOLDEN_ANGLE = Math.PI * (3.0 - Math.sqrt(5.0));

   private PaleRiderCorruptionService() {
   }

   public static void begin(LivingEntity rider) {
      rider.getPersistentData().putInt(TAG_CORRUPTION_INDEX, 0);
      rider.getPersistentData().putLong(TAG_CORRUPTION_CENTER, rider.blockPosition().asLong());
   }

   public static void end(LivingEntity rider) {
      rider.getPersistentData().remove(TAG_CORRUPTION_INDEX);
      rider.getPersistentData().remove(TAG_CORRUPTION_CENTER);
   }

   public static void tickDomain(LivingEntity rider, ServerLevel level) {
      BlockPos center = rider.blockPosition();
      BlockPos previousCenter = BlockPos.of(rider.getPersistentData().getLong(TAG_CORRUPTION_CENTER));
      if (previousCenter.distSqr(center) > 16.0) {
         rider.getPersistentData().putInt(TAG_CORRUPTION_INDEX, 0);
         rider.getPersistentData().putLong(TAG_CORRUPTION_CENTER, center.asLong());
      }
      int index = rider.getPersistentData().getInt(TAG_CORRUPTION_INDEX);
      for (int count = 0; count < COLUMNS_PER_TICK && index < COLUMN_COUNT; count++, index++) {
         corruptColumn(level, center, index);
      }
      rider.getPersistentData().putInt(TAG_CORRUPTION_INDEX, index);
   }

   public static void tickFootsteps(LivingEntity entity) {
      if (!(entity.level() instanceof ServerLevel level) || entity.tickCount % 4 != Math.floorMod(entity.getId(), 4)) return;
      LivingEntity owner = ownerOf(level, entity);
      if (owner == null || !isCalamityActive(owner) || entity.distanceToSqr(owner) > CALAMITY_RADIUS_SQR) return;
      BlockPos below = entity.blockPosition().below();
      BlockState state = level.getBlockState(below);
      if (!canCorrupt(level, below, state) || state.isAir() || state.is(Blocks.SOUL_SAND)) return;
      if (!state.getCollisionShape(level, below).isEmpty()) level.setBlock(below, Blocks.SOUL_SAND.defaultBlockState(), Block.UPDATE_ALL);
   }

   private static LivingEntity ownerOf(ServerLevel level, LivingEntity entity) {
      if (entity instanceof ApocalypseHorseEntity horse) return horse.getPaleRiderLivingOwner();
      if (entity instanceof RatSwarmEntity swarm) return swarm.getPaleRiderLivingOwner();
      if (PaleRiderInfectionService.isInfected(entity)) return PaleRiderInfectionService.getOwner(level, entity);
      return null;
   }

   private static boolean isCalamityActive(LivingEntity owner) {
      if (owner instanceof PaleRiderEntity rider) return rider.isCalamityActive();
      return owner instanceof net.minecraft.server.level.ServerPlayer player
         && PaleRiderInfectionService.isPaleRiderCardPlayer(player)
         && player.getPersistentData().getBoolean("PaleRiderCardCalamityActive");
   }

   private static void corruptColumn(ServerLevel level, BlockPos center, int index) {
      double progress = index / (double)Math.max(1, COLUMN_COUNT - 1);
      double radius = CALAMITY_RADIUS * Math.sqrt(progress);
      double angle = index * GOLDEN_ANGLE;
      int x = center.getX() + (int)Math.round(Math.cos(angle) * radius);
      int z = center.getZ() + (int)Math.round(Math.sin(angle) * radius);
      for (int y = center.getY() + VERTICAL_RADIUS; y >= center.getY() - VERTICAL_RADIUS; y--) {
         BlockPos pos = new BlockPos(x, y, z);
         double dx = pos.getX() + 0.5 - (center.getX() + 0.5);
         double dy = pos.getY() + 0.5 - (center.getY() + 0.5);
         double dz = pos.getZ() + 0.5 - (center.getZ() + 0.5);
         if (dx * dx + dy * dy + dz * dz > CALAMITY_RADIUS_SQR) continue;
         corruptBlock(level, pos);
      }
   }

   private static void corruptBlock(ServerLevel level, BlockPos pos) {
      BlockState state = level.getBlockState(pos);
      if (!canCorrupt(level, pos, state)) return;
      if (state.is(Blocks.GRASS_BLOCK)) {
         level.setBlock(pos, Blocks.COARSE_DIRT.defaultBlockState(), Block.UPDATE_ALL);
         return;
      }
      if (isPlant(state)) {
         BlockState replacement = ((pos.asLong() ^ level.getSeed()) & 7L) == 0L
            ? Blocks.WITHER_ROSE.defaultBlockState()
            : Blocks.DEAD_BUSH.defaultBlockState();
         if (replacement.canSurvive(level, pos)) level.setBlock(pos, replacement, Block.UPDATE_ALL);
         else level.removeBlock(pos, false);
         return;
      }
      if (isLivingBlock(state)) level.removeBlock(pos, false);
   }

   private static boolean canCorrupt(ServerLevel level, BlockPos pos, BlockState state) {
      return !state.isAir() && !state.is(Blocks.BEDROCK) && !state.is(BlockTags.PORTALS)
         && !state.hasBlockEntity() && state.getDestroySpeed(level, pos) >= 0.0F;
   }

   private static boolean isPlant(BlockState state) {
      return state.is(BlockTags.FLOWERS) || state.is(BlockTags.SAPLINGS) || state.is(BlockTags.CROPS)
         || state.is(Blocks.SHORT_GRASS) || state.is(Blocks.TALL_GRASS) || state.is(Blocks.FERN) || state.is(Blocks.LARGE_FERN)
         || state.is(Blocks.BROWN_MUSHROOM) || state.is(Blocks.RED_MUSHROOM) || state.is(Blocks.SUGAR_CANE)
         || state.is(Blocks.CACTUS) || state.is(Blocks.BAMBOO) || state.is(Blocks.BAMBOO_SAPLING)
         || state.is(Blocks.SEAGRASS) || state.is(Blocks.TALL_SEAGRASS) || state.is(Blocks.KELP) || state.is(Blocks.KELP_PLANT)
         || state.is(Blocks.VINE) || state.is(BlockTags.CAVE_VINES) || state.is(Blocks.LILY_PAD);
   }

   private static boolean isLivingBlock(BlockState state) {
      return state.is(BlockTags.LEAVES) || state.is(BlockTags.CORALS) || state.is(BlockTags.CORAL_BLOCKS)
         || state.is(BlockTags.CORAL_PLANTS) || state.is(BlockTags.WALL_CORALS) || state.is(BlockTags.NYLIUM)
         || state.is(BlockTags.WART_BLOCKS) || state.is(BlockTags.OVERWORLD_NATURAL_LOGS)
         || state.is(Blocks.MANGROVE_ROOTS) || state.is(Blocks.MUDDY_MANGROVE_ROOTS) || state.is(Blocks.ROOTED_DIRT)
         || state.is(Blocks.MOSS_BLOCK) || state.is(Blocks.MOSS_CARPET)
         || state.is(Blocks.BROWN_MUSHROOM_BLOCK) || state.is(Blocks.RED_MUSHROOM_BLOCK) || state.is(Blocks.MUSHROOM_STEM)
         || state.is(Blocks.AZALEA) || state.is(Blocks.FLOWERING_AZALEA) || state.is(Blocks.HANGING_ROOTS)
         || state.is(Blocks.BIG_DRIPLEAF) || state.is(Blocks.BIG_DRIPLEAF_STEM) || state.is(Blocks.SMALL_DRIPLEAF)
         || state.is(Blocks.SPORE_BLOSSOM) || state.is(Blocks.GLOW_LICHEN);
   }
}
