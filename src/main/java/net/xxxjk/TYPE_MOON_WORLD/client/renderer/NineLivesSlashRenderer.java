package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.entity.RubyProjectileEntity;

public final class NineLivesSlashRenderer {
   private static final ResourceLocation TEX = ResourceLocation.withDefaultNamespace("textures/entity/beacon_beam.png");

   public static void renderVisualSlash(ThrowableItemProjectile entity, float partialTicks, PoseStack poseStack, MultiBufferSource buffer) {
      if (entity instanceof RubyProjectileEntity ruby) {
         List<Vec3> trace = ruby.tracePos;
         if (trace.size() >= 2) {
            Level level = entity.level();
            Vec3 currentPos = entity.getPosition(partialTicks);
            Vec3 camPos = Minecraft.getInstance().getEntityRenderDispatcher().camera.getPosition();
            List<Vec3> points = new ArrayList<>(trace);
            if (points.size() >= 2) {
               points.add(currentPos);
               poseStack.pushPose();
               Pose pose = poseStack.last();
               VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucentEmissive(TEX));
               float baseWidth = 0.1F * ruby.getVisualScale();
               int samples = 5;
               float repeat = Math.max(1.0F, points.size() / 16.0F);

               for (int i = 0; i < points.size() - 1; i++) {
                  Vec3 p1 = points.get(i);
                  Vec3 p2 = points.get(i + 1);
                  Vec3 p0 = i > 0 ? points.get(i - 1) : p1.subtract(p2.subtract(p1));
                  Vec3 p3 = i < points.size() - 2 ? points.get(i + 2) : p2.add(p2.subtract(p1));

                  for (int j = 0; j < samples; j++) {
                     float t1 = (float)j / samples;
                     float t2 = (float)(j + 1) / samples;
                     Vec3 start = catmullRom(t1, p0, p1, p2, p3);
                     Vec3 end = catmullRom(t2, p0, p1, p2, p3);
                     float total = (points.size() - 1) * samples;
                     float gi1 = i * samples + j;
                     float gi2 = gi1 + 1.0F;
                     float pgr1 = gi1 / total;
                     float pgr2 = gi2 / total;
                     float w1 = visualWidth(baseWidth, pgr1);
                     float w2 = visualWidth(baseWidth, pgr2);
                     float a1 = visualAlpha(pgr1);
                     float a2 = visualAlpha(pgr2);
                     float u1 = pgr1 * repeat;
                     float u2 = pgr2 * repeat;
                     Vec3 startLocal = start.subtract(currentPos);
                     Vec3 endLocal = end.subtract(currentPos);
                     draw(level, pose, vc, startLocal, endLocal, camPos.subtract(currentPos), w1, w2, a1, a2, u1, u2, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F);
                  }
               }

               poseStack.popPose();
            }
         }
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

   private static float visualWidth(float base, float progress) {
      float s = Mth.sin((float) Math.PI * progress);
      return base * (0.4F + 0.6F * s);
   }

   private static float visualAlpha(float progress) {
      float s = Mth.sin((float) Math.PI * progress);
      return 0.6F + 0.4F * s;
   }

   private static void draw(
      Level level,
      Pose pose,
      VertexConsumer consumer,
      Vec3 start,
      Vec3 end,
      Vec3 viewOffset,
      float width1,
      float width2,
      float alpha1,
      float alpha2,
      float u1,
      float u2,
      float r1,
      float g1,
      float b1,
      float r2,
      float g2,
      float b2
   ) {
      Vec3 dir = end.subtract(start);
      if (!(dir.lengthSqr() < 1.0E-6)) {
         Vec3 viewDir = start.subtract(viewOffset);
         Vec3 cross = dir.cross(viewDir);
         if (!(cross.lengthSqr() < 1.0E-6)) {
            Vec3 right = cross.normalize();
            Vec3 offset1 = right.scale(width1 * 0.5);
            Vec3 offset2 = right.scale(width2 * 0.5);
            Vec3 v0 = start.subtract(offset1);
            Vec3 v1 = start.add(offset1);
            Vec3 v2 = end.subtract(offset2);
            Vec3 v3 = end.add(offset2);
            int lightStart = LightTexture.pack(15, 15);
            int lightEnd = LightTexture.pack(15, 15);
            vertex(pose, consumer, v0, r1, g1, b1, alpha1, u1, 0.0F, lightStart);
            vertex(pose, consumer, v2, r2, g2, b2, alpha2, u2, 0.0F, lightEnd);
            vertex(pose, consumer, v3, r2, g2, b2, alpha2, u2, 1.0F, lightEnd);
            vertex(pose, consumer, v1, r1, g1, b1, alpha1, u1, 1.0F, lightStart);
         }
      }
   }

   private static void vertex(Pose pose, VertexConsumer consumer, Vec3 pos, float r, float g, float b, float a, float u, float v, int packedLight) {
      consumer.addVertex(pose, (float)pos.x, (float)pos.y, (float)pos.z)
         .setColor(r, g, b, a)
         .setUv(u, v)
         .setOverlay(OverlayTexture.NO_OVERLAY)
         .setLight(packedLight)
         .setNormal(pose, 0.0F, 1.0F, 0.0F);
   }

   private static int packedLight(Level level, Vec3 pos) {
      BlockPos bp = BlockPos.containing(pos);
      int block = level.getBrightness(LightLayer.BLOCK, bp);
      int sky = level.getBrightness(LightLayer.SKY, bp);
      return LightTexture.pack(block, sky);
   }
}
