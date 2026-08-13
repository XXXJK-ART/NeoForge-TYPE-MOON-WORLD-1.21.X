package net.xxxjk.TYPE_MOON_WORLD.world.terrain;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.xxxjk.TYPE_MOON_WORLD.Config;
import net.xxxjk.TYPE_MOON_WORLD.performance.PerformanceMonitor;
import net.xxxjk.TYPE_MOON_WORLD.util.ModTags;

/** Shared, round-robin terrain queue. Damage logic never waits for terrain work. */
@EventBusSubscriber(modid = "typemoonworld")
public final class DeferredTerrainDestruction {
   private static final Map<ResourceKey<Level>, ArrayDeque<Job>> JOBS = new HashMap<>();
   private static final Map<ResourceKey<Level>, TickMetrics> LAST_METRICS = new HashMap<>();
   private DeferredTerrainDestruction() { }

   @FunctionalInterface public interface BlockPredicate { boolean canRemove(ServerLevel level, BlockPos pos, double distanceSqr, double radius, Vec3 center); }
   @FunctionalInterface public interface RemovalCallback { void onRemoved(ServerLevel level, BlockPos pos, int removed); }
   @FunctionalInterface public interface DetailedRemovalCallback { void onRemoved(ServerLevel level, BlockPos pos, BlockState previousState, int removed); }

   public static void queueSphere(ServerLevel level, Vec3 center, int radius, float maxHardness, int targetTicks) {
      if (radius > 0) add(level, new SphereJob(level, center, radius, radius, maxHardness, false, null, null).schedule(targetTicks));
   }
   public static void queueSphere(ServerLevel level, Vec3 center, double radius, int targetTicks, BlockPredicate predicate, RemovalCallback callback) {
      if (radius > 0 && predicate != null) add(level, new SphereJob(level, center, (int)Math.ceil(radius), radius, Float.MAX_VALUE, false, predicate, callback).schedule(targetTicks));
   }
   public static void queueShell(ServerLevel level, Vec3 center, double currentRadius, double previousRadius, int targetTicks, BlockPredicate predicate, RemovalCallback callback) {
      if (currentRadius > 0 && predicate != null) add(level, new SphereJob(level, center, (int)Math.ceil(currentRadius), currentRadius, Float.MAX_VALUE, true, predicate, callback).inner(previousRadius).schedule(targetTicks));
   }
   public static void queueEllipsoid(ServerLevel level, Vec3 center, int radius, int halfHeight, float maxHardness, int targetTicks) {
      if (radius > 0 && halfHeight > 0) add(level, new EllipsoidJob(level, center, radius, halfHeight, maxHardness).schedule(targetTicks));
   }

   public static void queueHemisphere(ServerLevel level, Vec3 center, double radius, boolean lower,
                                      float maxHardness, int targetTicks, BlockPredicate predicate,
                                      DetailedRemovalCallback callback, Runnable completion) {
      if (level == null || radius <= 0.0) return;
      add(level, new HemisphereJob(level, center, radius, lower, false, maxHardness, predicate, callback, completion).schedule(targetTicks));
   }

   public static void queueSphereDetailed(ServerLevel level, Vec3 center, double radius, float maxHardness,
                                          int targetTicks, BlockPredicate predicate,
                                          DetailedRemovalCallback callback, Runnable completion) {
      if (level == null || radius <= 0.0) return;
      add(level, new HemisphereJob(level, center, radius, false, true, maxHardness, predicate, callback, completion).schedule(targetTicks));
   }
   public static void queueDiagonalCut(ServerLevel level, Vec3 origin, Vec3 direction, double length, double width, double height, Runnable completion) {
      queueDiagonalCut(level, origin, direction, length, width, height, false, completion);
   }
   public static void queueDiagonalCut(ServerLevel level, Vec3 origin, Vec3 direction, double length, double width, double height, boolean mirrored, Runnable completion) {
      add(level, new DiagonalJob(level, origin, direction, length, width, height, mirrored, false, completion));
   }
   public static void queueDiagonalBurnShell(ServerLevel level, Vec3 origin, Vec3 direction, double length, double width, double height, double shell) {
      queueDiagonalBurnShell(level, origin, direction, length, width, height, false, shell);
   }
   public static void queueDiagonalBurnShell(ServerLevel level, Vec3 origin, Vec3 direction, double length, double width, double height, boolean mirrored, double shell) {
      add(level, new DiagonalJob(level, origin, direction, length, width, height, mirrored, true, null).shell(shell));
   }
   public static void queueCrossBurnShell(ServerLevel level, Vec3 origin, Vec3 direction, double length, double width, double height, boolean mirrored, double shell) {
      add(level, new DiagonalJob(level, origin, direction, length, width, height, mirrored, true, null).shell(shell).excludeCrossCore());
   }
   public static AdvancingDiagonalCut queueAdvancingDiagonalCut(ServerLevel level, Vec3 origin, Vec3 direction,
                                                                 double length, double width, double height,
                                                                 boolean mirrored, Runnable completion) {
      AdvancingDiagonalJob job = new AdvancingDiagonalJob(level, origin, direction, length, width, height, mirrored, completion);
      add(level, job);
      return new AdvancingDiagonalCut(job);
   }
   /** Queues an exact, distance-gated rectangular corridor. Cross-section values are block counts. */
   public static AdvancingCorridor queueAdvancingCorridor(ServerLevel level, Vec3 origin, Vec3 direction,
                                                           double length, int width, int height, Runnable completion) {
      CorridorJob job = new CorridorJob(level, origin, direction, length, width, height, completion);
      add(level, job);
      return new AdvancingCorridor(job);
   }

