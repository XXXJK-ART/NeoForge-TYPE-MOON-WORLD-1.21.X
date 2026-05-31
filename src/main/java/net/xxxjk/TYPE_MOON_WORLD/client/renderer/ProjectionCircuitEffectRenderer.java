package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.xxxjk.TYPE_MOON_WORLD.entity.ProjectionCircuitEffectEntity;
import net.xxxjk.TYPE_MOON_WORLD.magic.MagicCircuitColorHelper;

public class ProjectionCircuitEffectRenderer extends EntityRenderer<ProjectionCircuitEffectEntity> {
   public ProjectionCircuitEffectRenderer(Context context) {
      super(context);
   }

   @Override
   public void render(ProjectionCircuitEffectEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
      float radius = entity.getCurrentRadius(partialTicks);
      float alpha = entity.getCurrentAlpha(partialTicks);
      if (radius <= 0.01F || alpha <= 0.01F) {
         return;
      }

      if (TypeMoonEffectShaders.getUbwAnalysisRipple() != null && TypeMoonEffectShaders.getUbwAnalysisRipple().getUniform("Time") != null) {
         TypeMoonEffectShaders.getUbwAnalysisRipple().getUniform("Time").set((entity.tickCount + partialTicks) * 0.06F);
      }

      float r = MagicCircuitColorHelper.red(entity.getColor());
      float g = MagicCircuitColorHelper.green(entity.getColor());
      float b = MagicCircuitColorHelper.blue(entity.getColor());
      VertexConsumer consumer = buffer.getBuffer(UbwAnalysisRippleRenderType.ripple());
      poseStack.pushPose();
      poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
      ProjectileVisualEffectHelper.drawCenteredQuad(poseStack.last(), consumer, radius, radius, r, g, b, alpha);
      ProjectileVisualEffectHelper.drawCenteredQuad(poseStack.last(), consumer, radius * 0.65F, radius * 0.65F, r, g, b, alpha * 0.4F);
      for (int i = 0; i < 4; i++) {
         poseStack.pushPose();
         poseStack.mulPose(Axis.ZP.rotationDegrees(45.0F * i));
         ProjectileVisualEffectHelper.drawCenteredQuad(poseStack.last(), consumer, radius, Math.max(0.02F, radius * 0.05F), r, g, b, alpha * 0.24F);
         poseStack.popPose();
      }
      poseStack.popPose();
      super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
   }

   @Override
   public ResourceLocation getTextureLocation(ProjectionCircuitEffectEntity entity) {
      return InventoryMenu.BLOCK_ATLAS;
   }
}
