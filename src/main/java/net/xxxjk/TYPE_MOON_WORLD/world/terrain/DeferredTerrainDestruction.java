package net.xxxjk.TYPE_MOON_WORLD.world.terrain;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/** Shared, round-robin terrain queue. Damage logic never waits for terrain work. */
@EventBusSubscriber(modid = "typemoonworld")
public final class DeferredTerrainDestruction {
   private static final int MAX_CHECKS_PER_LEVEL_TICK = 4000;
   private static final long SOFT_BUDGET_NANOS = 4_000_000L;
   private static final Map<ResourceKey<Level>, ArrayDeque<Job>> JOBS = new HashMap<>();
   private DeferredTerrainDestruction() { }

   @FunctionalInterface public interface BlockPredicate { boolean canRemove(ServerLevel level, BlockPos pos, double distanceSqr, double radius, Vec3 center); }
   @FunctionalInterface public interface RemovalCallback { void onRemoved(ServerLevel level, BlockPos pos, int removed); }

   public static void queueSphere(ServerLevel level, Vec3 center, int radius, float maxHardness, int targetTicks) {
      if (radius > 0) add(level, new SphereJob(level, center, radius, radius, maxHardness, false, null, null));
   }
   public static void queueSphere(ServerLevel level, Vec3 center, double radius, int targetTicks, BlockPredicate predicate, RemovalCallback callback) {
      if (radius > 0 && predicate != null) add(level, new SphereJob(level, center, (int)Math.ceil(radius), radius, Float.MAX_VALUE, false, predicate, callback));
   }
   public static void queueShell(ServerLevel level, Vec3 center, double currentRadius, double previousRadius, int targetTicks, BlockPredicate predicate, RemovalCallback callback) {
      if (currentRadius > 0 && predicate != null) add(level, new SphereJob(level, center, (int)Math.ceil(currentRadius), currentRadius, Float.MAX_VALUE, true, predicate, callback).inner(previousRadius));
   }
   public static void queueEllipsoid(ServerLevel level, Vec3 center, int radius, int halfHeight, float maxHardness, int targetTicks) {
      if (radius > 0 && halfHeight > 0) add(level, new EllipsoidJob(level, center, radius, halfHeight, maxHardness));
   }
   public static void queueDiagonalCut(ServerLevel level, Vec3 origin, Vec3 direction, double length, double width, double height, Runnable completion) {
      add(level, new DiagonalJob(level, origin, direction, length, width, height, false, completion));
   }
   public static void queueDiagonalBurnShell(ServerLevel level, Vec3 origin, Vec3 direction, double length, double width, double height, double shell) {
      add(level, new DiagonalJob(level, origin, direction, length, width, height, true, null).shell(shell));
   }
   public static void queueDirectionalCut(ServerLevel level, Vec3 origin, Vec3 direction, double length, double width, double height, boolean funnel) {
      add(level, new DirectionalJob(level, origin, direction, length, width, height, funnel));
   }

   private static void add(ServerLevel level, Job job) { if (level != null && job != null) JOBS.computeIfAbsent(level.dimension(), k -> new ArrayDeque<>()).add(job); }
   @SubscribeEvent public static void tick(LevelTickEvent.Post event) {
      if (!(event.getLevel() instanceof ServerLevel level)) return;
      ArrayDeque<Job> queue = JOBS.get(level.dimension()); if (queue == null || queue.isEmpty()) return;
      long started = System.nanoTime(); int checked = 0;
      while (!queue.isEmpty() && checked < MAX_CHECKS_PER_LEVEL_TICK && System.nanoTime() - started < SOFT_BUDGET_NANOS) {
         Job job = queue.pollFirst(); int slice = 0;
         while (!job.done && slice < 128 && checked < MAX_CHECKS_PER_LEVEL_TICK) { job.advance(); slice++; checked++; }
         if (!job.done) queue.addLast(job); else job.finish();
      }
      if (queue.isEmpty()) JOBS.remove(level.dimension());
   }
   @SubscribeEvent public static void unload(LevelEvent.Unload event) { if (event.getLevel() instanceof Level level && !level.isClientSide()) JOBS.remove(level.dimension()); }

