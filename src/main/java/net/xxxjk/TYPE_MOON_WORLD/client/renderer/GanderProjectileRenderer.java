package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.entity.GanderProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.magic.nordic.MagicGander;

public class GanderProjectileRenderer extends EntityRenderer<GanderProjectileEntity> {
   private final ItemRenderer itemRenderer;
   private final float baseScale;
   private final boolean fullBright;
   private static final ResourceLocation TRAIL_TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/beacon_beam.png");

   public GanderProjectileRenderer(Context context) {
      this(context, 1.0F, true);
   }

   public GanderProjectileRenderer(Context context, float baseScale, boolean fullBright) {
      super(context);
      this.itemRenderer = context.getItemRenderer();
      this.baseScale = baseScale;
      this.fullBright = fullBright;
   }

   public void render(GanderProjectileEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
      float visualScale = entity.getVisualScale();
      float finalScale = this.baseScale * visualScale;
      poseStack.pushPose();
      if (entity.isChargingPreview()) {
         LivingEntity anchorSource = null;
         Entity owner = entity.getOwner();
         if (owner instanceof LivingEntity livingOwner) {
            anchorSource = livingOwner;
         }

         Minecraft minecraft = Minecraft.getInstance();
         if (minecraft.player != null && owner != null && owner.getId() == minecraft.player.getId()) {
            anchorSource = minecraft.player;
         }

         if (anchorSource != null) {
            Vec3 anchor = MagicGander.getChargeAnchor(anchorSource);
            Vec3 renderPos = entity.getPosition(partialTicks);
            Vec3 delta = anchor.subtract(renderPos);
            poseStack.translate(delta.x, delta.y, delta.z);
         }
      }

      if (GanderOrbShaderRegistry.isReady()) {
         this.renderOrb(entity, partialTicks, poseStack, buffer, finalScale);
      } else {
         poseStack.scale(finalScale, finalScale, finalScale);
         poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
         poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
         this.itemRenderer.renderStatic(entity.getItem(), net.minecraft.world.item.ItemDisplayContext.GROUND, packedLight, OverlayTexture.NO_OVERLAY, poseStack, buffer, entity.level(), entity.getId());
      }

      poseStack.popPose();
      this.renderTrail(entity, partialTicks, poseStack, buffer);
      super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
   }

   private void renderOrb(GanderProjectileEntity entity, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, float finalScale) {
      float time = entity.level().getGameTime() + partialTicks;
      if (GanderOrbShaderRegistry.getShader() != null && GanderOrbShaderRegistry.getShader().getUniform("Time") != null) {
         GanderOrbShaderRegistry.getShader().getUniform("Time").set(time * 0.08F);
      }

      float previewAlphaScale = entity.isChargingPreview() ? 0.72F : 1.0F;
      float pulse = 1.0F + 0.08F * (float)Math.sin(time * 0.55F);
      poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
      poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
      poseStack.mulPose(Axis.ZP.rotationDegrees(time * 3.5F));
      Pose pose = poseStack.last();
      VertexConsumer consumer = buffer.getBuffer(GanderOrbRenderType.orb());
      this.drawOrbQuad(pose, consumer, 0.16F * finalScale, 0.0F, 0.0F, 0.0F, 1.0F * previewAlphaScale);
      this.drawOrbQuad(pose, consumer, 0.25F * finalScale * (1.01F + 0.02F * (float)Math.cos(time * 0.8F)), 0.0F, 0.0F, 0.0F, 1.0F * previewAlphaScale);
      this.drawOrbQuad(pose, consumer, 0.34F * finalScale, 0.01F, 0.01F, 0.01F, 1.0F * previewAlphaScale);
      this.drawOrbQuad(pose, consumer, 0.44F * finalScale * pulse, 0.96F, 0.04F, 0.07F, 0.94F * previewAlphaScale);
   }

   private void drawOrbQuad(Pose pose, VertexConsumer consumer, float halfSize, float r, float g, float b, float a) {
      consumer.addVertex(pose, -halfSize, -halfSize, 0.0F).setColor(r, g, b, a).setUv(0.0F, 0.0F);
      consumer.addVertex(pose, halfSize, -halfSize, 0.0F).setColor(r, g, b, a).setUv(1.0F, 0.0F);
      consumer.addVertex(pose, halfSize, halfSize, 0.0F).setColor(r, g, b, a).setUv(1.0F, 1.0F);
      consumer.addVertex(pose, -halfSize, halfSize, 0.0F).setColor(r, g, b, a).setUv(0.0F, 1.0F);
   }

   private void renderTrail(GanderProjectileEntity entity, float partialTicks, PoseStack poseStack, MultiBufferSource buffer) {
      if (!entity.isChargingPreview()) {
         List<Vec3> trace = entity.tracePos;
         if (trace.size() >= 2) {
            Vec3 currentPos = entity.getPosition(partialTicks);
            List<Vec3> points = new ArrayList<>(trace);
            points.add(currentPos);
            if (points.size() >= 2) {
               VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucentEmissive(TRAIL_TEXTURE));
               Vec3 camPos = this.entityRenderDispatcher.camera.getPosition();
               Vec3 viewOffset = camPos.subtract(currentPos);
               poseStack.pushPose();
               Pose pose = poseStack.last();
               int samplesPerSegment = 5;
               float textureRepeat = Math.max(1.0F, points.size() / 18.0F);

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
                     float u1 = progress1 * textureRepeat;
                     float u2 = progress2 * textureRepeat;
                     float outerWidth1 = this.getWidth(0.22F, progress1);
                     float outerWidth2 = this.getWidth(0.22F, progress2);
                     float outerAlpha1 = this.getOuterAlpha(progress1);
                     float outerAlpha2 = this.getOuterAlpha(progress2);
                     float innerWidth1 = this.getWidth(0.12F, progress1);
                     float innerWidth2 = this.getWidth(0.12F, progress2);
                     float innerAlpha1 = this.getInnerAlpha(progress1);
                     float innerAlpha2 = this.getInnerAlpha(progress2);
                     this.drawBillboardSegment(
                        pose, consumer, startLocal, endLocal, viewOffset, outerWidth1, outerWidth2, outerAlpha1, outerAlpha2, u1, u2, 0.98F, 0.08F, 0.12F
                     );
                     this.drawBillboardSegment(
                        pose, consumer, startLocal, endLocal, viewOffset, innerWidth1, innerWidth2, innerAlpha1, innerAlpha2, u1, u2, 0.05F, 0.05F, 0.05F
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

   private float getOuterAlpha(float progress) {
      return 0.18F + 0.34F * progress * progress;
   }

   private float getInnerAlpha(float progress) {
      float p = progress * progress;
      return 0.26F + 0.44F * p * progress;
   }

   private void drawBillboardSegment(
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
      float r,
      float g,
      float b
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
         int light = 15728880;
         this.vertex(pose, consumer, v0, r, g, b, alpha1, u1, 0.0F, light);
         this.vertex(pose, consumer, v2, r, g, b, alpha2, u2, 0.0F, light);
         this.vertex(pose, consumer, v3, r, g, b, alpha2, u2, 1.0F, light);
         this.vertex(pose, consumer, v1, r, g, b, alpha1, u1, 1.0F, light);
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

   public ResourceLocation getTextureLocation(GanderProjectileEntity entity) {
      return TextureAtlas.LOCATION_BLOCKS;
   }
}
