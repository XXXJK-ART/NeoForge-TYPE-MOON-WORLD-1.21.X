package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.xxxjk.TYPE_MOON_WORLD.block.custom.SwordBarrelBlock;
import net.xxxjk.TYPE_MOON_WORLD.block.entity.SwordBarrelBlockEntity;

public class SwordBarrelBlockEntityRenderer implements BlockEntityRenderer<SwordBarrelBlockEntity> {
   public SwordBarrelBlockEntityRenderer(Context context) {
   }

   public void render(
      SwordBarrelBlockEntity tileEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay
   ) {
      ItemStack storedItem = tileEntity.getStoredItem();
      if (!storedItem.isEmpty()) {
         poseStack.pushPose();
         poseStack.translate(0.5, 0.5, 0.5);
         float scale = 0.75F;
         poseStack.scale(scale, scale, scale);
         float pitch = tileEntity.getCustomPitch();
         float yaw = tileEntity.getCustomYaw();
         switch ((Direction)tileEntity.getBlockState().getValue(SwordBarrelBlock.FACING)) {
            case WEST:
            case EAST:
               this.rotateItem(poseStack, 180.0F, 90.0F, -45.0F);
               break;
            case NORTH:
            case SOUTH:
               this.rotateItem(poseStack, 180.0F, 180.0F, -45.0F);
               break;
            case UP:
            case DOWN:
            default:
               this.rotateItem(poseStack, 180.0F, 180.0F, -45.0F);
         }

         float rotationX = 0.0F;
         float rotationY = 0.0F;
         float rotationZ = 0.0F;
         if ((Boolean)tileEntity.getBlockState().getValue(SwordBarrelBlock.ROTATION_A)) {
            rotationX += 11.0F;
         }

         if ((Boolean)tileEntity.getBlockState().getValue(SwordBarrelBlock.ROTATION_B)) {
            rotationY += 13.0F;
         }

         if ((Boolean)tileEntity.getBlockState().getValue(SwordBarrelBlock.ROTATION_C)) {
            rotationZ += 17.0F;
         }

         if ((Boolean)tileEntity.getBlockState().getValue(SwordBarrelBlock.ROTATION_D)) {
            rotationX -= 19.0F;
         }

         if ((Boolean)tileEntity.getBlockState().getValue(SwordBarrelBlock.ROTATION_E)) {
            rotationY -= 23.0F;
         }

         if ((Boolean)tileEntity.getBlockState().getValue(SwordBarrelBlock.ROTATION_F)) {
            rotationZ -= 29.0F;
         }

         if ((Boolean)tileEntity.getBlockState().getValue(SwordBarrelBlock.ROTATION_G)) {
            rotationX += 31.0F;
         }

         float translateX = 0.0F;
         float translateY = 0.0F;
         float translateZ = 0.0F;
         if ((Boolean)tileEntity.getBlockState().getValue(SwordBarrelBlock.ROTATION_A)) {
            translateX += 0.05F;
         }

         if ((Boolean)tileEntity.getBlockState().getValue(SwordBarrelBlock.ROTATION_B)) {
            translateY += 0.05F;
         }

         if ((Boolean)tileEntity.getBlockState().getValue(SwordBarrelBlock.ROTATION_C)) {
            translateZ += 0.05F;
         }

         if ((Boolean)tileEntity.getBlockState().getValue(SwordBarrelBlock.ROTATION_D)) {
            translateX -= 0.05F;
         }

         if ((Boolean)tileEntity.getBlockState().getValue(SwordBarrelBlock.ROTATION_E)) {
            translateY -= 0.05F;
         }

         if ((Boolean)tileEntity.getBlockState().getValue(SwordBarrelBlock.ROTATION_F)) {
            translateZ -= 0.05F;
         }

         if ((Boolean)tileEntity.getBlockState().getValue(SwordBarrelBlock.ROTATION_G)) {
            translateX += 0.03F;
         }

         poseStack.translate(translateX, translateY, translateZ);
         poseStack.mulPose(Axis.XP.rotationDegrees(rotationX));
         poseStack.mulPose(Axis.YP.rotationDegrees(rotationY));
         poseStack.mulPose(Axis.ZP.rotationDegrees(rotationZ));
         poseStack.scale(1.3F, 1.3F, 1.3F);
         Minecraft.getInstance()
            .getItemRenderer()
            .renderStatic(storedItem, ItemDisplayContext.FIXED, packedLight, packedOverlay, poseStack, bufferSource, tileEntity.getLevel(), 0);
         poseStack.popPose();
      }
   }

   private void rotateItem(PoseStack poseStack, float x, float y, float z) {
      poseStack.mulPose(Axis.XP.rotationDegrees(x));
      poseStack.mulPose(Axis.YP.rotationDegrees(y));
      poseStack.mulPose(Axis.ZP.rotationDegrees(z));
   }
}
