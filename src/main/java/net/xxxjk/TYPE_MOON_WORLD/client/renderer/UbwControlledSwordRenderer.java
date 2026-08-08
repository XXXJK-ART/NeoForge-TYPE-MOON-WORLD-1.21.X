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
import net.minecraft.world.item.ItemStack;
import net.xxxjk.TYPE_MOON_WORLD.entity.UbwControlledSwordEntity;

public final class UbwControlledSwordRenderer extends EntityRenderer<UbwControlledSwordEntity> {
   public UbwControlledSwordRenderer(EntityRendererProvider.Context context) {
      super(context);
   }

   @Override
   public void render(UbwControlledSwordEntity entity, float yaw, float partialTick,
         PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
      ItemStack stack = entity.getItem();
      if (stack.isEmpty()) {
         return;
      }
      poseStack.pushPose();
      float interpolatedYaw = entity.yRotO + (entity.getYRot() - entity.yRotO) * partialTick;
      float interpolatedPitch = entity.xRotO + (entity.getXRot() - entity.xRotO) * partialTick;
      poseStack.mulPose(Axis.YP.rotationDegrees(90.0F + interpolatedYaw));
      poseStack.mulPose(Axis.ZP.rotationDegrees(135.0F - interpolatedPitch));
      poseStack.translate(-0.59D, -0.59D, 0.0D);
      poseStack.scale(1.75F, 1.75F, 1.75F);
      Minecraft.getInstance().getItemRenderer().renderStatic(
         stack, ItemDisplayContext.GROUND, packedLight, OverlayTexture.NO_OVERLAY,
         poseStack, buffers, entity.level(), entity.getId());
      poseStack.popPose();
      super.render(entity, yaw, partialTick, poseStack, buffers, packedLight);
   }

   @Override
   public ResourceLocation getTextureLocation(UbwControlledSwordEntity entity) {
      return InventoryMenu.BLOCK_ATLAS;
   }
}