   /** Queues a distance-gated circular tunnel plus a cracked and depressed ground scar around it. */
   public static AdvancingCylinder queueAdvancingCylinder(ServerLevel level, Vec3 origin, Vec3 direction,
                                                           double length, int radius, int scarRadius,
                                                           Runnable completion) {
      CylinderJob job = new CylinderJob(level, origin, direction, length, radius, scarRadius, completion);
      add(level, job);
      return new AdvancingCylinder(job);
   }

   /** Queues a distance-gated vertical rift that clears the tunnel core upward to the open sky. */
   public static AdvancingSkyRift queueAdvancingSkyRift(ServerLevel level, Vec3 origin, Vec3 direction,
                                                        double length, int radius, Runnable completion) {
      SkyRiftJob job = new SkyRiftJob(level, origin, direction, length, radius, completion);
      add(level, job);
      return new AdvancingSkyRift(job);
   }

   /** Queues a sphere whose available work expands only when the caller advances its radius. */
   public static ExpandingSphere queueExpandingSphere(ServerLevel level, Vec3 center, int radius, Runnable completion) {
      ExpandingSphereJob job = new ExpandingSphereJob(level, center, radius, completion);
      add(level, job);
      return new ExpandingSphere(job);
   }
   /** Queues the exact swept volume between two consecutive blade poses. */
   public static void queueSweptBlade(ServerLevel level, Vec3 previousCenter, Vec3 currentCenter,
                                      Vec3 previousAxis, Vec3 currentAxis, Vec3 faceNormal,
                                      double bladeLength, double radius,
                                      boolean burnTrace, BooleanSupplier active) {
      if (level == null || previousCenter == null || currentCenter == null || previousAxis == null || currentAxis == null) return;
      add(level, new SweptBladeJob(level, previousCenter, currentCenter, previousAxis, currentAxis,
         faceNormal, bladeLength, radius, burnTrace, active));
   }

   /** Queues complete, near-to-far voxel clearing for one or more connected slash traces. */
   public static void queueBladeTrace(ServerLevel level, List<TraceSegment> segments, double radius,
                                      boolean burnTrace, BooleanSupplier active) {
      if (level == null || segments == null || segments.isEmpty()) return;
      add(level, new BladeTraceJob(level, segments, radius, burnTrace, active));
   }

   public record TraceSegment(Vec3 start, Vec3 end) { }
   public record TickMetrics(int checkedBlocks, long elapsedNanos, int queuedJobs) { }

   public static TickMetrics lastMetrics(ServerLevel level) {
      return level == null ? new TickMetrics(0, 0L, 0) : LAST_METRICS.getOrDefault(level.dimension(), new TickMetrics(0, 0L, 0));
   }
   public static void queueDirectionalCut(ServerLevel level, Vec3 origin, Vec3 direction, double length, double width, double height, boolean funnel) {
      add(level, new DirectionalJob(level, origin, direction, length, width, height, funnel, 80.0F));
   }
   public static void queueDirectionalCut(ServerLevel level, Vec3 origin, Vec3 direction, double length,
                                          double width, double height, boolean funnel, float maxHardness) {
      add(level, new DirectionalJob(level, origin, direction, length, width, height, funnel, maxHardness));
   }
   public static void queueUpperHemisphere(ServerLevel level, Vec3 center, double radius, double minimumYExclusive, float maxHardness) {
      if (level != null && radius > 0) add(level, new UpperHemisphereJob(level, center, radius, minimumYExclusive, maxHardness));
   }

   private static void add(ServerLevel level, Job job) {
      if (level == null || job == null) return;
      ArrayDeque<Job> queue = JOBS.computeIfAbsent(level.dimension(), ignored -> new ArrayDeque<>());
      for (Job existing : List.copyOf(queue)) {
         if (existing.supersedes(job)) return;
         if (job.supersedes(existing)) queue.remove(existing);
      }
      if (queue.size() >= Math.max(8, Config.maxQueuedTerrainJobs)) return;
      queue.add(job);
   }

   public static int maximumQueuedJobsPerDimension() {
      return Math.max(8, Config.maxQueuedTerrainJobs);
   }

   public static int totalQueuedJobs() {
      int total = 0;
      for (ArrayDeque<Job> queue : JOBS.values()) total += queue.size();
      return total;
   }

   static boolean overlappingImpactSupersedes(Vec3 center, double radius, float hardness, long queuedAt,
                                               Vec3 otherCenter, double otherRadius, float otherHardness,
                                               long otherQueuedAt) {
      if (Math.abs(queuedAt - otherQueuedAt) > 4L) return false;
      double overlap = Math.min(radius, otherRadius) * 0.75;
      return center.distanceToSqr(otherCenter) <= overlap * overlap
         && radius >= otherRadius && hardness >= otherHardness;
   }

   @SubscribeEvent public static void tick(LevelTickEvent.Post event) {
      if (!(event.getLevel() instanceof ServerLevel level)) return;
      ArrayDeque<Job> queue = JOBS.get(level.dimension()); if (queue == null || queue.isEmpty()) return;
      long started = System.nanoTime(); int checked = 0;
      int maxChecks = Math.max(1000, Config.terrainChecksPerTick);
      long normalBudgetNanos = Math.max(1_000_000L, Config.terrainBudgetMicros * 1000L);
      long softBudgetNanos = Math.max(500_000L,
         (long) (normalBudgetNanos * PerformanceMonitor.backgroundBudgetScale()));
      int idleJobs = 0;
      while (!queue.isEmpty() && checked < maxChecks && System.nanoTime() - started < softBudgetNanos) {
         Job job = queue.pollFirst(); int slice = 0;
         while (!job.done && job.ready() && slice < job.sliceLimit && checked < maxChecks
            && System.nanoTime() - started < softBudgetNanos) { job.advance(); slice++; checked++; }
         if (!job.done) queue.addLast(job); else job.finish();
         if (slice == 0) {
            idleJobs++;
            if (idleJobs >= queue.size()) break;
         } else {
            idleJobs = 0;
         }
      }
      LAST_METRICS.put(level.dimension(), new TickMetrics(checked, System.nanoTime() - started, queue.size()));
      if (queue.isEmpty()) JOBS.remove(level.dimension());
   }
   @SubscribeEvent public static void unload(LevelEvent.Unload event) { if (event.getLevel() instanceof Level level && !level.isClientSide()) { JOBS.remove(level.dimension()); LAST_METRICS.remove(level.dimension()); } }

