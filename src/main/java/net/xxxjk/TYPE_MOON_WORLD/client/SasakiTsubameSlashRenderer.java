package net.xxxjk.TYPE_MOON_WORLD.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.vfx.client.VFXMaterialRenderTypes;

/** Continuous, depth-tested blade ribbons; the particle effect supplies the loose sparks. */
final class SasakiTsubameSlashRenderer {
   private static final int SEGMENTS = 48;
   private static final float TRAIL_TRAVEL_TICKS = 11.0F;
   private static final Vec3 CONVERGENCE_POINT = new Vec3(0.0, 1.32, -8.4);
   private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
      TYPE_MOON_WORLD.MOD_ID, "textures/particle/sasaki_tsubame_blade_stretched.png");

   private SasakiTsubameSlashRenderer() { }

   /**
    * Build one of the three authored sweeps. They start around the wielder, bow through
    * different sides of a shallow 3D spiral, and all arrive at the same forward point.
    */
   static List<Vec3> trailPath(int blade, float age, Vec3 bladeStart) {
      float progress = Mth.clamp(age / TRAIL_TRAVEL_TICKS, 0.0F, 1.0F);
      int segments = Math.max(2, Mth.ceil(SEGMENTS * progress));
      List<Vec3> path = new ArrayList<>(segments + 1);
      for (int i = 0; i <= segments; i++) {
         float t = progress * i / segments;
         path.add(sweepPoint(blade, t, bladeStart));
      }
      return path;
   }

   private static Vec3 sweepPoint(int blade, float t, Vec3 bladeStart) {
      int lane = Mth.clamp(blade - 1, 0, 2);
      double phase = lane * (Math.PI * 2.0 / 3.0);
      double c = Math.cos(phase);
      double s = Math.sin(phase);
      double startZ = Math.min(-0.65, bladeStart.z + 0.12 * c);
      Vec3 start = new Vec3(bladeStart.x + 0.34 * c, bladeStart.y + 0.34 * s, startZ);
      Vec3 bendA = new Vec3(1.42 * Math.cos(phase + 0.42), 1.32 + 1.12 * Math.sin(phase + 0.42), -2.45 + 0.16 * s);
      Vec3 bendB = new Vec3(0.72 * Math.cos(phase + 1.34), 1.32 + 0.58 * Math.sin(phase + 1.34), -5.45 + 0.14 * c);

      float u = 1.0F - t;
      return start.scale(u * u * u)
         .add(bendA.scale(3.0F * u * u * t))
         .add(bendB.scale(3.0F * u * t * t))
         .add(CONVERGENCE_POINT.scale(t * t * t));
   }

   /** One open, forward-sweeping ribbon per sword, with its origin seeded at that sword's tip. */
   static void render(PoseStack poses, MultiBufferSource.BufferSource buffers, float age,
                      List<Vec3> bladePath, Vec3 camera) {
      float alpha = Mth.clamp(age / 0.5F, 0.0F, 1.0F)
         * (1.0F - Mth.clamp((age - 9.0F) / 9.0F, 0.0F, 1.0F));
      if (alpha <= 0.0F || bladePath.size() < 2) return;
      RenderType type = VFXMaterialRenderTypes.get("vanilla", TEXTURE, false);
      VertexConsumer consumer = buffers.getBuffer(type);
      tipRibbon(poses, consumer, bladePath, camera, 0.26F, alpha * 0.70F, false);
      tipRibbon(poses, consumer, bladePath, camera, 0.065F, alpha, true);
      buffers.endBatch(type);
   }

   /** Draws a short ribbon through sampled sword-tip positions for ordinary sword techniques. */
   static void renderBladeTrail(PoseStack poses, MultiBufferSource.BufferSource buffers, float ageSinceLastSample,
                                List<Vec3> bladePath, Vec3 camera) {
      if (bladePath.size() < 2) return;
      float alpha = 1.0F - Mth.clamp(ageSinceLastSample / 6.0F, 0.0F, 1.0F);
      if (alpha <= 0.0F) return;
      RenderType type = VFXMaterialRenderTypes.get("vanilla", TEXTURE, false);
      VertexConsumer consumer = buffers.getBuffer(type);
      tipRibbon(poses, consumer, bladePath, camera, 0.22F, alpha * 0.68F, false);
      tipRibbon(poses, consumer, bladePath, camera, 0.052F, alpha, true);
      buffers.endBatch(type);
   }

   private static void tipRibbon(PoseStack poses, VertexConsumer consumer, List<Vec3> path, Vec3 camera,
                                 float width, float alpha, boolean core) {
      Vec3 previousSide = Vec3.ZERO;
      for (int i = 0; i < path.size() - 1; i++) {
         float t0 = i / (float)(path.size() - 1);
         float t1 = (i + 1) / (float)(path.size() - 1);
         Vec3 center0 = path.get(i);
         Vec3 center1 = path.get(i + 1);
         Vec3 tangent = center1.subtract(center0);
         if (tangent.lengthSqr() < 1.0E-10) continue;
         Vec3 side = tangent.cross(camera.subtract(center0)).normalize();
         if (side.lengthSqr() < 1.0E-8) side = tangent.cross(new Vec3(0, 1, 0)).normalize();
         if (side.dot(previousSide) < 0) side = side.scale(-1);
         Vec3 side0 = previousSide.lengthSqr() < 1.0E-8 ? side : previousSide;
         float width0 = width * tailEnvelope(t0);
         float width1 = width * tailEnvelope(t1);
         float a0 = alpha * tailEnvelope(t0);
         float a1 = alpha * tailEnvelope(t1);
         float r = core ? 1.0F : 0.84F;
         float g = core ? 1.0F : 0.68F;
         vertex(poses, consumer, center0.add(side0.scale(-width0)), 1.0F - t0, 0.0F, r, g, 1.0F, a0);
         vertex(poses, consumer, center1.add(side.scale(-width1)), 1.0F - t1, 0.0F, r, g, 1.0F, a1);
         vertex(poses, consumer, center1.add(side.scale(width1)), 1.0F - t1, 1.0F, r, g, 1.0F, a1);
         vertex(poses, consumer, center0.add(side0.scale(width0)), 1.0F - t0, 1.0F, r, g, 1.0F, a0);
         previousSide = side;
      }
   }

   private static float tailEnvelope(float t) {
      // A fine tail grows into the bright blade tip, then tapers at the exact tip.
      return Mth.clamp(t * 4.0F, 0.0F, 1.0F) * (float)Math.sqrt(1.0F - t);
   }

   private static void ribbon(PoseStack poses, VertexConsumer consumer, Vec3 offset, float tilt,
                               float progress, float age, float lag, float width, float alpha,
                               int lane, boolean core) {
      float head = Mth.lerp(progress, -1.15F, 1.20F) + lag;
      float tail = head - 1.85F;
      for (int segment = 0; segment < SEGMENTS; segment++) {
         float t0 = segment / (float)SEGMENTS;
         float t1 = (segment + 1) / (float)SEGMENTS;
         float angle0 = Mth.lerp(t0, tail, head);
         float angle1 = Mth.lerp(t1, tail, head);
         float width0 = width * envelope(t0);
         float width1 = width * envelope(t1);
         Vec3 center0 = point(offset, tilt, progress, angle0);
         Vec3 center1 = point(offset, tilt, progress, angle1);
         float flow0 = 0.66F + 0.34F * Mth.sin(t0 * 32.0F - age * 2.8F + lane * 2.0F);
         float flow1 = 0.66F + 0.34F * Mth.sin(t1 * 32.0F - age * 2.8F + lane * 2.0F);
         float a0 = alpha * envelope(t0) * (core ? 1.0F : flow0);
         float a1 = alpha * envelope(t1) * (core ? 1.0F : flow1);
         float r = core ? 1.0F : 0.84F;
         float g = core ? 1.0F : 0.68F;
         float b = 1.0F;
         vertex(poses, consumer, center0, -width0, t0, 0.0F, r, g, b, a0);
         vertex(poses, consumer, center1, -width1, t1, 0.0F, r, g, b, a1);
         vertex(poses, consumer, center1, width1, t1, 1.0F, r, g, b, a1);
         vertex(poses, consumer, center0, width0, t0, 1.0F, r, g, b, a0);
      }
   }

   private static float envelope(float t) {
      return Mth.sin(t * Mth.PI) * (0.40F + 0.60F * t);
   }

   private static Vec3 point(Vec3 offset, float tilt, float progress, float angle) {
      float across = Mth.sin(angle) * 3.60F;
      float radians = tilt * Mth.DEG_TO_RAD;
      return new Vec3(offset.x + across * Mth.cos(radians),
         1.10 + offset.y + across * Mth.sin(radians) + Mth.cos(angle) * 0.45,
         offset.z - 1.10 - Mth.cos(angle * 0.5F) * 2.65 - progress * 1.30);
   }

   private static void vertex(PoseStack poses, VertexConsumer consumer, Vec3 center, float width,
                               float u, float v, float r, float g, float b, float a) {
      consumer.addVertex(poses.last(), (float)center.x, (float)center.y + width, (float)center.z)
         .setUv(u, v).setColor(r, g, b, a);
   }

   private static void vertex(PoseStack poses, VertexConsumer consumer, Vec3 point,
                               float u, float v, float r, float g, float b, float a) {
      consumer.addVertex(poses.last(), (float)point.x, (float)point.y, (float)point.z)
         .setUv(u, v).setColor(r, g, b, a);
   }

   static void renderCut(PoseStack poses, MultiBufferSource.BufferSource buffers, float age, float tilt, boolean thrust) {
      float progress = Mth.clamp(age / 4.0F, 0.0F, 1.0F);
      float alpha = Mth.clamp(age * 2.0F, 0.0F, 1.0F) * (1.0F - Mth.clamp((age - 3.0F) / 4.0F, 0.0F, 1.0F));
      if (alpha <= 0.0F || age >= 7.0F) return;
      RenderType type = VFXMaterialRenderTypes.get("vanilla", TEXTURE, false);
      poses.pushPose();
      poses.scale(thrust ? 0.22F : 0.66F, thrust ? 0.40F : 0.75F, thrust ? 0.95F : 0.70F);
      var consumer = buffers.getBuffer(type);
      ribbon(poses, consumer, Vec3.ZERO, tilt, progress, age, 0.0F, 0.50F, alpha * 0.85F, 0, false);
      ribbon(poses, consumer, Vec3.ZERO, tilt, progress, age, -0.12F, 0.14F, alpha, 0, true);
      poses.popPose();
      buffers.endBatch(type);
   }
}
