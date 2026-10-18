package com.example.typemoonaddon.client.renderer;

import com.example.typemoonaddon.registry.AddonEntities;
import com.example.typemoonaddon.registry.AddonItems;
import com.example.typemoonaddon.worm.WormEntity;
import com.example.typemoonaddon.worm.WormStackData;
import com.example.typemoonaddon.worm.WormType;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Renders worm items with the same three-dimensional models used by live worms. */
public final class WormItemRenderer extends BlockEntityWithoutLevelRenderer {
    private WormEntity previewEntity;
    private Level previewLevel;

    public WormItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack,
                             MultiBufferSource buffer, int packedLight, int packedOverlay) {
        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        if (level == null) {
            return;
        }

        if (previewEntity == null || previewLevel != level) {
            previewEntity = AddonEntities.WORM.get().create(level);
            previewLevel = level;
        }
        if (previewEntity == null) {
            return;
        }

        WormType variant = variantFor(stack);
        previewEntity.setVariant(variant);
        previewEntity.setGuPower(WormStackData.gu(stack));
        previewEntity.setYRot(0.0F);
        previewEntity.setXRot(0.0F);
        previewEntity.yRotO = 0.0F;
        previewEntity.xRotO = 0.0F;
        previewEntity.yBodyRot = 0.0F;
        previewEntity.yBodyRotO = 0.0F;

        EntityRenderer<? super WormEntity> renderer = minecraft.getEntityRenderDispatcher().getRenderer(previewEntity);
        if (renderer == null) {
            return;
        }

        float scale = variant == WormType.WINGED ? 1.0F : 0.75F;
        if (displayContext == ItemDisplayContext.GUI || displayContext == ItemDisplayContext.FIXED) {
            scale *= 1.15F;
        }

        poseStack.pushPose();
        // ItemRenderer has already moved the item origin to the centre of its half-block.
        poseStack.translate(0.5F, -0.1F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(35.0F));
        poseStack.scale(scale, scale, scale);
        renderer.render(previewEntity, 0.0F, 0.0F, poseStack, buffer, packedLight);
        poseStack.popPose();
    }

    private static WormType variantFor(ItemStack stack) {
        if (stack.is(AddonItems.ENGRAVED_WORM.get())) {
            return WormType.SILVERFISH;
        }
        return WormStackData.type(stack);
    }
}
