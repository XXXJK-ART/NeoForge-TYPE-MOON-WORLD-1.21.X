
package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import net.minecraft.core.Direction;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.xxxjk.TYPE_MOON_WORLD.block.custom.UBWWeaponBlock;
import net.xxxjk.TYPE_MOON_WORLD.block.entity.UBWWeaponBlockEntity;

public class UBWWeaponBlockEntityRenderer implements BlockEntityRenderer<UBWWeaponBlockEntity> {

    public UBWWeaponBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(UBWWeaponBlockEntity tileEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ItemStack storedItem = tileEntity.getStoredItem();

        if (!storedItem.isEmpty()) {
            poseStack.pushPose();

            // Translate to center and move down to insert into ground
            poseStack.translate(0.5D, 0.2D, 0.5D);

            // Determine rotation angle based on block direction
            switch (tileEntity.getBlockState().getValue(UBWWeaponBlock.FACING)) {
                case WEST:
                case EAST:
                    this.rotateItem(poseStack, 180f, 90f, -45f);
                    break;
                case NORTH:
                case SOUTH:
                    this.rotateItem(poseStack, 180f, 180f, -45f);
                    break;
            }

            // Further adjust angles based on seven extra tags
            float rotationX = 0f;
            float rotationY = 0f;
            float rotationZ = 0f;

            if (tileEntity.getBlockState().getValue(UBWWeaponBlock.ROTATION_A)) {
                rotationX += 11;
            }
            if (tileEntity.getBlockState().getValue(UBWWeaponBlock.ROTATION_B)) {
                rotationY += 13;
            }
            if (tileEntity.getBlockState().getValue(UBWWeaponBlock.ROTATION_C)) {
                rotationZ += 17;
            }
            if (tileEntity.getBlockState().getValue(UBWWeaponBlock.ROTATION_D)) {
                rotationX -= 19;
            }
            if (tileEntity.getBlockState().getValue(UBWWeaponBlock.ROTATION_E)) {
                rotationY -= 23;
            }
            if (tileEntity.getBlockState().getValue(UBWWeaponBlock.ROTATION_F)) {
                rotationZ -= 29;
            }
            if (tileEntity.getBlockState().getValue(UBWWeaponBlock.ROTATION_G)) {
                rotationX += 31;
            }

            // XYZ axis slight changes
            float translateX = 0f;
            float translateY = 0f;
            float translateZ = 0f;

            if (tileEntity.getBlockState().getValue(UBWWeaponBlock.ROTATION_A)) {
                translateX += 0.05f;
            }
            if (tileEntity.getBlockState().getValue(UBWWeaponBlock.ROTATION_B)) {
                translateY += 0.05f;
            }
            if (tileEntity.getBlockState().getValue(UBWWeaponBlock.ROTATION_C)) {
                translateZ += 0.05f;
            }
            if (tileEntity.getBlockState().getValue(UBWWeaponBlock.ROTATION_D)) {
                translateX -= 0.05f;
            }
            if (tileEntity.getBlockState().getValue(UBWWeaponBlock.ROTATION_E)) {
                translateY -= 0.05f;
            }
            if (tileEntity.getBlockState().getValue(UBWWeaponBlock.ROTATION_F)) {
                translateZ -= 0.05f;
            }
            if (tileEntity.getBlockState().getValue(UBWWeaponBlock.ROTATION_G)) {
                translateX += 0.03f;
            }

            // Apply position offset
            poseStack.translate(translateX, translateY, translateZ);

            // Apply additional random rotation angles
            poseStack.mulPose(Axis.XP.rotationDegrees(rotationX));
            poseStack.mulPose(Axis.YP.rotationDegrees(rotationY));
            poseStack.mulPose(Axis.ZP.rotationDegrees(rotationZ));

            // Scale the item
            poseStack.scale(1.3F, 1.3F, 1.3F);

            // Render the item
            Minecraft.getInstance().getItemRenderer().renderStatic(
                    storedItem,
                    ItemDisplayContext.FIXED,
                    packedLight,
                    packedOverlay,
                    poseStack,
                    bufferSource,
                    tileEntity.getLevel(),
                    0
            );

            poseStack.popPose();
        }
    }

    private void rotateItem(PoseStack poseStack, float x, float y, float z) {
        poseStack.mulPose(Axis.XP.rotationDegrees(x));
        poseStack.mulPose(Axis.YP.rotationDegrees(y));
        poseStack.mulPose(Axis.ZP.rotationDegrees(z));
    }
}
