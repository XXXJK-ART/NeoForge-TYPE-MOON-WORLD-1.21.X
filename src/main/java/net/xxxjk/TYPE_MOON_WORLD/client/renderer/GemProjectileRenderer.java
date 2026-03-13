package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.entity.RubyProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.SapphireProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.TopazProjectileEntity;

public class GemProjectileRenderer<T extends ThrowableItemProjectile> extends EntityRenderer<T> {
   private final ItemRenderer itemRenderer;
   private final float scale;
   private static final ResourceLocation TRAIL_TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/end_gateway_beam.png");

   public GemProjectileRenderer(Context context, float r, float g, float b) {
      this(context, 1.0F, true, r, g, b);
   }

   public GemProjectileRenderer(Context context, float scale, boolean fullBright, float r, float g, float b) {
      super(context);
      this.itemRenderer = context.getItemRenderer();
      this.scale = scale;
   }

   public void render(T entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
      poseStack.pushPose();
      poseStack.scale(this.scale, this.scale, this.scale);
      poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
      poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
      this.itemRenderer
         .renderStatic(entity.getItem(), ItemDisplayContext.GROUND, packedLight, OverlayTexture.NO_OVERLAY, poseStack, buffer, entity.level(), entity.getId());
      poseStack.popPose();
      this.renderTrail(entity, partialTicks, poseStack, buffer);
      super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
   }

   private void renderTrail(T entity, float partialTicks, PoseStack poseStack, MultiBufferSource buffer) {
      Level level = entity.level();
      List<Vec3> trace;
      if (entity instanceof RubyProjectileEntity ruby) {
         trace = ruby.tracePos;
      } else if (entity instanceof SapphireProjectileEntity sapphire) {
         trace = sapphire.tracePos;
      } else {
         if (!(entity instanceof TopazProjectileEntity topaz)) {
            return;
         }

         trace = topaz.tracePos;
      }

      if (trace.size() >= 2) {
         if (entity instanceof RubyProjectileEntity ruby && ruby.getGemType() == 99) {
            NineLivesSlashRenderer.renderVisualSlash(entity, partialTicks, poseStack, buffer);
         } else {
            VertexConsumer vertexConsumer = buffer.getBuffer(
               RenderType.entityTranslucentEmissive(ResourceLocation.withDefaultNamespace("textures/entity/beacon_beam.png"))
            );
            float r = 1.0F;
            float g = 1.0F;
            float b = 1.0F;
            if (entity instanceof RubyProjectileEntity ruby) {
               int type = ruby.getGemType();
               if (type == 0) {
                  r = 1.0F;
                  g = 0.2F;
                  b = 0.2F;
               } else if (type == 1) {
                  r = 0.2F;
                  g = 0.2F;
                  b = 1.0F;
               } else if (type == 2) {
                  r = 0.2F;
                  g = 1.0F;
                  b = 0.2F;
               } else if (type == 3) {
                  r = 1.0F;
                  g = 1.0F;
                  b = 0.2F;
               } else if (type == 4) {
                  r = 0.0F;
                  g = 1.0F;
                  b = 1.0F;
               } else if (type == 5) {
                  r = 1.0F;
                  g = 1.0F;
                  b = 1.0F;
               } else if (type == 6) {
                  r = 0.15F;
                  g = 0.15F;
                  b = 0.15F;
               } else if (type == 99) {
                  r = 1.0F;
                  g = 1.0F;
                  b = 1.0F;
               } else {
                  r = 1.0F;
                  g = 1.0F;
                  b = 1.0F;
               }
            } else if (entity instanceof SapphireProjectileEntity) {
               r = 0.2F;
               g = 0.2F;
               b = 1.0F;
            } else if (entity instanceof TopazProjectileEntity) {
               r = 1.0F;
               g = 1.0F;
               b = 0.2F;
            }

            Vec3 currentPos = entity.getPosition(partialTicks);
            Vec3 camPos = this.entityRenderDispatcher.camera.getPosition();
            List<Vec3> points = new ArrayList<>(trace);
            if (points.size() >= 2) {
               points.add(currentPos);
               poseStack.pushPose();
               Pose pose = poseStack.last();
               float baseWidth = 0.3F;
               int samplesPerSegment = 5;
               float textureRepeat = Math.max(1.0F, points.size() / 16.0F);

               for (int i = 0; i < points.size() - 1; i++) {
                  Vec3 p1 = points.get(i);
                  Vec3 p2 = points.get(i + 1);
                  Vec3 p0 = i > 0 ? points.get(i - 1) : p1.subtract(p2.subtract(p1));
                  Vec3 p3 = i < points.size() - 2 ? points.get(i + 2) : p2.add(p2.subtract(p1));

                  for (int j = 0; j < samplesPerSegment; j++) {
                     float t1 = (float)j / samplesPerSegment;
                     float t2 = (float)(j + 1) / samplesPerSegment;
                     Vec3 start = this.catmullRom(t1, p0, p1, p2, p3);
                     Vec3 end = this.catmullRom(t2, p0, p1, p2, p3);
                     Vec3 startLocal = start.subtract(currentPos);
                     Vec3 endLocal = end.subtract(currentPos);
                     float totalPoints = (points.size() - 1) * samplesPerSegment;
                     float globalIndex1 = i * samplesPerSegment + j;
                     float globalIndex2 = globalIndex1 + 1.0F;
                     float progress1 = globalIndex1 / totalPoints;
                     float progress2 = globalIndex2 / totalPoints;
                     float width1 = this.getWidth(baseWidth, progress1);
                     float width2 = this.getWidth(baseWidth, progress2);
                     float alpha1 = this.getAlpha(progress1);
                     float alpha2 = this.getAlpha(progress2);
                     float u1 = progress1 * textureRepeat;
                     float u2 = progress2 * textureRepeat;
                     this.drawBillboardSegment(
                        level,
                        pose,
                        vertexConsumer,
                        startLocal,
                        endLocal,
                        camPos.subtract(currentPos),
                        width1,
                        width2,
                        alpha1,
                        alpha2,
                        u1,
                        u2,
                        r,
                        g,
                        b,
                        r,
                        g,
                        b
                     );
                  }
               }

               poseStack.popPose();
            }
         }
      }
   }

