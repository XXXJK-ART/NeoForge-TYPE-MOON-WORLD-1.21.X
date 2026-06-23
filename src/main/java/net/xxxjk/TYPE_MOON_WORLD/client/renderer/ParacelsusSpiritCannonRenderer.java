package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.xxxjk.TYPE_MOON_WORLD.client.model.ParacelsusSpiritCannonModel;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ParacelsusSpiritCannonEntity;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class ParacelsusSpiritCannonRenderer extends GeoEntityRenderer<ParacelsusSpiritCannonEntity> {
   public ParacelsusSpiritCannonRenderer(Context renderManager) {
      super(renderManager, new ParacelsusSpiritCannonModel());
      this.withScale(0.72F);
   }

   @Override
   public void render(
      ParacelsusSpiritCannonEntity entity,
      float entityYaw,
      float partialTicks,
      PoseStack poseStack,
      MultiBufferSource bufferSource,
      int packedLight
   ) {
      poseStack.pushPose();
      float yaw = entity.yRotO + (entity.getYRot() - entity.yRotO) * partialTicks;
      float pitch = entity.xRotO + (entity.getXRot() - entity.xRotO) * partialTicks;
      poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
      poseStack.mulPose(Axis.XP.rotationDegrees(-pitch));
      poseStack.mulPose(Axis.ZP.rotationDegrees(entity.getAimYaw() * 0.02F));
      super.render(entity, entityYaw, partialTicks, poseStack, bufferSource, packedLight);
      poseStack.popPose();
   }
}