   private abstract static class Job {
      final ServerLevel level; final long queuedAt; boolean done; Runnable completion; int sliceLimit = 128;
      Job(ServerLevel level) { this.level = level; this.queuedAt = level.getGameTime(); }
      abstract void advance();
      boolean ready() { return true; }
      boolean supersedes(Job other) { return false; }
      void finish() { if (completion != null) completion.run(); }
      Job schedule(int targetTicks, long estimatedChecks) {
         if (targetTicks > 0) sliceLimit = Mth.clamp((int)Math.ceil(estimatedChecks / (double)targetTicks), 16, 512);
         return this;
      }
      boolean valid(BlockPos pos, float maxHardness) {
         if (!Config.terrainDestructionEnabled || !level.getWorldBorder().isWithinBounds(pos)
            || !level.hasChunkAt(pos) || level.getBlockEntity(pos) != null) return false;
         BlockState state = level.getBlockState(pos); float hardness = state.getDestroySpeed(level, pos);
         return !state.isAir() && !state.is(ModTags.Blocks.TERRAIN_IMMUNE) && !isProtected(state) && hardness >= 0 && hardness <= maxHardness
            && state.getExplosionResistance(level, pos, null) < 1200;
      }

      private static boolean isProtected(BlockState state) {
         return state.is(Blocks.BEDROCK) || state.is(Blocks.NETHER_PORTAL) || state.is(Blocks.END_PORTAL)
            || state.is(Blocks.END_PORTAL_FRAME) || state.is(Blocks.COMMAND_BLOCK)
            || state.is(Blocks.CHAIN_COMMAND_BLOCK) || state.is(Blocks.REPEATING_COMMAND_BLOCK)
            || state.is(Blocks.STRUCTURE_BLOCK) || state.is(Blocks.JIGSAW);
      }
   }
   private static final class SphereJob extends Job {
      final Vec3 center; final int scan; final double radius, radiusSqr; final float hardness; final boolean shellOnly; final BlockPredicate predicate; final RemovalCallback callback;
      double innerSqr; int x, y, z, removed;
      SphereJob(ServerLevel level, Vec3 center, int scan, double radius, float hardness, boolean shellOnly, BlockPredicate predicate, RemovalCallback callback) { super(level); this.center=center; this.scan=scan; this.radius=radius; this.radiusSqr=radius*radius; this.hardness=hardness; this.shellOnly=shellOnly; this.predicate=predicate; this.callback=callback; x=y=z=-scan; }
      SphereJob inner(double value) { innerSqr=Math.max(0,value*value); return this; }
      SphereJob schedule(int targetTicks) { super.schedule(targetTicks, (long)(scan * 2 + 1) * (scan * 2 + 1) * (scan * 2 + 1)); return this; }
      void advance() { int cx=x,cy=y,cz=z; cursor(); double d=cx*cx+cy*cy+cz*cz; if(d>radiusSqr || d<=innerSqr)return; BlockPos p=BlockPos.containing(center.x+cx,center.y+cy,center.z+cz); if(valid(p,hardness) && (predicate==null || predicate.canRemove(level,p,d,radius,center))){ if(level.removeBlock(p,false)){removed++; if(callback!=null)callback.onRemoved(level,p,removed);} } }
      void cursor(){if(++z>scan){z=-scan;if(++y>scan){y=-scan;if(++x>scan)done=true;}}}
   }
   private static final class EllipsoidJob extends Job {
      final Vec3 center; final int radius,height; final float hardness; int x,y,z;
      EllipsoidJob(ServerLevel level,Vec3 center,int radius,int height,float hardness){super(level);this.center=center;this.radius=radius;this.height=height;this.hardness=hardness;x=z=-radius;y=-height;}
      EllipsoidJob schedule(int targetTicks){super.schedule(targetTicks,(long)(radius*2+1)*(radius*2+1)*(height*2+1));return this;}
      void advance(){int cx=x,cy=y,cz=z;cursor();double n=(cx*cx+cz*cz)/(double)(radius*radius)+(cy*cy)/(double)(height*height);if(n<=1){BlockPos p=BlockPos.containing(center.x+cx,center.y+cy,center.z+cz);if(valid(p,hardness))level.removeBlock(p,false);}}
      void cursor(){if(++z>radius){z=-radius;if(++y>height){y=-height;if(++x>radius)done=true;}}}
   }

   private static final class HemisphereJob extends Job {
      final Vec3 center;
      final int scan;
      final double radius, radiusSqr;
      final boolean lower;
      final boolean full;
      final float hardness;
      final BlockPredicate predicate;
      final DetailedRemovalCallback callback;
      int x, y, z, removed;

      HemisphereJob(ServerLevel level, Vec3 center, double radius, boolean lower, boolean full, float hardness,
                    BlockPredicate predicate, DetailedRemovalCallback callback, Runnable completion) {
         super(level);
         this.center = center;
         this.scan = (int)Math.ceil(radius);
         this.radius = radius;
         this.radiusSqr = radius * radius;
         this.lower = lower;
         this.full = full;
         this.hardness = hardness;
         this.predicate = predicate;
         this.callback = callback;
         this.completion = completion;
         this.x = this.z = -scan;
         this.y = lower || full ? -scan : 0;
      }

