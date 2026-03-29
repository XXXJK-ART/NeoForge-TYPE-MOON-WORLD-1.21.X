package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.xxxjk.TYPE_MOON_WORLD.entity.TsumukariLightColumnEffectEntity;

public class TsumukariLightColumnEffectRenderer extends EntityRenderer<TsumukariLightColumnEffectEntity> {
   private static final ResourceLocation COLUMN_TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/beacon_beam.png");

   public TsumukariLightColumnEffectRenderer(Context context) {
      super(context);
   }

   @Override
   public void render(TsumukariLightColumnEffectEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
      float alpha = entity.getCurrentAlpha(partialTicks);
      if (alpha <= 0.01F) {
         return;
      }

      float height = entity.getHeightValue();
      float width = entity.getWidthValue();
      VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucentEmissive(COLUMN_TEXTURE));
      poseStack.pushPose();
      poseStack.translate(0.0F, height * 0.5F, 0.0F);
      for (int i = 0; i < 3; i++) {
         poseStack.pushPose();
         poseStack.mulPose(Axis.YP.rotationDegrees(60.0F * i));
         ProjectileVisualEffectHelper.drawCenteredQuad(poseStack.last(), consumer, width, height * 0.5F, 1.0F, 1.0F, 1.0F, alpha * (0.92F - i * 0.12F));
         ProjectileVisualEffectHelper.drawCenteredQuad(poseStack.last(), consumer, width * 0.45F, height * 0.5F, 1.0F, 1.0F, 1.0F, alpha);
         poseStack.popPose();
      }
      poseStack.popPose();
      super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
   }

   @Override
   public ResourceLocation getTextureLocation(TsumukariLightColumnEffectEntity entity) {
      return InventoryMenu.BLOCK_ATLAS;
   }
}
