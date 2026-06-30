package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.client.model.RhoAiasEntityModel;
import net.xxxjk.TYPE_MOON_WORLD.entity.RhoAiasEntity;
import org.jetbrains.annotations.Nullable;
import com.mojang.math.Axis;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class RhoAiasEntityRenderer extends GeoEntityRenderer<RhoAiasEntity> {
   public RhoAiasEntityRenderer(Context context) {
      super(context, new RhoAiasEntityModel());
      this.withScale(1.8F);
   }

   @Override
   public void render(RhoAiasEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
      poseStack.pushPose();
      poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
      super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
      poseStack.popPose();
   }

   @Override
   public RenderType getRenderType(
      RhoAiasEntity animatable,
      ResourceLocation texture,
      @Nullable MultiBufferSource bufferSource,
      float partialTick
   ) {
      return RenderType.entityTranslucent(texture);
   }
}