      HemisphereJob schedule(int targetTicks) {
         int height = full ? scan * 2 + 1 : scan + 1;
         super.schedule(targetTicks, (long)(scan * 2 + 1) * (scan * 2 + 1) * height);
         return this;
      }

      @Override boolean supersedes(Job other) {
         if (!(other instanceof HemisphereJob candidate) || this.full != candidate.full
            || this.lower != candidate.lower) return false;
         return overlappingImpactSupersedes(this.center, this.radius, this.hardness, this.queuedAt,
            candidate.center, candidate.radius, candidate.hardness, candidate.queuedAt);
      }

      @Override void advance() {
         int cx = x, cy = y, cz = z;
         cursor();
         double distanceSqr = cx * cx + cy * cy + cz * cz;
         if (distanceSqr > radiusSqr) return;
         BlockPos pos = BlockPos.containing(center.x + cx, center.y + cy, center.z + cz);
         if (!valid(pos, hardness) || predicate != null && !predicate.canRemove(level, pos, distanceSqr, radius, center)) return;
         BlockState previous = level.getBlockState(pos);
         if (level.removeBlock(pos, false)) {
            removed++;
            if (callback != null) callback.onRemoved(level, pos, previous, removed);
         }
      }

      private void cursor() {
         if (++z > scan) {
            z = -scan;
            int maxY = lower && !full ? 0 : scan;
            if (++y > maxY) {
               y = lower || full ? -scan : 0;
               if (++x > scan) done = true;
            }
         }
      }
   }
   private static final class DiagonalJob extends Job {
      final Vec3 origin,forward,side; final double length,width,height,sign; final boolean burn; double shell; boolean excludeCrossCore; int along,normal,vertical;
      DiagonalJob(ServerLevel level,Vec3 origin,Vec3 direction,double length,double width,double height,boolean mirrored,boolean burn,Runnable completion){super(level);this.origin=origin;Vec3 flat=new Vec3(direction.x,0,direction.z);this.forward=flat.lengthSqr()<1e-6?new Vec3(0,0,1):flat.normalize();this.side=new Vec3(-forward.z,0,forward.x);this.length=length;this.width=width;this.height=height;this.sign=mirrored?-1.0:1.0;this.burn=burn;this.completion=completion;resetCursor();}
      DiagonalJob shell(double shell){this.shell=shell;resetCursor();return this;}
      DiagonalJob excludeCrossCore(){this.excludeCrossCore=true;return this;}
      void resetCursor(){along=burn?(int)Math.floor(-shell*2.0):0;normal=(int)-Math.ceil(width/2+shell);vertical=(int)-Math.ceil(height/2+shell);}
      void advance(){double distance=along*.5,n=normal,y=vertical;cursor();boolean inCore=distance>=0&&distance<=length&&Math.abs(n)<=width/2&&Math.abs(y)<=height/2;if(burn&&inCore)return;if(!burn&&!inCore)return;double sideOffset=sign*y+n*Math.sqrt(2.0);Vec3 point=origin.add(forward.scale(distance)).add(side.scale(sideOffset)).add(0,y,0);if(burn&&excludeCrossCore&&insideEitherCore(point,distance))return;BlockPos pos=BlockPos.containing(point);if(!valid(pos,Float.MAX_VALUE))return;if(burn)level.setBlock(pos,level.random.nextFloat()<.75F?Blocks.NETHERRACK.defaultBlockState():Blocks.MAGMA_BLOCK.defaultBlockState(),3);else level.removeBlock(pos,false);}
      boolean insideEitherCore(Vec3 point,double distance){if(distance<0||distance>length)return false;Vec3 rel=point.subtract(origin);double sideDistance=rel.dot(side),verticalDistance=rel.y;return Math.abs(verticalDistance)<=height/2&&Math.min(Math.abs(sideDistance-verticalDistance),Math.abs(sideDistance+verticalDistance))/Math.sqrt(2.0)<=width/2;}
      void cursor(){int maxNormal=(int)Math.ceil(width/2+shell),maxY=(int)Math.ceil(height/2+shell),maxAlong=(int)Math.ceil((length+shell)*2.0);if(++vertical>maxY){vertical=-maxY;if(++normal>maxNormal){normal=-maxNormal;if(++along>maxAlong)done=true;}}}
   }
   public static final class AdvancingDiagonalCut {
      private final AdvancingDiagonalJob job;
      private AdvancingDiagonalCut(AdvancingDiagonalJob job){this.job=job;}
      public void advanceTo(double distance){job.advanceTo(distance);}
      public void seal(){job.seal();}
      public boolean isComplete(){return job.done;}
   }
   private static final class AdvancingDiagonalJob extends Job {
      final Vec3 origin,forward,side; final double length,width,height,sign; int along,normal,vertical; double targetDistance; boolean sealed;
      AdvancingDiagonalJob(ServerLevel level,Vec3 origin,Vec3 direction,double length,double width,double height,boolean mirrored,Runnable completion){super(level);this.origin=origin;Vec3 flat=new Vec3(direction.x,0,direction.z);this.forward=flat.lengthSqr()<1e-6?new Vec3(0,0,1):flat.normalize();this.side=new Vec3(-forward.z,0,forward.x);this.length=length;this.width=width;this.height=height;this.sign=mirrored?-1.0:1.0;this.completion=completion;normal=(int)-Math.ceil(width/2);vertical=(int)-Math.ceil(height/2);}
      void advanceTo(double distance){targetDistance=Math.max(targetDistance,Math.min(length,distance));}
      void seal(){sealed=true;targetDistance=length;if(along>Math.ceil(length))done=true;}
      @Override boolean ready(){return !done&&along<=Math.floor(targetDistance+1.0E-6);}
      @Override void advance(){double distance=along,n=normal,y=vertical;cursor();double sideOffset=sign*y+n*Math.sqrt(2.0);if(sign<0&&Math.abs(sideOffset-y)/Math.sqrt(2.0)<=width/2)return;BlockPos pos=BlockPos.containing(origin.add(forward.scale(distance)).add(side.scale(sideOffset)).add(0,y,0));if(valid(pos,Float.MAX_VALUE))level.removeBlock(pos,false);}
      void cursor(){int maxNormal=(int)Math.ceil(width/2),maxY=(int)Math.ceil(height/2);if(++vertical>maxY){vertical=-maxY;if(++normal>maxNormal){normal=-maxNormal;if(++along>Math.ceil(length)&&sealed)done=true;}}}
   }

