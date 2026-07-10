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
import net.xxxjk.TYPE_MOON_WORLD.entity.OdaMatchlockBulletEntity;

public class OdaMatchlockBulletRenderer extends EntityRenderer<OdaMatchlockBulletEntity> {
   private static final ResourceLocation ORB_TEXTURE = ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/models/armor/magic_circuit_item.png");
   private static final ResourceLocation TRAIL_TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/beacon_beam.png");

   public OdaMatchlockBulletRenderer(Context context) {
      super(context);
   }

   @Override
   public void render(OdaMatchlockBulletEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
      float time = entity.tickCount + partialTicks;
      poseStack.pushPose();
      poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
      poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
      poseStack.mulPose(Axis.ZP.rotationDegrees(time * 18.0F));
      VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucentEmissive(ORB_TEXTURE));
      float pulse = 1.0F + 0.12F * (float)Math.sin(time * 0.75F);
      float scale = entity.getVisualScale();
      drawQuad(poseStack.last(), consumer, 0.16F * pulse * scale, 1.0F, 0.78F, 0.24F, 0.95F);
      drawQuad(poseStack.last(), consumer, 0.09F * scale, 1.0F, 0.98F, 0.82F, 1.0F);
      poseStack.popPose();

      ProjectileVisualEffectHelper.renderRibbonTrail(
         entity.tracePos,
         entity.getPosition(partialTicks),
         this.entityRenderDispatcher.camera.getPosition(),
         poseStack,
         buffer,
         TRAIL_TEXTURE,
         0.11F * entity.getVisualScale(),
         0xFFE06A,
         0.72F
      );
      super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
   }

   private static void drawQuad(PoseStack.Pose pose, VertexConsumer consumer, float halfSize, float r, float g, float b, float a) {
      vertex(pose, consumer, -halfSize, -halfSize, r, g, b, a, 0.0F, 0.0F);
      vertex(pose, consumer, halfSize, -halfSize, r, g, b, a, 1.0F, 0.0F);
      vertex(pose, consumer, halfSize, halfSize, r, g, b, a, 1.0F, 1.0F);
      vertex(pose, consumer, -halfSize, halfSize, r, g, b, a, 0.0F, 1.0F);
   }

   private static void vertex(PoseStack.Pose pose, VertexConsumer consumer, float x, float y, float r, float g, float b, float a, float u, float v) {
      consumer.addVertex(pose, x, y, 0.0F)
         .setColor(r, g, b, a)
         .setUv(u, v)
         .setOverlay(OverlayTexture.NO_OVERLAY)
         .setLight(15728880)
         .setNormal(pose, 0.0F, 1.0F, 0.0F);
   }

   @Override
   public ResourceLocation getTextureLocation(OdaMatchlockBulletEntity entity) {
      return ORB_TEXTURE;
   }
}