   private Vec3 catmullRom(float t, Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3) {
      float t2 = t * t;
      float t3 = t2 * t;
      double x = 0.5 * (2.0 * p1.x + (-p0.x + p2.x) * t + (2.0 * p0.x - 5.0 * p1.x + 4.0 * p2.x - p3.x) * t2 + (-p0.x + 3.0 * p1.x - 3.0 * p2.x + p3.x) * t3);
      double y = 0.5 * (2.0 * p1.y + (-p0.y + p2.y) * t + (2.0 * p0.y - 5.0 * p1.y + 4.0 * p2.y - p3.y) * t2 + (-p0.y + 3.0 * p1.y - 3.0 * p2.y + p3.y) * t3);
      double z = 0.5 * (2.0 * p1.z + (-p0.z + p2.z) * t + (2.0 * p0.z - 5.0 * p1.z + 4.0 * p2.z - p3.z) * t2 + (-p0.z + 3.0 * p1.z - 3.0 * p2.z + p3.z) * t3);
      return new Vec3(x, y, z);
   }

   private float getWidth(float base, float progress) {
      return base * (0.2F + 0.8F * (float)Math.sqrt(progress));
   }

   private float getAlpha(float progress) {
      return 0.2F + 0.3F * progress * progress;
   }

   private float getVisualWidth(float base, float progress) {
      float s = Mth.sin((float) Math.PI * progress);
      return base * (0.4F + 0.6F * s);
   }

   private float getVisualAlpha(float progress) {
      float s = Mth.sin((float) Math.PI * progress);
      return 0.6F + 0.4F * s;
   }

   private void drawBillboardSegment(
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
            int lightStart = 15728880;
            int lightEnd = 15728880;
            this.vertex(pose, consumer, v0, r1, g1, b1, alpha1, u1, 0.0F, lightStart);
            this.vertex(pose, consumer, v2, r2, g2, b2, alpha2, u2, 0.0F, lightEnd);
            this.vertex(pose, consumer, v3, r2, g2, b2, alpha2, u2, 1.0F, lightEnd);
            this.vertex(pose, consumer, v1, r1, g1, b1, alpha1, u1, 1.0F, lightStart);
         }
      }
   }

   private void vertex(Pose pose, VertexConsumer consumer, Vec3 pos, float r, float g, float b, float a, float u, float v, int packedLight) {
      consumer.addVertex(pose, (float)pos.x, (float)pos.y, (float)pos.z)
         .setColor(r, g, b, a)
         .setUv(u, v)
         .setOverlay(OverlayTexture.NO_OVERLAY)
         .setLight(packedLight)
         .setNormal(pose, 0.0F, 1.0F, 0.0F);
   }

   private int packedLight(Level level, Vec3 pos) {
      BlockPos bp = BlockPos.containing(pos);
      int block = level.getBrightness(LightLayer.BLOCK, bp);
      int sky = level.getBrightness(LightLayer.SKY, bp);
      return LightTexture.pack(block, sky);
   }

   public ResourceLocation getTextureLocation(T entity) {
      return TRAIL_TEXTURE;
   }
}
