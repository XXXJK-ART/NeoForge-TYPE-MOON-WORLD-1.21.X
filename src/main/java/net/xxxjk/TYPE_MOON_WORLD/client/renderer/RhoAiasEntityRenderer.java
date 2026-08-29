package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.client.model.RhoAiasEntityModel;
import net.xxxjk.TYPE_MOON_WORLD.entity.RhoAiasEntity;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class RhoAiasEntityRenderer extends GeoEntityRenderer<RhoAiasEntity> {
   public RhoAiasEntityRenderer(Context context) {
      super(context, new RhoAiasEntityModel());
      this.withScale(1.8F);
   }

   @Override
   public void render(RhoAiasEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
      if (entity.isParticleOnly()) {
         return;
      }
      poseStack.pushPose();
      super.render(entity, 0.0F, partialTick, poseStack, bufferSource, packedLight);
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