   public static final class AdvancingCorridor {
      private final CorridorJob job;
      private AdvancingCorridor(CorridorJob job) { this.job = job; }
      public void advanceTo(double distance) { job.advanceTo(distance); }
      public void seal() { job.seal(); }
      public boolean isComplete() { return job.done; }
   }

   private static final class CorridorJob extends Job {
      final Vec3 origin, forward, side;
      final double length;
      final int width, height, minSide, minY;
      int along, sideOffset, yOffset;
      double targetDistance;
      boolean sealed;

      CorridorJob(ServerLevel level, Vec3 origin, Vec3 direction, double length, int width, int height, Runnable completion) {
         super(level);
         this.origin = origin;
         Vec3 flat = new Vec3(direction.x, 0.0, direction.z);
         this.forward = flat.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : flat.normalize();
         this.side = new Vec3(-this.forward.z, 0.0, this.forward.x);
         this.length = Math.max(0.0, length);
         this.width = Math.max(1, width);
         this.height = Math.max(1, height);
         this.minSide = -(this.width / 2);
         this.minY = -(this.height / 2);
         this.sideOffset = this.minSide;
         this.yOffset = this.minY;
         this.completion = completion;
      }

      void advanceTo(double distance) { this.targetDistance = Math.max(this.targetDistance, Math.min(this.length, distance)); }
      void seal() { this.sealed = true; this.targetDistance = this.length; }
      @Override boolean ready() { return !done && along <= Math.floor(targetDistance + 1.0E-6); }
      @Override void advance() {
         int currentAlong = along, currentSide = sideOffset, currentY = yOffset;
         cursor();
         Vec3 point = origin.add(forward.scale(currentAlong)).add(side.scale(currentSide)).add(0.0, currentY, 0.0);
         BlockPos pos = BlockPos.containing(point);
         if (valid(pos, Float.MAX_VALUE)) level.removeBlock(pos, false);
      }
      void cursor() {
         if (++yOffset >= minY + height) {
            yOffset = minY;
            if (++sideOffset >= minSide + width) {
               sideOffset = minSide;
               if (++along > Math.ceil(length) && sealed) done = true;
            }
         }
      }
   }

   public static final class AdvancingCylinder {
      private final CylinderJob job;
      private AdvancingCylinder(CylinderJob job) { this.job = job; }
      public void advanceTo(double distance) { job.advanceTo(distance); }
      public void seal() { job.seal(); }
      public void sealAt(double distance) { job.sealAt(distance); }
      public boolean isComplete() { return job.done; }
   }

   private static final class CylinderJob extends Job {
      private static final int CORE_PHASE = 0;
      private static final int SCAR_PHASE = 1;
      final Vec3 origin, forward, side;
      final double length;
      final int radius, radiusSqr, scarRadius;
      int along, phase, sideOffset, verticalOffset, scarDepth;
      double targetDistance;
      double sealedDistance;
      boolean sealed;

      CylinderJob(ServerLevel level, Vec3 origin, Vec3 direction, double length, int radius, int scarRadius,
                  Runnable completion) {
         super(level);
         this.origin = origin;
         Vec3 flat = new Vec3(direction.x, 0.0, direction.z);
         this.forward = flat.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : flat.normalize();
         this.side = new Vec3(-this.forward.z, 0.0, this.forward.x);
         this.length = Math.max(0.0, length);
         this.radius = Math.max(1, radius);
         this.radiusSqr = this.radius * this.radius;
         this.scarRadius = Math.max(this.radius, scarRadius);
         this.sideOffset = -this.radius;
         this.verticalOffset = -this.radius;
         this.sealedDistance = this.length;
         this.completion = completion;
      }

      void advanceTo(double distance) { targetDistance = Math.max(targetDistance, Math.min(length, distance)); }
      void seal() { sealed = true; targetDistance = length; }
      void sealAt(double distance) {
         sealed = true;
         sealedDistance = Math.max(0.0, Math.min(length, distance));
         targetDistance = Math.max(targetDistance, sealedDistance);
      }
      @Override boolean ready() { return !done && along <= Math.floor(targetDistance + 1.0E-6); }

      @Override void advance() {
         if (phase == CORE_PHASE) advanceCore();
         else advanceScar();
      }

      private void advanceCore() {
         int currentSide = sideOffset, currentY = verticalOffset;
         coreCursor();
         if (currentSide * currentSide + currentY * currentY > radiusSqr) return;
         Vec3 point = origin.add(forward.scale(along)).add(side.scale(currentSide)).add(0.0, currentY, 0.0);
         BlockPos pos = BlockPos.containing(point);
         if (valid(pos, Float.MAX_VALUE)) level.removeBlock(pos, false);
      }

      private void advanceScar() {
         int currentAlong = along, currentSide = sideOffset, currentDepth = scarDepth;
         int depth = scarDepthAt(currentAlong, currentSide);
         scarCursor();
         if (currentDepth >= depth) return;
         Vec3 column = origin.add(forward.scale(currentAlong)).add(side.scale(currentSide));
         int x = Mth.floor(column.x), z = Mth.floor(column.z);
         int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
         BlockPos pos = new BlockPos(x, surfaceY - currentDepth, z);
         if (valid(pos, Float.MAX_VALUE)) level.removeBlock(pos, false);
      }

