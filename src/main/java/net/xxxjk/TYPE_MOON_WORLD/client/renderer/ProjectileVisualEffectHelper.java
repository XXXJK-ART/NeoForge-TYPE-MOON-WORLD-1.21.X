package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.magic.MagicCircuitColorHelper;

public final class ProjectileVisualEffectHelper {
   private static final ResourceLocation CIRCUIT_TEXTURE = ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/models/armor/magic_circuit_item.png");

   private ProjectileVisualEffectHelper() {
   }

   public static void captureTrace(List<Vec3> trace, Entity entity, int maxPoints) {
      Vec3 pos = entity.position();
      if (trace.isEmpty() || trace.get(trace.size() - 1).distanceToSqr(pos) >= 0.01) {
         trace.add(pos);
         while (trace.size() > maxPoints) {
            trace.remove(0);
         }
      }
   }

   public static void renderRibbonTrail(
      List<Vec3> trace, Vec3 currentPos, Vec3 cameraPos, PoseStack poseStack, MultiBufferSource buffer, ResourceLocation texture, float baseWidth, int packedColor, float alphaScale
   ) {
      if (trace.size() < 2) {
         return;
      }

      List<Vec3> points = new ArrayList<>(trace);
      points.add(currentPos);
      VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucentEmissive(texture));
      Pose pose = poseStack.last();
      int samplesPerSegment = 5;
      float textureRepeat = Math.max(1.0F, points.size() / 16.0F);
      float red = MagicCircuitColorHelper.red(packedColor);
      float green = MagicCircuitColorHelper.green(packedColor);
      float blue = MagicCircuitColorHelper.blue(packedColor);

