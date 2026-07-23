package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ConceptSwordEntity;

public final class ConceptSwordRenderer extends EntityRenderer<ConceptSwordEntity> {
   private static final ResourceLocation TRAIL_TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/beacon_beam.png");

   public ConceptSwordRenderer(EntityRendererProvider.Context context) {
      super(context);
   }

   @Override
   public void render(ConceptSwordEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
      poseStack.pushPose();
      float yaw = entity.yRotO + (entity.getYRot() - entity.yRotO) * partialTicks;
      float pitch = entity.xRotO + (entity.getXRot() - entity.xRotO) * partialTicks;
      poseStack.mulPose(Axis.YP.rotationDegrees(90.0F + yaw));
      poseStack.mulPose(Axis.ZP.rotationDegrees(135.0F - pitch));
      poseStack.translate(-0.59, -0.59, 0.0);
      poseStack.scale(1.85F, 1.85F, 1.85F);
      Minecraft.getInstance().getItemRenderer().renderStatic(entity.getItem(), ItemDisplayContext.GROUND, 15728880,
         OverlayTexture.NO_OVERLAY, poseStack, buffer, entity.level(), entity.getId());
      poseStack.popPose();
      if (!entity.tracePos.isEmpty()) {
         ProjectileVisualEffectHelper.renderRibbonTrail(entity.tracePos, entity.getPosition(partialTicks),
            Minecraft.getInstance().getEntityRenderDispatcher().camera.getPosition(), poseStack, buffer,
            TRAIL_TEXTURE, 0.32F, 0xF01820, 0.9F);
      }
      super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
   }

   @Override
   public ResourceLocation getTextureLocation(ConceptSwordEntity entity) {
      return InventoryMenu.BLOCK_ATLAS;
   }
}