      private int scarDepthAt(int distance, int offset) {
         int absolute = Math.abs(offset);
         if (absolute <= radius || absolute > scarRadius) return 0;
         if (absolute <= radius + 3) return radius + 5 - absolute;
         double wanderingCrack = radius + 4.0 + (scarRadius - radius - 4.0)
            * (0.5 + 0.5 * Math.sin(distance * 0.087));
         boolean mainCrack = Math.abs(absolute - wanderingCrack) < 1.15;
         boolean branch = Math.floorMod(distance, 37) < 6
            && absolute <= radius + 4 + Math.floorMod(distance, 37) / 2;
         if (!mainCrack && !branch) return 0;
         return 1 + Math.floorMod(distance * 31 + offset * 17, 3);
      }

      private void coreCursor() {
         if (++verticalOffset > radius) {
            verticalOffset = -radius;
            if (++sideOffset > radius) {
               phase = SCAR_PHASE;
               sideOffset = -scarRadius;
               scarDepth = 0;
            }
         }
      }

      private void scarCursor() {
         if (++scarDepth >= 4) {
            scarDepth = 0;
            if (++sideOffset > scarRadius) nextSlice();
         }
      }

      private void nextSlice() {
         along++;
         phase = CORE_PHASE;
         sideOffset = -radius;
         verticalOffset = -radius;
         if (along > Math.ceil(sealedDistance) && sealed) done = true;
      }
   }

   public static final class AdvancingSkyRift {
      private final SkyRiftJob job;
      private AdvancingSkyRift(SkyRiftJob job) { this.job = job; }
      public void advanceTo(double distance) { job.advanceTo(distance); }
      public void seal() { job.seal(); }
      public void sealAt(double distance) { job.sealAt(distance); }
      public boolean isComplete() { return job.done; }
   }

   private static final class SkyRiftJob extends Job {
      final Vec3 origin, forward, side;
      final double length;
      final int radius, radiusSqr, minY;
      int along, sideOffset, currentY;
      double targetDistance;
      double sealedDistance;
      boolean sealed, columnReady;

      SkyRiftJob(ServerLevel level, Vec3 origin, Vec3 direction, double length, int radius, Runnable completion) {
         super(level);
         this.origin = origin;
         Vec3 flat = new Vec3(direction.x, 0.0, direction.z);
         this.forward = flat.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : flat.normalize();
         this.side = new Vec3(-this.forward.z, 0.0, this.forward.x);
         this.length = Math.max(0.0, length);
         this.radius = Math.max(1, radius);
         this.radiusSqr = this.radius * this.radius;
         this.minY = Mth.clamp(Mth.floor(origin.y), level.getMinBuildHeight(), level.getMaxBuildHeight() - 1);
         this.sideOffset = -this.radius;
         this.sealedDistance = this.length;
         this.completion = completion;
      }

      void advanceTo(double distance) { targetDistance = Math.max(targetDistance, Math.min(length, distance)); }
      void seal() { sealed = true; targetDistance = length; }
      void sealAt(double distance) {
         sealed = true;
         sealedDistance = Math.max(0.0, Math.min(length, distance));
         targetDistance = Math.max(targetDistance, sealedDistance);
      }
      @Override boolean ready() { return !done && along <= Math.floor(targetDistance + 1.0E-6); }

      @Override void advance() {
         while (!done && ready()) {
            if (!columnReady && !prepareColumn()) continue;
            BlockPos pos = columnPos(currentY);
            currentY--;
            if (currentY < minY) nextColumn();
            if (valid(pos, Float.MAX_VALUE)) level.removeBlock(pos, false);
            return;
         }
      }

      private boolean prepareColumn() {
         if (sideOffset * sideOffset > radiusSqr) {
            nextColumn();
            return false;
         }
         Vec3 column = origin.add(forward.scale(along)).add(side.scale(sideOffset));
         int x = Mth.floor(column.x), z = Mth.floor(column.z);
         currentY = Math.min(level.getMaxBuildHeight() - 1, level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1);
         if (currentY < minY) {
            nextColumn();
            return false;
         }
         columnReady = true;
         return true;
      }

      private BlockPos columnPos(int y) {
         Vec3 column = origin.add(forward.scale(along)).add(side.scale(sideOffset));
         return new BlockPos(Mth.floor(column.x), y, Mth.floor(column.z));
      }

      private void nextColumn() {
         columnReady = false;
         if (++sideOffset > radius) {
            sideOffset = -radius;
            if (++along > Math.ceil(sealedDistance) && sealed) done = true;
         }
      }
   }

   public static final class ExpandingSphere {
      private final ExpandingSphereJob job;
      private ExpandingSphere(ExpandingSphereJob job) { this.job = job; }
      public void advanceTo(double radius) { job.advanceTo(radius); }
      public void seal() { job.seal(); }
      public boolean isComplete() { return job.done; }
   }

   private static final class ExpandingSphereJob extends Job {
      final Vec3 center;
      final int maxRadius;
      int shell, x, y, z;
      double targetRadius;
      boolean sealed;

