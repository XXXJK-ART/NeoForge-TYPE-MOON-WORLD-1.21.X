package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

final class MagicOrbProjectileRenderHelper {
   private static final ResourceLocation TRAIL_TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/beacon_beam.png");

   private MagicOrbProjectileRenderHelper() {
   }

   static <T extends Entity> void renderOrb(
      EntityRenderer<T> renderer, T entity, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, Quaternionf cameraOrientation, float scale, float r, float g, float b
   ) {
      float time = entity.level().getGameTime() + partialTicks;
      float pulse = 1.0F + 0.08F * (float)Math.sin(time * 0.55F);
      poseStack.pushPose();
      poseStack.mulPose(cameraOrientation);
      poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
      poseStack.mulPose(Axis.ZP.rotationDegrees(time * 4.0F));
      Pose pose = poseStack.last();
      VertexConsumer consumer = buffer.getBuffer(GanderOrbRenderType.orb());
      drawOrbQuad(pose, consumer, 0.13F * scale, 1.0F, 1.0F, 1.0F, 0.95F);
      drawOrbQuad(pose, consumer, 0.22F * scale, r, g, b, 0.92F);
      drawOrbQuad(pose, consumer, 0.32F * scale * pulse, r, g, b, 0.58F);
      poseStack.popPose();
   }

   static <T extends Entity> void renderTrail(
      EntityRenderer<T> renderer, T entity, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, Vec3 cameraPosition, List<Vec3> trace, float r, float g, float b
   ) {
      if (trace == null || trace.size() < 2) {
         return;
      }
      Vec3 currentPos = entity.getPosition(partialTicks);
      List<Vec3> points = new ArrayList<>(trace);
      points.add(currentPos);
      if (points.size() < 2) {
         return;
      }

      VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucentEmissive(TRAIL_TEXTURE));
      Vec3 viewOffset = cameraPosition.subtract(currentPos);
      poseStack.pushPose();
      Pose pose = poseStack.last();
      int samplesPerSegment = 2;
      float textureRepeat = Math.max(1.0F, points.size() / 18.0F);

      for (int i = 0; i < points.size() - 1; i++) {
         Vec3 p1 = points.get(i);
         Vec3 p2 = points.get(i + 1);
         Vec3 p0 = i > 0 ? points.get(i - 1) : p1.subtract(p2.subtract(p1));
         Vec3 p3 = i < points.size() - 2 ? points.get(i + 2) : p2.add(p2.subtract(p1));

         for (int j = 0; j < samplesPerSegment; j++) {
            float t1 = (float)j / samplesPerSegment;
            float t2 = (float)(j + 1) / samplesPerSegment;
            Vec3 start = catmullRom(t1, p0, p1, p2, p3).subtract(currentPos);
            Vec3 end = catmullRom(t2, p0, p1, p2, p3).subtract(currentPos);
            float total = (points.size() - 1) * samplesPerSegment;
            float progress1 = (i * samplesPerSegment + j) / total;
            float progress2 = (i * samplesPerSegment + j + 1.0F) / total;
            float u1 = progress1 * textureRepeat;
            float u2 = progress2 * textureRepeat;
            drawBillboardSegment(pose, consumer, start, end, viewOffset, width(0.16F, progress1), width(0.16F, progress2), alpha(progress1), alpha(progress2), u1, u2, r, g, b);
            drawBillboardSegment(pose, consumer, start, end, viewOffset, width(0.07F, progress1), width(0.07F, progress2), 0.42F, 0.62F, u1, u2, 1.0F, 1.0F, 1.0F);
         }
      }

      poseStack.popPose();
   }

   private static void drawOrbQuad(Pose pose, VertexConsumer consumer, float halfSize, float r, float g, float b, float a) {
      consumer.addVertex(pose, -halfSize, -halfSize, 0.0F).setColor(r, g, b, a).setUv(0.0F, 0.0F);
      consumer.addVertex(pose, halfSize, -halfSize, 0.0F).setColor(r, g, b, a).setUv(1.0F, 0.0F);
      consumer.addVertex(pose, halfSize, halfSize, 0.0F).setColor(r, g, b, a).setUv(1.0F, 1.0F);
      consumer.addVertex(pose, -halfSize, halfSize, 0.0F).setColor(r, g, b, a).setUv(0.0F, 1.0F);
   }

   private static Vec3 catmullRom(float t, Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3) {
      float t2 = t * t;
      float t3 = t2 * t;
      double x = 0.5 * (2.0 * p1.x + (-p0.x + p2.x) * t + (2.0 * p0.x - 5.0 * p1.x + 4.0 * p2.x - p3.x) * t2 + (-p0.x + 3.0 * p1.x - 3.0 * p2.x + p3.x) * t3);
      double y = 0.5 * (2.0 * p1.y + (-p0.y + p2.y) * t + (2.0 * p0.y - 5.0 * p1.y + 4.0 * p2.y - p3.y) * t2 + (-p0.y + 3.0 * p1.y - 3.0 * p2.y + p3.y) * t3);
      double z = 0.5 * (2.0 * p1.z + (-p0.z + p2.z) * t + (2.0 * p0.z - 5.0 * p1.z + 4.0 * p2.z - p3.z) * t2 + (-p0.z + 3.0 * p1.z - 3.0 * p2.z + p3.z) * t3);
      return new Vec3(x, y, z);
   }

   private static float width(float base, float progress) {
      return base * (0.2F + 0.8F * (float)Math.sqrt(progress));
   }

   private static float alpha(float progress) {
      return 0.15F + 0.44F * progress * progress;
   }

   private static void drawBillboardSegment(
      Pose pose, VertexConsumer consumer, Vec3 start, Vec3 end, Vec3 viewOffset, float width1, float width2, float alpha1, float alpha2, float u1, float u2, float r, float g, float b
   ) {
      Vec3 dir = end.subtract(start);
      if (dir.lengthSqr() < 1.0E-6) {
         return;
      }
      Vec3 viewDir = start.subtract(viewOffset);
      Vec3 cross = dir.cross(viewDir);
      if (cross.lengthSqr() < 1.0E-6) {
         cross = dir.cross(new Vec3(0.0, 1.0, 0.0));
         if (cross.lengthSqr() < 1.0E-6) {
            cross = new Vec3(1.0, 0.0, 0.0);
         }
      }

      Vec3 right = cross.normalize();
      Vec3 offset1 = right.scale(width1 * 0.5);
      Vec3 offset2 = right.scale(width2 * 0.5);
      vertex(pose, consumer, start.subtract(offset1), r, g, b, alpha1, u1, 0.0F);
      vertex(pose, consumer, end.subtract(offset2), r, g, b, alpha2, u2, 0.0F);
      vertex(pose, consumer, end.add(offset2), r, g, b, alpha2, u2, 1.0F);
      vertex(pose, consumer, start.add(offset1), r, g, b, alpha1, u1, 1.0F);
   }

   private static void vertex(Pose pose, VertexConsumer consumer, Vec3 pos, float r, float g, float b, float a, float u, float v) {
      consumer.addVertex(pose, (float)pos.x, (float)pos.y, (float)pos.z)
         .setColor(r, g, b, a)
         .setUv(u, v)
         .setOverlay(OverlayTexture.NO_OVERLAY)
         .setLight(15728880)
         .setNormal(pose, 0.0F, 1.0F, 0.0F);
   }
}
