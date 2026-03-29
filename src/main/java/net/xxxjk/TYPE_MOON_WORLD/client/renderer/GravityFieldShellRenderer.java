package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.xxxjk.TYPE_MOON_WORLD.entity.GravityFieldShellEffectEntity;

public class GravityFieldShellRenderer extends EntityRenderer<GravityFieldShellEffectEntity> {
   public GravityFieldShellRenderer(Context context) {
      super(context);
   }

   @Override
   public void render(GravityFieldShellEffectEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
      float alpha = entity.getCurrentAlpha(partialTicks);
      if (alpha <= 0.01F) {
         return;
      }

      poseStack.pushPose();
      if (TypeMoonEffectShaders.getGravityShell() != null && TypeMoonEffectShaders.getGravityShell().getUniform("Time") != null) {
         TypeMoonEffectShaders.getGravityShell().getUniform("Time").set((entity.tickCount + partialTicks) * 0.08F);
      }

      float radiusXZ = entity.getRadiusXZ();
      float radiusY = entity.getRadiusY();
      VertexConsumer consumer = buffer.getBuffer(GravityShellRenderType.shell());
      poseStack.translate(0.0F, -0.02F, 0.0F);
      GravityShellMeshHelper.drawUpperHemisphere(poseStack.last(), consumer, radiusXZ, radiusY, 0.15F, 0.05F, 0.21F, alpha);
      GravityShellMeshHelper.drawUpperHemisphere(poseStack.last(), consumer, radiusXZ * 0.92F, radiusY * 0.94F, 0.07F, 0.02F, 0.11F, alpha * 0.36F);
      poseStack.popPose();
      super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
   }

   @Override
   public ResourceLocation getTextureLocation(GravityFieldShellEffectEntity entity) {
      return InventoryMenu.BLOCK_ATLAS;
   }
}
