package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.entity.BloodstainEffectEntity;

public class BloodstainEffectRenderer extends EntityRenderer<BloodstainEffectEntity> {
   private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "textures/entity/effect/bloodstain.png");

   public BloodstainEffectRenderer(Context context) {
      super(context);
   }

   @Override
   public void render(BloodstainEffectEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
      float alpha = entity.getCurrentAlpha(partialTicks);
      if (alpha <= 0.01F) {
         return;
      }

      VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucent(TEXTURE));
      poseStack.pushPose();
      poseStack.mulPose(Axis.YP.rotationDegrees(entity.getRotation()));
      poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
      float scale = entity.getScale();
      ProjectileVisualEffectHelper.drawCenteredQuad(poseStack.last(), consumer, scale, scale, 1.0F, 1.0F, 1.0F, alpha);
      poseStack.popPose();
      super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
   }

   @Override
   public ResourceLocation getTextureLocation(BloodstainEffectEntity entity) {
      return TEXTURE;
   }
}
