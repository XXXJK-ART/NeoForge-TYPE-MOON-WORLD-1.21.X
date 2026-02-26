
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
import net.xxxjk.TYPE_MOON_WORLD.block.custom.SwordBarrelBlock;
import net.xxxjk.TYPE_MOON_WORLD.block.entity.SwordBarrelBlockEntity;

public class SwordBarrelBlockEntityRenderer implements BlockEntityRenderer<SwordBarrelBlockEntity> {

    public SwordBarrelBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(SwordBarrelBlockEntity tileEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ItemStack storedItem = tileEntity.getStoredItem();

        if (!storedItem.isEmpty()) {
            poseStack.pushPose();

            // Translate to center
            poseStack.translate(0.5D, 0.5D, 0.5D);
            
            // Apply scale
            float scale = 0.75F;
            poseStack.scale(scale, scale, scale);

            // "瀹炰綋鍓戣鍙互鎸夌収瑙掑害鎵庡湪鍦颁笂" - Use captured projectile rotation
            float pitch = tileEntity.getCustomPitch();
            float yaw = tileEntity.getCustomYaw();
            
            // Revert to UBW standard rotation logic (Vertical or fixed angle)
            // User requested: "杩欎簺鍓戦洦鐢熸垚鐨勫墤涓嶉渶瑕佷互鍚勭瑙掑害鎻掑湪鍦颁笂锛屽氨鍙傝€僽bw鐨勫熀纭€鐨勪笉鍚岃搴?
            // UBWWeaponBlockEntityRenderer logic:
            
            // Determine rotation angle based on block direction
            switch (tileEntity.getBlockState().getValue(SwordBarrelBlock.FACING)) {
                case WEST:
                case EAST:
                    this.rotateItem(poseStack, 180f, 90f, -45f);
                    break;
                case NORTH:
                case SOUTH:
                    this.rotateItem(poseStack, 180f, 180f, -45f);
                    break;
                case UP:
                case DOWN:
                default:
                    // Default vertical planting like UBW?
                    // UBW logic seems to only handle horizontal facing?
                    // Let's stick to UBW logic for consistency.
                    // But UBWWeaponBlock only has HORIZONTAL_FACING.
                    // SwordBarrelBlock has FACING (6 directions).
                    // If UP/DOWN, maybe random horizontal rotation?
                    this.rotateItem(poseStack, 180f, 180f, -45f);
                    break;
            }

            // Further adjust angles based on seven extra tags (Random variation like UBW)
            float rotationX = 0f;
            float rotationY = 0f;
            float rotationZ = 0f;

            if (tileEntity.getBlockState().getValue(SwordBarrelBlock.ROTATION_A)) {
                rotationX += 11;
            }
            if (tileEntity.getBlockState().getValue(SwordBarrelBlock.ROTATION_B)) {
                rotationY += 13;
            }
            if (tileEntity.getBlockState().getValue(SwordBarrelBlock.ROTATION_C)) {
                rotationZ += 17;
            }
            if (tileEntity.getBlockState().getValue(SwordBarrelBlock.ROTATION_D)) {
                rotationX -= 19;
            }
            if (tileEntity.getBlockState().getValue(SwordBarrelBlock.ROTATION_E)) {
                rotationY -= 23;
            }
            if (tileEntity.getBlockState().getValue(SwordBarrelBlock.ROTATION_F)) {
                rotationZ -= 29;
            }
            if (tileEntity.getBlockState().getValue(SwordBarrelBlock.ROTATION_G)) {
                rotationX += 31;
            }

            // XYZ axis slight changes
            float translateX = 0f;
            float translateY = 0f;
            float translateZ = 0f;

            if (tileEntity.getBlockState().getValue(SwordBarrelBlock.ROTATION_A)) {
                translateX += 0.05f;
            }
            if (tileEntity.getBlockState().getValue(SwordBarrelBlock.ROTATION_B)) {
                translateY += 0.05f;
            }
            if (tileEntity.getBlockState().getValue(SwordBarrelBlock.ROTATION_C)) {
                translateZ += 0.05f;
            }
            if (tileEntity.getBlockState().getValue(SwordBarrelBlock.ROTATION_D)) {
                translateX -= 0.05f;
            }
            if (tileEntity.getBlockState().getValue(SwordBarrelBlock.ROTATION_E)) {
                translateY -= 0.05f;
            }
            if (tileEntity.getBlockState().getValue(SwordBarrelBlock.ROTATION_F)) {
                translateZ -= 0.05f;
            }
            if (tileEntity.getBlockState().getValue(SwordBarrelBlock.ROTATION_G)) {
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

            Minecraft.getInstance().getItemRenderer().renderStatic(
                    storedItem,
                    ItemDisplayContext.FIXED, // Use FIXED like UBW
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
