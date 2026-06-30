package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.util.Mth;
import net.xxxjk.TYPE_MOON_WORLD.client.model.RedSkeletonHajunModel;
import net.xxxjk.TYPE_MOON_WORLD.entity.RedSkeletonHajunEntity;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class RedSkeletonHajunRenderer extends GeoEntityRenderer<RedSkeletonHajunEntity> {
   public RedSkeletonHajunRenderer(Context renderManager) {
      super(renderManager, new RedSkeletonHajunModel());
      this.withScale(1.6F);
   }

   @Override
   public void render(RedSkeletonHajunEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
      poseStack.pushPose();
      float yaw = Mth.rotLerp(partialTicks, entity.yRotO, entity.getYRot());
      poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
      super.render(entity, entityYaw, partialTicks, poseStack, bufferSource, packedLight);
      poseStack.popPose();
   }
}
