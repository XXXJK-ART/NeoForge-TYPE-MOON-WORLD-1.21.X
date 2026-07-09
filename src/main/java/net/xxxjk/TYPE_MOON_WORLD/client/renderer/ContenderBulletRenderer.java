package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.entity.ContenderBulletEntity;

public class ContenderBulletRenderer extends EntityRenderer<ContenderBulletEntity> {
   private static final ResourceLocation BULLET_TEXTURE = ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/item/bullet.png");
   private static final ResourceLocation TRAIL_TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/beacon_beam.png");

   public ContenderBulletRenderer(Context context) {
      super(context);
   }

   @Override
   public void render(ContenderBulletEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
      poseStack.pushPose();
      poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
      poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
      VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucentEmissive(BULLET_TEXTURE));
      float size = entity.isOriginBullet() ? 0.13F : 0.09F;
      float r = entity.isOriginBullet() ? 0.8F : 1.0F;
      float g = entity.isOriginBullet() ? 0.25F : 0.9F;
      float b = entity.isOriginBullet() ? 0.12F : 0.55F;
      drawQuad(poseStack.last(), consumer, size, r, g, b);
      poseStack.popPose();

      ProjectileVisualEffectHelper.renderRibbonTrail(
         entity.tracePos,
         entity.getPosition(partialTicks),
         this.entityRenderDispatcher.camera.getPosition(),
         poseStack,
         buffer,
         TRAIL_TEXTURE,
         entity.isOriginBullet() ? 0.075F : 0.045F,
         entity.isOriginBullet() ? 0xDD2211 : 0xFFD66A,
         entity.isOriginBullet() ? 0.78F : 0.48F
      );
      super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
   }

   private static void drawQuad(PoseStack.Pose pose, VertexConsumer consumer, float halfSize, float r, float g, float b) {
      vertex(pose, consumer, -halfSize, -halfSize, r, g, b, 0.0F, 0.0F);
      vertex(pose, consumer, halfSize, -halfSize, r, g, b, 1.0F, 0.0F);
      vertex(pose, consumer, halfSize, halfSize, r, g, b, 1.0F, 1.0F);
      vertex(pose, consumer, -halfSize, halfSize, r, g, b, 0.0F, 1.0F);
   }

   private static void vertex(PoseStack.Pose pose, VertexConsumer consumer, float x, float y, float r, float g, float b, float u, float v) {
      consumer.addVertex(pose, x, y, 0.0F)
         .setColor(r, g, b, 0.95F)
         .setUv(u, v)
         .setOverlay(OverlayTexture.NO_OVERLAY)
         .setLight(15728880)
         .setNormal(pose, 0.0F, 1.0F, 0.0F);
   }

   @Override
   public ResourceLocation getTextureLocation(ContenderBulletEntity entity) {
      return BULLET_TEXTURE;
   }
}