   private abstract static class Job {
      final ServerLevel level; boolean done; Runnable completion;
      Job(ServerLevel level) { this.level = level; }
      abstract void advance();
      void finish() { if (completion != null) completion.run(); }
      boolean valid(BlockPos pos, float maxHardness) {
         if (!level.hasChunkAt(pos) || level.getBlockEntity(pos) != null) return false;
         BlockState state = level.getBlockState(pos); float hardness = state.getDestroySpeed(level, pos);
         return !state.isAir() && !state.is(Blocks.BEDROCK) && hardness >= 0 && hardness <= maxHardness && state.getExplosionResistance(level, pos, null) < 1200;
      }
   }
   private static final class SphereJob extends Job {
      final Vec3 center; final int scan; final double radius, radiusSqr; final float hardness; final boolean shellOnly; final BlockPredicate predicate; final RemovalCallback callback;
      double innerSqr; int x, y, z, removed;
      SphereJob(ServerLevel level, Vec3 center, int scan, double radius, float hardness, boolean shellOnly, BlockPredicate predicate, RemovalCallback callback) { super(level); this.center=center; this.scan=scan; this.radius=radius; this.radiusSqr=radius*radius; this.hardness=hardness; this.shellOnly=shellOnly; this.predicate=predicate; this.callback=callback; x=y=z=-scan; }
      SphereJob inner(double value) { innerSqr=Math.max(0,value*value); return this; }
      void advance() { int cx=x,cy=y,cz=z; cursor(); double d=cx*cx+cy*cy+cz*cz; if(d>radiusSqr || d<=innerSqr)return; BlockPos p=BlockPos.containing(center.x+cx,center.y+cy,center.z+cz); if(predicate!=null ? predicate.canRemove(level,p,d,radius,center) : valid(p,hardness)){ if(level.removeBlock(p,false)){removed++; if(callback!=null)callback.onRemoved(level,p,removed);} } }
      void cursor(){if(++z>scan){z=-scan;if(++y>scan){y=-scan;if(++x>scan)done=true;}}}
   }
   private static final class EllipsoidJob extends Job {
      final Vec3 center; final int radius,height; final float hardness; int x,y,z;
      EllipsoidJob(ServerLevel level,Vec3 center,int radius,int height,float hardness){super(level);this.center=center;this.radius=radius;this.height=height;this.hardness=hardness;x=z=-radius;y=-height;}
      void advance(){int cx=x,cy=y,cz=z;cursor();double n=(cx*cx+cz*cz)/(double)(radius*radius)+(cy*cy)/(double)(height*height);if(n<=1){BlockPos p=BlockPos.containing(center.x+cx,center.y+cy,center.z+cz);if(valid(p,hardness))level.removeBlock(p,false);}}
      void cursor(){if(++z>radius){z=-radius;if(++y>height){y=-height;if(++x>radius)done=true;}}}
   }
   private static final class DiagonalJob extends Job {
      final Vec3 origin,forward,side; final double length,width,height; final boolean burn; double shell; int along,lateral,vertical;
      DiagonalJob(ServerLevel level,Vec3 origin,Vec3 direction,double length,double width,double height,boolean burn,Runnable completion){super(level);this.origin=origin;Vec3 flat=new Vec3(direction.x,0,direction.z);this.forward=flat.lengthSqr()<1e-6?new Vec3(0,0,1):flat.normalize();this.side=new Vec3(-forward.z,0,forward.x);this.length=length;this.width=width;this.height=height;this.burn=burn;this.completion=completion;lateral=(int)-Math.ceil(width/2);vertical=(int)-Math.ceil(height/2);}
      DiagonalJob shell(double shell){this.shell=shell;lateral=(int)-Math.ceil(width/2+shell);vertical=(int)-Math.ceil(height/2+shell);return this;}
      void advance(){double d=along*.5, halfW=width/2, halfH=height/2;double lat=lateral;double y=vertical;cursor();boolean core=Math.abs(lat)<=halfW&&Math.abs(y)<=halfH;if(burn&&core)return;if(!burn&&!core)return;Vec3 point=origin.add(forward.scale(d)).add(side.scale(lat+y*.85)).add(0,y,0);BlockPos p=BlockPos.containing(point);if(!valid(p,Float.MAX_VALUE))return;if(burn)level.setBlock(p,level.random.nextFloat()<.75F?Blocks.NETHERRACK.defaultBlockState():Blocks.MAGMA_BLOCK.defaultBlockState(),3);else level.removeBlock(p,false);}
      void cursor(){int maxLat=(int)Math.ceil(width/2+shell),maxY=(int)Math.ceil(height/2+shell);if(++vertical>maxY){vertical=-maxY;if(++lateral>maxLat){lateral=-maxLat;if(++along>(int)Math.ceil(length*2))done=true;}}}
   }
   private static final class DirectionalJob extends Job {
      final Vec3 origin, forward, right, up; final double length, width, height; final boolean funnel; int along, lateral, vertical;
      DirectionalJob(ServerLevel level,Vec3 origin,Vec3 direction,double length,double width,double height,boolean funnel){super(level);this.origin=origin;this.forward=direction.lengthSqr()<1e-6?new Vec3(0,0,1):direction.normalize();Vec3 worldUp=Math.abs(forward.y)>.95?new Vec3(0,0,1):new Vec3(0,1,0);this.right=forward.cross(worldUp).normalize();this.up=right.cross(forward).normalize();this.length=length;this.width=width;this.height=height;this.funnel=funnel;lateral=(int)-Math.ceil(width/2);vertical=(int)-Math.ceil(height/2);}
      void advance(){double distance=along*.5,progress=distance/length;double factor;if(funnel)factor=progress<.35?Math.max(.08,progress/.35):progress>.8?Math.max(.1,1-(progress-.8)*4.5):1;else factor=Math.max(.18,Math.sin(Math.PI*Math.max(0,Math.min(1,progress))));double hw=width*.5*factor,hh=height*.5*factor;int lat=lateral,v=vertical;cursor();if(Math.abs(lat)>hw||Math.abs(v)>hh||distance<4)return;BlockPos p=BlockPos.containing(origin.add(forward.scale(distance)).add(right.scale(lat)).add(up.scale(v)));if(valid(p,80))level.removeBlock(p,false);}
      void cursor(){int maxLat=(int)Math.ceil(width/2),maxY=(int)Math.ceil(height/2);if(++vertical>maxY){vertical=-maxY;if(++lateral>maxLat){lateral=-maxLat;if(++along>(int)Math.ceil(length*2))done=true;}}}
   }
}
