package net.xxxjk.TYPE_MOON_WORLD.world.terrain;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;

public final class DeferredTerrainDestruction {
   private static final long SOFT_NANOS_PER_TICK = 2_000_000L;
   private static final int MIN_BLOCKS_PER_TICK = 64;
   private static final int MAX_BLOCKS_PER_TICK = 1400;
   private static final RemovalCallback NO_CALLBACK = (level, pos, removed) -> {
   };

   private DeferredTerrainDestruction() {
   }

   @FunctionalInterface
   public interface BlockPredicate {
      boolean canRemove(ServerLevel level, BlockPos pos, double distanceSqr, double radius, Vec3 center);
   }

   @FunctionalInterface
   public interface RemovalCallback {
      void onRemoved(ServerLevel level, BlockPos pos, int removed);
   }

   public static void queueSphere(ServerLevel level, Vec3 center, int radius, float maxHardness, int targetTicks) {
      if (level == null || radius <= 0) {
         return;
      }
      int volumeEstimate = Math.max(1, (int)Math.ceil((4.0 * Math.PI * radius * radius * radius) / 3.0));
      int initialBudget = clamp(volumeEstimate / Math.max(1, targetTicks), MIN_BLOCKS_PER_TICK, MAX_BLOCKS_PER_TICK);
      new SphereJob(level, center, radius, maxHardness, initialBudget).schedule();
   }

   public static void queueSphere(
      ServerLevel level,
      Vec3 center,
      double radius,
      int targetTicks,
      BlockPredicate predicate,
      RemovalCallback callback
   ) {
      if (level == null || radius <= 0.0 || predicate == null) {
         return;
      }
      int r = (int)Math.ceil(radius);
      int volumeEstimate = Math.max(1, (int)Math.ceil((4.0 * Math.PI * radius * radius * radius) / 3.0));
      int initialBudget = clamp(volumeEstimate / Math.max(1, targetTicks), MIN_BLOCKS_PER_TICK, MAX_BLOCKS_PER_TICK);
      new CustomSphereJob(level, center, radius, r, 0.0, predicate, callback == null ? NO_CALLBACK : callback, initialBudget).schedule();
   }

   public static void queueShell(
      ServerLevel level,
      Vec3 center,
      double currentRadius,
      double previousRadius,
      int targetTicks,
      BlockPredicate predicate,
      RemovalCallback callback
   ) {
      if (level == null || currentRadius <= 0.0 || predicate == null) {
         return;
      }
      double inner = Math.max(0.0, previousRadius);
      int r = (int)Math.ceil(currentRadius);
      double shellVolume = Math.max(1.0, (4.0 * Math.PI * (currentRadius * currentRadius * currentRadius - inner * inner * inner)) / 3.0);
      int initialBudget = clamp((int)Math.ceil(shellVolume / Math.max(1, targetTicks)), MIN_BLOCKS_PER_TICK, MAX_BLOCKS_PER_TICK);
      new CustomSphereJob(level, center, currentRadius, r, inner, predicate, callback == null ? NO_CALLBACK : callback, initialBudget).schedule();
   }

   public static void queueEllipsoid(ServerLevel level, Vec3 center, int radius, int halfHeight, float maxHardness, int targetTicks) {
      if (level == null || radius <= 0 || halfHeight <= 0) {
         return;
      }
      int volumeEstimate = Math.max(1, (int)Math.ceil((4.0 * Math.PI * radius * radius * halfHeight) / 3.0));
      int initialBudget = clamp(volumeEstimate / Math.max(1, targetTicks), MIN_BLOCKS_PER_TICK, MAX_BLOCKS_PER_TICK);
      new EllipsoidJob(level, center, radius, halfHeight, maxHardness, initialBudget).schedule();
   }

   private abstract static class Job {
      protected final ServerLevel level;
      protected final Vec3 center;
      protected final float maxHardness;
      protected int budget;

      protected Job(ServerLevel level, Vec3 center, float maxHardness, int budget) {
         this.level = level;
         this.center = center;
         this.maxHardness = maxHardness;
         this.budget = budget;
      }

      protected void schedule() {
         TYPE_MOON_WORLD.queueServerWork(1, this::run);
      }

      private void run() {
         long start = System.nanoTime();
         int checked = 0;
         while (checked < this.budget && this.advanceOne()) {
            checked++;
            if ((checked & 63) == 0 && System.nanoTime() - start > SOFT_NANOS_PER_TICK) {
               break;
            }
         }

         long elapsed = System.nanoTime() - start;
         if (elapsed > SOFT_NANOS_PER_TICK && this.budget > MIN_BLOCKS_PER_TICK) {
            this.budget = Math.max(MIN_BLOCKS_PER_TICK, this.budget * 3 / 4);
         } else if (elapsed < SOFT_NANOS_PER_TICK / 2 && checked >= this.budget && this.budget < MAX_BLOCKS_PER_TICK) {
            this.budget = Math.min(MAX_BLOCKS_PER_TICK, this.budget + Math.max(16, this.budget / 8));
         }

         if (!this.isDone()) {
            this.schedule();
         }
      }

      protected boolean tryRemove(BlockPos pos) {
         BlockState state = this.level.getBlockState(pos);
         float hardness = state.getDestroySpeed(this.level, pos);
         if (state.isAir()
            || state.is(Blocks.BEDROCK)
            || hardness < 0.0F
            || hardness > this.maxHardness
            || state.getExplosionResistance(this.level, pos, null) >= 1200.0F) {
            return false;
         }
         return this.level.removeBlock(pos, false);
      }

