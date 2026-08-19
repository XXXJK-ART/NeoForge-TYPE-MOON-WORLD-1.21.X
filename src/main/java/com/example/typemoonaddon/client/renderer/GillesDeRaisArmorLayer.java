package com.example.typemoonaddon.client.renderer;

import com.example.typemoonaddon.entity.GillesDeRaisEntity;
import com.example.typemoonaddon.registry.AddonItems;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

public final class GillesDeRaisArmorLayer extends RenderLayer<GillesDeRaisEntity, PlayerModel<GillesDeRaisEntity>> {
    private static final GillesDeRaisArmorRenderer RENDERER = new GillesDeRaisArmorRenderer();
    private static ItemStack renderStack;

    public GillesDeRaisArmorLayer(RenderLayerParent<GillesDeRaisEntity, PlayerModel<GillesDeRaisEntity>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, GillesDeRaisEntity entity,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        RENDERER.prepForRender(
                entity,
                renderStack(),
                EquipmentSlot.CHEST,
                getParentModel(),
                bufferSource,
                partialTick,
                limbSwing,
                limbSwingAmount,
                netHeadYaw,
                headPitch
        );
        RENDERER.setAllVisible(true);
        RenderType renderType = RENDERER.getRenderType(
                AddonItems.CURSED_ARMOR_RENDER.get(),
                RENDERER.getTextureLocation(AddonItems.CURSED_ARMOR_RENDER.get()),
                bufferSource,
                partialTick
        );
        RENDERER.renderToBuffer(
                poseStack,
                bufferSource.getBuffer(renderType),
                packedLight,
                OverlayTexture.NO_OVERLAY,
                0xFFFFFFFF
        );
    }

    private static ItemStack renderStack() {
        if (renderStack == null) {
            renderStack = new ItemStack(AddonItems.CURSED_ARMOR_RENDER.get());
        }
        return renderStack;
    }
}
