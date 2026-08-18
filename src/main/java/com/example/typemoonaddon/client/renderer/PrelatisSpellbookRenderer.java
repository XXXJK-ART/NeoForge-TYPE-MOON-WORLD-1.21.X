package com.example.typemoonaddon.client.renderer;

import com.mojang.math.Axis;
import com.mojang.blaze3d.vertex.PoseStack;
import com.example.typemoonaddon.client.model.PrelatisSpellbookModel;
import com.example.typemoonaddon.item.PrelatisSpellbookItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.renderer.MultiBufferSource;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public final class PrelatisSpellbookRenderer extends GeoItemRenderer<PrelatisSpellbookItem> {
    public PrelatisSpellbookRenderer() {
        super(new PrelatisSpellbookModel());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack,
                             @Nullable MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (isHandDisplay(displayContext)) {
            poseStack.translate(0.0F, 0.08F, -0.02F);
            poseStack.scale(0.92F, 0.92F, 0.92F);
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(-78.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(10.0F));
        }
        super.renderByItem(stack, displayContext, poseStack, bufferSource, packedLight, packedOverlay);
    }

    private static boolean isHandDisplay(ItemDisplayContext displayContext) {
        return displayContext == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
            || displayContext == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
            || displayContext == ItemDisplayContext.THIRD_PERSON_LEFT_HAND
            || displayContext == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
    }
}
