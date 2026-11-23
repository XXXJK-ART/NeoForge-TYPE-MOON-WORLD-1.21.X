package net.xxxjk.TYPE_MOON_WORLD.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.DiarmuidTrailRenderType;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantBaseAction;

/** Short green ribbons through the actual rendered blade tip, including riding and model scaling. */
@EventBusSubscriber(modid = "typemoonworld", value = Dist.CLIENT)
public final class ZhaoYunSpearTrailClient {
   private static final float LIFETIME = 3.0F;
   private static final Map<UUID, Trail> TRAILS = new HashMap<>();
   private static ClientLevel activeLevel;
   private record Point(Vec3 position, double time) { }
   private static final class Trail {
      final LivingEntity owner;
      final long start;
      final float width;
      final ArrayDeque<Point> points = new ArrayDeque<>();
      Trail(LivingEntity owner, long start, int action) {
         this.owner = owner; this.start = start;
         this.width = action == ServantBaseAction.ZHAO_YUN_HELD_DANCE ? 0.15F
            : action == ServantBaseAction.ZHAO_YUN_RECOVERY ? 0.08F : 0.095F;
      }
   }
   private ZhaoYunSpearTrailClient() { }

   private static void ensureLevel() {
      if (activeLevel != Minecraft.getInstance().level) {
         TRAILS.clear(); activeLevel = Minecraft.getInstance().level;
      }
   }

   public static void capture(LivingEntity owner, ServantBaseAnimationClient.ActiveAction action, Vec3 tip, float partial) {
      ensureLevel();
      Trail trail = TRAILS.get(owner.getUUID());
      if (trail == null || trail.owner != owner || trail.start != action.start()) {
         trail = new Trail(owner, action.start(), action.action()); TRAILS.put(owner.getUUID(), trail);
      }
      double now = owner.level().getGameTime() + (double)partial;
      Point previous = trail.points.peekLast();
      if (previous != null && (now < previous.time || previous.position.distanceToSqr(tip) > 16)) trail.points.clear();
      if (previous == null || previous.position.distanceToSqr(tip) > 0.00001) trail.points.addLast(new Point(tip, now));
      while (trail.points.size() > 96) trail.points.removeFirst();
   }

   @SubscribeEvent
   public static void render(RenderLevelStageEvent event) {
      if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
      ensureLevel();
      var mc = Minecraft.getInstance();
      if (mc.level == null) return;
      float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
      double now = mc.level.getGameTime() + (double)partial;
      var buffers = mc.renderBuffers().bufferSource();
      var type = DiarmuidTrailRenderType.trail();
      var vertices = buffers.getBuffer(type);
      Vec3 camera = event.getCamera().getPosition();
      var iterator = TRAILS.values().iterator();
      while (iterator.hasNext()) {
         Trail trail = iterator.next();
         while (!trail.points.isEmpty() && now - trail.points.peekFirst().time > LIFETIME) trail.points.removeFirst();
         if (!trail.owner.isAlive() || trail.owner.isRemoved() || trail.points.isEmpty()) { iterator.remove(); continue; }
         var points = trail.points.toArray(Point[]::new);
         // Smooth only between captured blade positions; low frame rates must not turn a
         // gunflower into a polygon, and the live tip remains the exact sampled endpoint.
         for (int index = 0; index + 1 < points.length; index++) {
            Point a = points[index], b = points[index + 1];
            Vec3 before = points[Math.max(0, index - 1)].position;
            Vec3 after = points[Math.min(points.length - 1, index + 2)].position;
            Point previous = a;
            Vec3 previousTangent = tangent(before, a.position, b.position, after, 0);
            for (int step = 1; step <= 4; step++) {
               double weight = step / 4.0;
               Point point = new Point(curve(before, a.position, b.position, after, weight),
                  a.time + (b.time - a.time) * weight);
               Vec3 nextTangent = tangent(before, a.position, b.position, after, weight);
               segment(event.getPoseStack(), vertices, previous, point, previousTangent, nextTangent, camera, now, trail.width);
               previous = point;
               previousTangent = nextTangent;
            }
         }
      }
      buffers.endBatch(type);
   }

   private static Vec3 tangent(Vec3 before, Vec3 a, Vec3 b, Vec3 after, double t) {
      return curve(before, a, b, after, Math.min(1, t + 0.001))
         .subtract(curve(before, a, b, after, Math.max(0, t - 0.001)));
   }

   private static Vec3 curve(Vec3 before, Vec3 a, Vec3 b, Vec3 after, double t) {
      Vec3 incoming = b.subtract(before).scale(0.5), outgoing = after.subtract(a).scale(0.5);
      double length = a.distanceTo(b);
      if (incoming.lengthSqr() > length * length) incoming = incoming.normalize().scale(length);
      if (outgoing.lengthSqr() > length * length) outgoing = outgoing.normalize().scale(length);
      double t2 = t * t, t3 = t2 * t;
      return a.scale(2 * t3 - 3 * t2 + 1).add(incoming.scale(t3 - 2 * t2 + t))
         .add(b.scale(-2 * t3 + 3 * t2)).add(outgoing.scale(t3 - t2));
   }

   private static void segment(PoseStack poses, VertexConsumer vertices, Point first, Point second,
      Vec3 firstTangent, Vec3 secondTangent, Vec3 camera, double now, float width) {
      Vec3 a = first.position.subtract(camera), b = second.position.subtract(camera), direction = b.subtract(a);
      if (direction.lengthSqr() < 1.0E-8) return;
      Vec3 sideA = side(firstTangent, a), sideB = side(secondTangent, b);
      float lifeA = 1F - Mth.clamp((float)(now - first.time) / LIFETIME, 0F, 1F);
      float lifeB = 1F - Mth.clamp((float)(now - second.time) / LIFETIME, 0F, 1F);
      for (int strip = 0; strip < 6; strip++) {
         float lo = -1F + strip / 3F, hi = lo + 1F / 3F;
         vertex(poses, vertices, a.add(sideA.scale(width * lo)), lo, lifeA);
         vertex(poses, vertices, b.add(sideB.scale(width * lo)), lo, lifeB);
         vertex(poses, vertices, b.add(sideB.scale(width * hi)), hi, lifeB);
         vertex(poses, vertices, a.add(sideA.scale(width * hi)), hi, lifeA);
      }
   }

   private static Vec3 side(Vec3 tangent, Vec3 point) {
      Vec3 side = tangent.cross(point.scale(-1)).normalize();
      return side.lengthSqr() < 1.0E-8 ? tangent.cross(new Vec3(0, 1, 0)).normalize() : side;
   }

   private static void vertex(PoseStack poses, VertexConsumer vertices, Vec3 point, float edge, float life) {
      float core = Math.max(0F, 1F - Math.abs(edge)), hot = (float)Math.pow(core, 5);
      vertices.addVertex(poses.last(), (float)point.x, (float)point.y, (float)point.z)
         .setColor(0.07F + hot * 0.65F, 0.95F, 0.24F + hot * 0.58F, core * life * 0.8F);
   }
}