      ExpandingSphereJob(ServerLevel level, Vec3 center, int radius, Runnable completion) {
         super(level);
         this.center = center;
         this.maxRadius = Math.max(1, radius);
         this.completion = completion;
      }
      void advanceTo(double radius) { targetRadius = Math.max(targetRadius, Math.min(maxRadius, radius)); }
      void seal() { sealed = true; targetRadius = maxRadius; }
      @Override boolean ready() { return !done && shell <= Math.floor(targetRadius + 1.0E-6); }
      @Override void advance() {
         int cx = x, cy = y, cz = z, currentShell = shell;
         cursor();
         if (Math.max(Math.max(Math.abs(cx), Math.abs(cy)), Math.abs(cz)) != currentShell) return;
         if (cx * cx + cy * cy + cz * cz > maxRadius * maxRadius) return;
         BlockPos pos = BlockPos.containing(center.x + cx, center.y + cy, center.z + cz);
         if (valid(pos, Float.MAX_VALUE)) level.removeBlock(pos, false);
      }
      void cursor() {
         if (++z > shell) {
            z = -shell;
            if (++y > shell) {
               y = -shell;
               if (++x > shell) {
                  if (++shell > maxRadius) { if (sealed) done = true; return; }
                  x = y = z = -shell;
               }
            }
         }
      }
   }

   private static final class SweptBladeJob extends Job {
      final Vec3 previousCenter, currentCenter, previousAxis, currentAxis, faceNormal;
      final double bladeLength, radiusSqr;
      final boolean burnTrace;
      final BooleanSupplier active;
      final int motionSteps, alongSteps, crossRadius, crossStep;
      int motion, along, crossA, crossB;
      final Set<Long> burned = new HashSet<>();

      SweptBladeJob(ServerLevel level, Vec3 previousCenter, Vec3 currentCenter, Vec3 previousAxis,
                    Vec3 currentAxis, Vec3 faceNormal, double bladeLength, double radius,
                    boolean burnTrace, BooleanSupplier active) {
         super(level);
         this.previousCenter = previousCenter;
         this.currentCenter = currentCenter;
         this.previousAxis = previousAxis.lengthSqr() < 1.0E-6 ? new Vec3(0, 1, 0) : previousAxis.normalize();
         this.currentAxis = currentAxis.lengthSqr() < 1.0E-6 ? this.previousAxis : currentAxis.normalize();
         Vec3 normal = faceNormal == null ? Vec3.ZERO : faceNormal.normalize();
         this.faceNormal = normal.lengthSqr() < 1.0E-6 ? new Vec3(0, 0, 1) : normal;
         this.bladeLength = Math.max(2.0, bladeLength);
         double sweepRadius = Math.max(1.0, radius);
         this.radiusSqr = sweepRadius * sweepRadius;
         this.burnTrace = burnTrace;
         this.active = active;
         this.motionSteps = Math.max(1, Mth.ceil(previousCenter.distanceTo(currentCenter) / 2.0));
         double alongStep = sweepRadius >= 8.0 ? 3.0 : 2.0;
         this.alongSteps = Math.max(1, Mth.ceil(this.bladeLength / alongStep));
         this.crossRadius = Mth.ceil(sweepRadius);
         this.crossStep = sweepRadius >= 8.0 ? 3 : 1;
         this.crossA = this.crossB = -this.crossRadius;
      }

      @Override void advance() {
         if (active != null && !active.getAsBoolean()) { done = true; return; }
         int currentMotion = motion, currentAlong = along, currentA = crossA, currentB = crossB;
         cursor();
         if (currentA * currentA + currentB * currentB > radiusSqr) return;
         double motionProgress = currentMotion / (double)motionSteps;
         double alongProgress = currentAlong / (double)alongSteps;
         Vec3 center = previousCenter.lerp(currentCenter, motionProgress);
         Vec3 axis = previousAxis.lerp(currentAxis, motionProgress);
         if (axis.lengthSqr() < 1.0E-6) axis = currentAxis;
         axis = axis.normalize();
         Vec3 widthAxis = faceNormal.cross(axis);
         if (widthAxis.lengthSqr() < 1.0E-6) widthAxis = new Vec3(1, 0, 0);
         widthAxis = widthAxis.normalize();
         Vec3 point = center.add(axis.scale((alongProgress - 0.5) * bladeLength))
            .add(faceNormal.scale(currentA)).add(widthAxis.scale(currentB));
         BlockPos pos = BlockPos.containing(point);
         if (!valid(pos, Float.MAX_VALUE)) return;
         if (level.removeBlock(pos, false) && burnTrace) burnAdjacent(pos);
      }

      private void burnAdjacent(BlockPos cut) {
         for (Direction direction : Direction.values()) {
            BlockPos adjacent = cut.relative(direction);
            if (!burned.add(adjacent.asLong()) || !level.hasChunkAt(adjacent)) continue;
            BlockState state = level.getBlockState(adjacent);
            if (state.isAir() || state.is(Blocks.BEDROCK) || level.getBlockEntity(adjacent) != null) continue;
            long hash = adjacent.asLong() * 341873128712L + 132897987541L;
            BlockState replacement = ((hash >>> 16) & 0xFFL) < 38L
               ? Blocks.MAGMA_BLOCK.defaultBlockState() : Blocks.NETHERRACK.defaultBlockState();
            level.setBlock(adjacent, replacement, 3);
         }
      }

      private void cursor() {
         crossB += crossStep;
         if (crossB > crossRadius) {
            crossB = -crossRadius;
            crossA += crossStep;
            if (crossA > crossRadius) {
               crossA = -crossRadius;
               if (++along > alongSteps) {
                  along = 0;
                  if (++motion > motionSteps) done = true;
               }
            }
         }
      }
   }

   private static final class BladeTraceJob extends Job {
      final List<TraceSegment> segments;
      final double radius, radiusSqr;
      final boolean burnTrace;
      final BooleanSupplier active;
      final Set<Long> burned = new HashSet<>();
      int segmentIndex;
      int along, crossA, crossB;
      int alongMax, crossRadius;
      Vec3 start, forward, widthAxis, heightAxis;
      boolean segmentReady;

