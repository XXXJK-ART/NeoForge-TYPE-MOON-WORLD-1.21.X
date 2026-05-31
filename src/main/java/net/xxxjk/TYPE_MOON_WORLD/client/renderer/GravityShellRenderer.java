package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.xxxjk.TYPE_MOON_WORLD.entity.GravityShellEffectEntity;

public class GravityShellRenderer extends EntityRenderer<GravityShellEffectEntity> {
   public GravityShellRenderer(Context context) {
      super(context);
   }

   @Override
   public void render(GravityShellEffectEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
      poseStack.pushPose();
      float alpha = entity.getAlphaStrength();
      float radiusXZ = entity.getRadiusXZ();
      float radiusY = entity.getRadiusY();
      if (TypeMoonEffectShaders.getGravityShell() != null && TypeMoonEffectShaders.getGravityShell().getUniform("Time") != null) {
         TypeMoonEffectShaders.getGravityShell().getUniform("Time").set((entity.tickCount + partialTicks) * 0.08F);
      }

      VertexConsumer consumer = buffer.getBuffer(GravityShellRenderType.shell());
      poseStack.translate(0.0F, -radiusY * 0.03F, 0.0F);
      GravityShellMeshHelper.drawUpperHemisphere(poseStack.last(), consumer, radiusXZ, radiusY, 0.09F, 0.03F, 0.14F, alpha);
      GravityShellMeshHelper.drawUpperHemisphere(poseStack.last(), consumer, radiusXZ * 0.9F, radiusY * 0.92F, 0.04F, 0.01F, 0.07F, alpha * 0.3F);
      poseStack.popPose();
      super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
   }

   @Override
   public ResourceLocation getTextureLocation(GravityShellEffectEntity entity) {
      return InventoryMenu.BLOCK_ATLAS;
   }
}