      for (int i = 0; i < points.size() - 1; i++) {
         Vec3 p1 = points.get(i);
         Vec3 p2 = points.get(i + 1);
         Vec3 p0 = i > 0 ? points.get(i - 1) : p1.subtract(p2.subtract(p1));
         Vec3 p3 = i < points.size() - 2 ? points.get(i + 2) : p2.add(p2.subtract(p1));

         for (int j = 0; j < samplesPerSegment; j++) {
            float t1 = (float)j / samplesPerSegment;
            float t2 = (float)(j + 1) / samplesPerSegment;
            Vec3 start = catmullRom(t1, p0, p1, p2, p3);
            Vec3 end = catmullRom(t2, p0, p1, p2, p3);
            Vec3 startLocal = start.subtract(currentPos);
            Vec3 endLocal = end.subtract(currentPos);
            float total = (points.size() - 1) * samplesPerSegment;
            float gi1 = i * samplesPerSegment + j;
            float gi2 = gi1 + 1.0F;
            float pgr1 = gi1 / total;
            float pgr2 = gi2 / total;
            float width1 = baseWidth * (0.25F + 0.75F * (float)Math.sqrt(pgr1));
            float width2 = baseWidth * (0.25F + 0.75F * (float)Math.sqrt(pgr2));
            float alpha1 = alphaScale * (0.18F + 0.42F * pgr1 * pgr1);
            float alpha2 = alphaScale * (0.18F + 0.42F * pgr2 * pgr2);
            float u1 = pgr1 * textureRepeat;
            float u2 = pgr2 * textureRepeat;
            drawBillboardSegment(pose, consumer, startLocal, endLocal, cameraPos.subtract(currentPos), width1, width2, alpha1, alpha2, u1, u2, red, green, blue);
         }
      }
   }

   public static void renderResolveSlices(PoseStack poseStack, MultiBufferSource buffer, int packedColor, float scale, float progress, float time) {
      float alpha = (1.0F - progress) * 0.78F;
      if (alpha <= 0.01F) {
         return;
      }

      VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucentEmissive(CIRCUIT_TEXTURE));
      float red = MagicCircuitColorHelper.red(packedColor);
      float green = MagicCircuitColorHelper.green(packedColor);
      float blue = MagicCircuitColorHelper.blue(packedColor);
      float baseWidth = scale * (0.2F + 0.05F * (float)Math.sin(time * 0.35F));
      float baseHeight = scale * 1.45F;

      for (int i = 0; i < 4; i++) {
         poseStack.pushPose();
         poseStack.mulPose(Axis.ZP.rotationDegrees(45.0F * i + time * 1.6F));
         drawCenteredQuad(poseStack.last(), consumer, baseWidth, baseHeight, red, green, blue, alpha * (1.0F - i * 0.16F));
         poseStack.popPose();
      }
   }

   public static void renderHoverHalo(PoseStack poseStack, MultiBufferSource buffer, int packedColor, float radius, float time) {
      VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucentEmissive(CIRCUIT_TEXTURE));
      float red = MagicCircuitColorHelper.red(packedColor);
      float green = MagicCircuitColorHelper.green(packedColor);
      float blue = MagicCircuitColorHelper.blue(packedColor);
      poseStack.pushPose();
      poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
      drawCenteredQuad(poseStack.last(), consumer, radius * (1.0F + 0.08F * (float)Math.sin(time * 0.2F)), radius * (1.0F + 0.08F * (float)Math.sin(time * 0.2F)), red, green, blue, 0.42F);
      poseStack.popPose();
   }

    public static void renderCircuitRippleLayers(PoseStack poseStack, MultiBufferSource buffer, int packedColor, float radius, float alpha, float time) {
      if (radius <= 0.01F || alpha <= 0.01F) {
         return;
      }

      VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucentEmissive(CIRCUIT_TEXTURE));
      float red = MagicCircuitColorHelper.red(packedColor);
      float green = MagicCircuitColorHelper.green(packedColor);
      float blue = MagicCircuitColorHelper.blue(packedColor);
      float[] scales = new float[]{1.0F, 0.78F, 0.56F};

      for (int i = 0; i < scales.length; i++) {
         poseStack.pushPose();
         poseStack.mulPose(Axis.ZP.rotationDegrees(time * (1.8F + i * 0.6F) + i * 37.0F));
         float scaledRadius = radius * scales[i];
         drawCenteredQuad(poseStack.last(), consumer, scaledRadius, scaledRadius, red, green, blue, alpha * (0.34F - i * 0.06F));
         poseStack.popPose();
      }
   }

   private static Vec3 catmullRom(float t, Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3) {
      float t2 = t * t;
      float t3 = t2 * t;
      double x = 0.5 * (2.0 * p1.x + (-p0.x + p2.x) * t + (2.0 * p0.x - 5.0 * p1.x + 4.0 * p2.x - p3.x) * t2 + (-p0.x + 3.0 * p1.x - 3.0 * p2.x + p3.x) * t3);
      double y = 0.5 * (2.0 * p1.y + (-p0.y + p2.y) * t + (2.0 * p0.y - 5.0 * p1.y + 4.0 * p2.y - p3.y) * t2 + (-p0.y + 3.0 * p1.y - 3.0 * p2.y + p3.y) * t3);
      double z = 0.5 * (2.0 * p1.z + (-p0.z + p2.z) * t + (2.0 * p0.z - 5.0 * p1.z + 4.0 * p2.z - p3.z) * t2 + (-p0.z + 3.0 * p1.z - 3.0 * p2.z + p3.z) * t3);
      return new Vec3(x, y, z);
   }

   private static void drawBillboardSegment(
      Pose pose, VertexConsumer consumer, Vec3 start, Vec3 end, Vec3 viewOffset, float width1, float width2, float alpha1, float alpha2, float u1, float u2, float r, float g, float b
   ) {
      Vec3 dir = end.subtract(start);
      if (!(dir.lengthSqr() < 1.0E-6)) {
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
         Vec3 v0 = start.subtract(offset1);
         Vec3 v1 = start.add(offset1);
         Vec3 v2 = end.subtract(offset2);
         Vec3 v3 = end.add(offset2);
         vertex(pose, consumer, v0, r, g, b, alpha1, u1, 0.0F);
         vertex(pose, consumer, v2, r, g, b, alpha2, u2, 0.0F);
         vertex(pose, consumer, v3, r, g, b, alpha2, u2, 1.0F);
         vertex(pose, consumer, v1, r, g, b, alpha1, u1, 1.0F);
      }
   }

   public static void drawCenteredQuad(Pose pose, VertexConsumer consumer, float halfWidth, float halfHeight, float r, float g, float b, float a) {
      vertex(pose, consumer, new Vec3(-halfWidth, -halfHeight, 0.0), r, g, b, a, 0.0F, 0.0F);
      vertex(pose, consumer, new Vec3(halfWidth, -halfHeight, 0.0), r, g, b, a, 1.0F, 0.0F);
      vertex(pose, consumer, new Vec3(halfWidth, halfHeight, 0.0), r, g, b, a, 1.0F, 1.0F);
      vertex(pose, consumer, new Vec3(-halfWidth, halfHeight, 0.0), r, g, b, a, 0.0F, 1.0F);
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
