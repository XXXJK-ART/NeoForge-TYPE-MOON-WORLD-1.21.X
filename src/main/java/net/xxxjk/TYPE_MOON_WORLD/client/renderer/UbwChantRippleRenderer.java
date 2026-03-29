package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.xxxjk.TYPE_MOON_WORLD.entity.UbwChantRippleEntity;
import net.xxxjk.TYPE_MOON_WORLD.magic.MagicCircuitColorHelper;

public class UbwChantRippleRenderer extends EntityRenderer<UbwChantRippleEntity> {
   public UbwChantRippleRenderer(Context context) {
      super(context);
   }

   @Override
   public void render(UbwChantRippleEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
      float radius = entity.getRadius();
      float alpha = entity.getAlphaStrength();
      if (radius <= 0.01F || alpha <= 0.01F) {
         return;
      }

      if (TypeMoonEffectShaders.getUbwAnalysisRipple() != null && TypeMoonEffectShaders.getUbwAnalysisRipple().getUniform("Time") != null) {
         TypeMoonEffectShaders.getUbwAnalysisRipple().getUniform("Time").set((entity.tickCount + partialTicks) * 0.05F);
      }

      float r = MagicCircuitColorHelper.red(entity.getColor());
      float g = MagicCircuitColorHelper.green(entity.getColor());
      float b = MagicCircuitColorHelper.blue(entity.getColor());
      VertexConsumer consumer = buffer.getBuffer(UbwAnalysisRippleRenderType.ripple());
      poseStack.pushPose();
      poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
      drawQuad(poseStack.last(), consumer, radius, radius, r, g, b, alpha * 0.55F);
      drawQuad(poseStack.last(), consumer, radius * 0.72F, radius * 0.72F, r, g, b, alpha * 0.45F);
      drawQuad(poseStack.last(), consumer, radius * 0.45F, radius * 0.45F, r, g, b, alpha * 0.35F);

      for (int i = 0; i < 6; i++) {
         poseStack.pushPose();
         poseStack.mulPose(Axis.ZP.rotationDegrees(30.0F * i));
         drawQuad(poseStack.last(), consumer, radius, Math.max(0.03F, radius * 0.035F), r, g, b, alpha * 0.25F);
         poseStack.popPose();
      }

      poseStack.popPose();
      super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
   }

   private static void drawQuad(Pose pose, VertexConsumer consumer, float halfWidth, float halfHeight, float r, float g, float b, float a) {
      consumer.addVertex(pose, -halfWidth, -halfHeight, 0.0F).setColor(r, g, b, a).setUv(0.0F, 0.0F);
      consumer.addVertex(pose,  halfWidth, -halfHeight, 0.0F).setColor(r, g, b, a).setUv(1.0F, 0.0F);
      consumer.addVertex(pose,  halfWidth,  halfHeight, 0.0F).setColor(r, g, b, a).setUv(1.0F, 1.0F);
      consumer.addVertex(pose, -halfWidth,  halfHeight, 0.0F).setColor(r, g, b, a).setUv(0.0F, 1.0F);
   }

   @Override
   public ResourceLocation getTextureLocation(UbwChantRippleEntity entity) {
      return InventoryMenu.BLOCK_ATLAS;
   }
}