      protected boolean tryRemove(BlockPos pos, BlockPredicate predicate, RemovalCallback callback, double distanceSqr, double radius, int removed) {
         if (!predicate.canRemove(this.level, pos, distanceSqr, radius, this.center)) {
            return false;
         }
         if (this.level.removeBlock(pos, false)) {
            callback.onRemoved(this.level, pos, removed + 1);
            return true;
         }
         return false;
      }

      protected abstract boolean advanceOne();

      protected abstract boolean isDone();
   }

   private static final class SphereJob extends Job {
      private final int radius;
      private final int radiusSqr;
      private int x;
      private int y;
      private int z;
      private boolean done;

      private SphereJob(ServerLevel level, Vec3 center, int radius, float maxHardness, int budget) {
         super(level, center, maxHardness, budget);
         this.radius = radius;
         this.radiusSqr = radius * radius;
         this.x = -radius;
         this.y = -radius;
         this.z = -radius;
      }

      @Override
      protected boolean advanceOne() {
         while (!this.done) {
            int cx = this.x;
            int cy = this.y;
            int cz = this.z;
            this.advanceCursor();
            if (cx * cx + cy * cy + cz * cz <= this.radiusSqr) {
               this.tryRemove(BlockPos.containing(this.center.x + cx, this.center.y + cy, this.center.z + cz));
               return true;
            }
         }
         return false;
      }

      private void advanceCursor() {
         this.z++;
         if (this.z > this.radius) {
            this.z = -this.radius;
            this.y++;
            if (this.y > this.radius) {
               this.y = -this.radius;
               this.x++;
               if (this.x > this.radius) {
                  this.done = true;
               }
            }
         }
      }

      @Override
      protected boolean isDone() {
         return this.done;
      }
   }

   private static final class EllipsoidJob extends Job {
      private final int radius;
      private final int halfHeight;
      private int x;
      private int y;
      private int z;
      private boolean done;

      private EllipsoidJob(ServerLevel level, Vec3 center, int radius, int halfHeight, float maxHardness, int budget) {
         super(level, center, maxHardness, budget);
         this.radius = radius;
         this.halfHeight = halfHeight;
         this.x = -radius;
         this.y = -halfHeight;
         this.z = -radius;
      }

      @Override
      protected boolean advanceOne() {
         while (!this.done) {
            int cx = this.x;
            int cy = this.y;
            int cz = this.z;
            this.advanceCursor();
            double normalized = (cx * cx + cz * cz) / (double)(this.radius * this.radius)
               + (cy * cy) / (double)(this.halfHeight * this.halfHeight);
            if (normalized <= 1.0) {
               this.tryRemove(BlockPos.containing(this.center.x + cx, this.center.y + cy, this.center.z + cz));
               return true;
            }
         }
         return false;
      }

      private void advanceCursor() {
         this.z++;
         if (this.z > this.radius) {
            this.z = -this.radius;
            this.y++;
            if (this.y > this.halfHeight) {
               this.y = -this.halfHeight;
               this.x++;
               if (this.x > this.radius) {
                  this.done = true;
               }
            }
         }
      }

      @Override
      protected boolean isDone() {
         return this.done;
      }
   }

   private static final class CustomSphereJob extends Job {
      private final double radius;
      private final int scanRadius;
      private final double innerRadiusSqr;
      private final double radiusSqr;
      private final BlockPredicate predicate;
      private final RemovalCallback callback;
      private int x;
      private int y;
      private int z;
      private int removed;
      private boolean done;

      private CustomSphereJob(
         ServerLevel level,
         Vec3 center,
         double radius,
         int scanRadius,
         double innerRadius,
         BlockPredicate predicate,
         RemovalCallback callback,
         int budget
      ) {
         super(level, center, Float.MAX_VALUE, budget);
         this.radius = radius;
         this.scanRadius = scanRadius;
         this.innerRadiusSqr = innerRadius * innerRadius;
         this.radiusSqr = radius * radius;
         this.predicate = predicate;
         this.callback = callback;
         this.x = -scanRadius;
         this.y = -scanRadius;
         this.z = -scanRadius;
      }

      @Override
      protected boolean advanceOne() {
         while (!this.done) {
            int cx = this.x;
            int cy = this.y;
            int cz = this.z;
            this.advanceCursor();
            double distSqr = cx * cx + cy * cy + cz * cz;
            if (distSqr <= this.radiusSqr && distSqr > this.innerRadiusSqr) {
               BlockPos pos = BlockPos.containing(this.center.x + cx, this.center.y + cy, this.center.z + cz);
               if (this.tryRemove(pos, this.predicate, this.callback, distSqr, this.radius, this.removed)) {
                  this.removed++;
               }
               return true;
            }
         }
         return false;
      }

      private void advanceCursor() {
         this.z++;
         if (this.z > this.scanRadius) {
            this.z = -this.scanRadius;
            this.y++;
            if (this.y > this.scanRadius) {
               this.y = -this.scanRadius;
               this.x++;
               if (this.x > this.scanRadius) {
                  this.done = true;
               }
            }
         }
      }

      @Override
      protected boolean isDone() {
         return this.done;
      }
   }

   private static int clamp(int value, int min, int max) {
      return Math.max(min, Math.min(max, value));
   }
}
