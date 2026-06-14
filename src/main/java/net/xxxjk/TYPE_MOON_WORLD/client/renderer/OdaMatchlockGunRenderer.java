package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.xxxjk.TYPE_MOON_WORLD.client.model.OdaMatchlockGunModel;
import net.xxxjk.TYPE_MOON_WORLD.entity.OdaMatchlockGunEntity;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class OdaMatchlockGunRenderer extends GeoEntityRenderer<OdaMatchlockGunEntity> {
   public OdaMatchlockGunRenderer(Context renderManager) {
      super(renderManager, new OdaMatchlockGunModel());
      this.withScale(0.9F);
   }

   @Override
   public void render(OdaMatchlockGunEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
      poseStack.pushPose();
      float yaw = entity.yRotO + (entity.getYRot() - entity.yRotO) * partialTicks;
      float pitch = entity.xRotO + (entity.getXRot() - entity.xRotO) * partialTicks;
      poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
      poseStack.mulPose(Axis.XP.rotationDegrees(-pitch));
      poseStack.mulPose(Axis.ZP.rotationDegrees(entity.getAimPitch() * 0.04F));
      super.render(entity, entityYaw, partialTicks, poseStack, bufferSource, packedLight);
      poseStack.popPose();
   }
}
