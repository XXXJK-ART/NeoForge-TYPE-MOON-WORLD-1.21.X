package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.xxxjk.TYPE_MOON_WORLD.entity.ExpandingRingEffectEntity;
import net.xxxjk.TYPE_MOON_WORLD.magic.MagicCircuitColorHelper;

public class ExpandingRingEffectRenderer extends EntityRenderer<ExpandingRingEffectEntity> {
   public ExpandingRingEffectRenderer(Context context) {
      super(context);
   }

   @Override
   public void render(ExpandingRingEffectEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
      float radius = entity.getCurrentRadius(partialTicks);
      float alpha = entity.getCurrentAlpha(partialTicks);
      if (radius <= 0.01F || alpha <= 0.01F) {
         return;
      }

      if (TypeMoonEffectShaders.getExpandingRing() != null && TypeMoonEffectShaders.getExpandingRing().getUniform("Time") != null) {
         TypeMoonEffectShaders.getExpandingRing().getUniform("Time").set((entity.tickCount + partialTicks) * 0.06F);
      }

      float r = MagicCircuitColorHelper.red(entity.getColor());
      float g = MagicCircuitColorHelper.green(entity.getColor());
      float b = MagicCircuitColorHelper.blue(entity.getColor());
      VertexConsumer consumer = buffer.getBuffer(ExpandingRingRenderType.ring());
      poseStack.pushPose();
      poseStack.mulPose(Axis.YP.rotationDegrees(entity.getYawDegrees()));
      poseStack.mulPose(Axis.XP.rotationDegrees(90.0F + entity.getTiltDegrees()));
      ProjectileVisualEffectHelper.drawCenteredQuad(poseStack.last(), consumer, radius, radius, r, g, b, alpha);
      float inner = Math.max(0.08F, radius - entity.getThickness());
      ProjectileVisualEffectHelper.drawCenteredQuad(poseStack.last(), consumer, inner, inner, r, g, b, alpha * 0.45F);
      poseStack.popPose();
      super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
   }

   @Override
   public ResourceLocation getTextureLocation(ExpandingRingEffectEntity entity) {
      return InventoryMenu.BLOCK_ATLAS;
   }
}
