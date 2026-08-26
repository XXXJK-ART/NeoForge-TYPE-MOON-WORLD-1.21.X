package io.github.typemoonaddon.client.renderer;

import io.github.typemoonaddon.data.ImaginarySpaceData.CursedArmorState;
import io.github.typemoonaddon.registry.ModAttachments;
import io.github.typemoonaddon.registry.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public final class CursedArmorLayer<T extends LivingEntity, M extends HumanoidModel<T>> extends RenderLayer<T, M> {
    private static final CursedArmorRenderer RENDERER = new CursedArmorRenderer();
    private static ItemStack renderStack;

    public CursedArmorLayer(RenderLayerParent<T, M> parent) {
        super(parent);
    }

    @Override
    public void render(
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        int packedLight,
        T entity,
        float limbSwing,
        float limbSwingAmount,
        float partialTick,
        float ageInTicks,
        float netHeadYaw,
        float headPitch
    ) {
        CursedArmorState state = entity.getData(ModAttachments.CURSED_ARMOR_VIEW.get()).state();
        if (state == CursedArmorState.NONE || state == CursedArmorState.REMOVED) {
            return;
        }
        RENDERER.clearFirstPersonArm();
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
        renderCurrent(poseStack, bufferSource, packedLight, partialTick);
    }

    public static void renderFirstPersonArm(
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        int packedLight,
        net.minecraft.client.player.AbstractClientPlayer player,
        net.minecraft.world.entity.HumanoidArm arm,
        HumanoidModel<?> model,
        float partialTick
    ) {
        if (!coversFirstPersonArm(arm)) {
            return;
        }
        CursedArmorState state = player.getData(ModAttachments.CURSED_ARMOR_VIEW.get()).state();
        if (state == CursedArmorState.NONE || state == CursedArmorState.REMOVED) {
            return;
        }
        RENDERER.setFirstPersonArm(arm);
        RENDERER.prepForRender(
            player,
            renderStack(),
            EquipmentSlot.CHEST,
            model,
            bufferSource,
            partialTick,
            0.0F,
            0.0F,
            0.0F,
            0.0F
        );
        renderCurrent(poseStack, bufferSource, packedLight, partialTick);
        RENDERER.clearFirstPersonArm();
    }

    public static boolean coversFirstPersonArm(net.minecraft.world.entity.HumanoidArm arm) {
        return arm == net.minecraft.world.entity.HumanoidArm.LEFT
            || arm == net.minecraft.world.entity.HumanoidArm.RIGHT;
    }

    private static void renderCurrent(
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        int packedLight,
        float partialTick
    ) {
        RenderType renderType = RENDERER.getRenderType(
            ModItems.CURSED_ARMOR_RENDER.get(),
            RENDERER.getTextureLocation(ModItems.CURSED_ARMOR_RENDER.get()),
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
            renderStack = new ItemStack(ModItems.CURSED_ARMOR_RENDER.get());
        }
        return renderStack;
    }
}