      BladeTraceJob(ServerLevel level, List<TraceSegment> segments, double radius,
                    boolean burnTrace, BooleanSupplier active) {
         super(level);
         this.segments = List.copyOf(segments);
         this.radius = Math.max(1.0, radius);
         this.radiusSqr = this.radius * this.radius;
         this.crossRadius = Mth.ceil(this.radius);
         this.burnTrace = burnTrace;
         this.active = active;
      }

      @Override void advance() {
         if (active != null && !active.getAsBoolean()) { done = true; return; }
         if (!segmentReady && !prepareSegment()) return;
         int currentAlong = along;
         int currentA = crossA;
         int currentB = crossB;
         cursor();
         if (currentA * currentA + currentB * currentB > radiusSqr) return;
         Vec3 point = start.add(forward.scale(currentAlong)).add(widthAxis.scale(currentA)).add(heightAxis.scale(currentB));
         BlockPos pos = BlockPos.containing(point);
         if (!valid(pos, Float.MAX_VALUE)) return;
         if (level.removeBlock(pos, false) && burnTrace) burnAdjacent(pos);
      }

      private boolean prepareSegment() {
         while (segmentIndex < segments.size()) {
            TraceSegment segment = segments.get(segmentIndex++);
            if (segment == null || segment.start() == null || segment.end() == null) continue;
            Vec3 delta = segment.end().subtract(segment.start());
            double length = delta.length();
            if (length < 1.0E-4) continue;
            this.start = segment.start();
            this.forward = delta.scale(1.0 / length);
            Vec3 worldUp = Math.abs(forward.y) > 0.92 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
            this.widthAxis = forward.cross(worldUp).normalize();
            this.heightAxis = widthAxis.cross(forward).normalize();
            this.alongMax = Mth.ceil(length);
            this.along = 0;
            this.crossA = -crossRadius;
            this.crossB = -crossRadius;
            this.segmentReady = true;
            return true;
         }
         done = true;
         return false;
      }

      private void cursor() {
         crossB++;
         if (crossB > crossRadius) {
            crossB = -crossRadius;
            if (++crossA > crossRadius) {
               crossA = -crossRadius;
               if (++along > alongMax) segmentReady = false;
            }
         }
      }

      private void burnAdjacent(BlockPos cut) {
         for (Direction direction : Direction.values()) {
            BlockPos adjacent = cut.relative(direction);
            if (!burned.add(adjacent.asLong()) || !level.hasChunkAt(adjacent)) continue;
            BlockState state = level.getBlockState(adjacent);
            if (state.isAir() || state.is(Blocks.BEDROCK) || level.getBlockEntity(adjacent) != null) continue;
            long hash = adjacent.asLong() * 341873128712L + 132897987541L;
            BlockState replacement = ((hash >>> 16) & 0xFFL) < 38L
               ? Blocks.MAGMA_BLOCK.defaultBlockState() : Blocks.NETHERRACK.defaultBlockState();
            level.setBlock(adjacent, replacement, 3);
         }
      }
   }
   private static final class DirectionalJob extends Job {
      final Vec3 origin, forward, right, up; final double length, width, height; final boolean funnel; final float maxHardness; int along, lateral, vertical;
      DirectionalJob(ServerLevel level,Vec3 origin,Vec3 direction,double length,double width,double height,boolean funnel,float maxHardness){super(level);this.origin=origin;this.forward=direction.lengthSqr()<1e-6?new Vec3(0,0,1):direction.normalize();Vec3 worldUp=Math.abs(forward.y)>.95?new Vec3(0,0,1):new Vec3(0,1,0);this.right=forward.cross(worldUp).normalize();this.up=right.cross(forward).normalize();this.length=length;this.width=width;this.height=height;this.funnel=funnel;this.maxHardness=maxHardness;lateral=(int)-Math.ceil(width/2);vertical=(int)-Math.ceil(height/2);}
      void advance(){double distance=along*.5,progress=distance/length;double factor;if(funnel)factor=progress<.35?Math.max(.08,progress/.35):progress>.8?Math.max(.1,1-(progress-.8)*4.5):1;else factor=Math.max(.18,Math.sin(Math.PI*Math.max(0,Math.min(1,progress))));double hw=width*.5*factor,hh=height*.5*factor;int lat=lateral,v=vertical;cursor();if(Math.abs(lat)>hw||Math.abs(v)>hh||distance<4)return;BlockPos p=BlockPos.containing(origin.add(forward.scale(distance)).add(right.scale(lat)).add(up.scale(v)));if(valid(p,maxHardness))level.removeBlock(p,false);}
      void cursor(){int maxLat=(int)Math.ceil(width/2),maxY=(int)Math.ceil(height/2);if(++vertical>maxY){vertical=-maxY;if(++lateral>maxLat){lateral=-maxLat;if(++along>(int)Math.ceil(length*2))done=true;}}}
   }
   private static final class UpperHemisphereJob extends Job {
      final Vec3 center; final int radius; final double radiusSqr, minimumY; final float hardness; int x, y, z;
      UpperHemisphereJob(ServerLevel level,Vec3 center,double radius,double minimumY,float hardness){super(level);this.center=center;this.radius=(int)Math.ceil(radius);this.radiusSqr=radius*radius;this.minimumY=minimumY;this.hardness=hardness;x=z=-this.radius;y=0;}
      void advance(){int cx=x,cy=y,cz=z;cursor();double worldY=center.y+cy;if(worldY<=minimumY||cx*cx+cy*cy+cz*cz>radiusSqr)return;BlockPos pos=BlockPos.containing(center.x+cx,worldY,center.z+cz);if(valid(pos,hardness))level.removeBlock(pos,false);}
      void cursor(){if(++z>radius){z=-radius;if(++y>radius){y=0;if(++x>radius)done=true;}}}
   }
}
