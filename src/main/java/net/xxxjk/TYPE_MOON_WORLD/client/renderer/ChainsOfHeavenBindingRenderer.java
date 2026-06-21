package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.xxxjk.TYPE_MOON_WORLD.client.model.ChainsOfHeavenBindingModel;
import net.xxxjk.TYPE_MOON_WORLD.entity.ChainsOfHeavenBindingEntity;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class ChainsOfHeavenBindingRenderer extends GeoEntityRenderer<ChainsOfHeavenBindingEntity> {
   public ChainsOfHeavenBindingRenderer(Context renderManager) {
      super(renderManager, new ChainsOfHeavenBindingModel());
      this.withScale(1.1F);
   }

   @Override
   public void render(ChainsOfHeavenBindingEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
      poseStack.pushPose();
      poseStack.scale(entity.getWidthScale(), entity.getHeightScale(), entity.getWidthScale());
      if (entity.isDivineBind()) {
         poseStack.scale(1.12F, 1.12F, 1.12F);
      }
      super.render(entity, entityYaw, partialTicks, poseStack, bufferSource, packedLight);
      poseStack.popPose();
   }
}
