package com.example.typemoonaddon.client.renderer;

import com.example.typemoonaddon.client.model.CursedArmorModel;
import com.example.typemoonaddon.config.GameplayConfig;
import com.example.typemoonaddon.data.ImaginarySpaceData.CursedArmorState;
import com.example.typemoonaddon.item.CursedArmorRenderItem;
import com.example.typemoonaddon.registry.AddonAttachments;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public final class CursedArmorRenderer extends GeoArmorRenderer<CursedArmorRenderItem> {
    private HumanoidArm firstPersonArm;

    public CursedArmorRenderer() {
        super(new CursedArmorModel());
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }

    @Override
    protected void applyBoneVisibilityBySlot(net.minecraft.world.entity.EquipmentSlot slot) {
        if (firstPersonArm == null) {
            setAllBonesVisible(true);
            return;
        }
        setAllBonesVisible(false);
        setBoneVisible(firstPersonArm == HumanoidArm.RIGHT ? rightArm : leftArm, true);
    }

    public void setFirstPersonArm(HumanoidArm arm) {
        firstPersonArm = arm;
    }

    public void clearFirstPersonArm() {
        firstPersonArm = null;
    }

    @Override
    public void renderRecursively(
        PoseStack poseStack,
        CursedArmorRenderItem item,
        GeoBone bone,
        RenderType renderType,
        MultiBufferSource bufferSource,
        VertexConsumer buffer,
        boolean isReRender,
        float partialTick,
        int packedLight,
        int packedOverlay,
        int renderColor
    ) {
        if (bone.getName().startsWith("piece_")) {
            bone.setHidden(!pieceVisible(bone.getName(), partialTick));
        }
        super.renderRecursively(
            poseStack,
            item,
            bone,
            renderType,
            bufferSource,
            buffer,
            isReRender,
            partialTick,
            packedLight,
            packedOverlay,
            renderColor
        );
    }

    private boolean pieceVisible(String name, float partialTick) {
        Entity entity = getCurrentEntity();
        if (entity == null) {
            return false;
        }
        var view = entity.getData(AddonAttachments.CURSED_ARMOR_VIEW.get());
        CursedArmorState state = view.state();
        if (state == CursedArmorState.ACTIVE) {
            return true;
        }
        if (state != CursedArmorState.FORMING && state != CursedArmorState.DISSOLVING) {
            return false;
        }
        String[] parts = name.split("_");
        int index = parts.length > 1 ? parse(parts[1]) : 0;
        int centerY10 = parts.length > 2 ? parse(parts[2]) : 120;
        float elapsed = entity.level().getGameTime() + partialTick - view.stageStartTick();
        float progress = Mth.clamp(elapsed / GameplayConfig.CURSED_ARMOR_TRANSITION_TICKS, 0.0F, 1.0F);
        float noise = stableNoise(index);
        if (state == CursedArmorState.DISSOLVING) {
            return progress < 0.1F + noise * 0.9F;
        }
        float normalizedHeight = Mth.clamp((centerY10 - 20.0F) / 225.0F, 0.0F, 1.0F);
        float threshold = (1.0F - normalizedHeight) * 0.78F + noise * 0.22F;
        return progress >= threshold;
    }

    private static float stableNoise(int value) {
        int hash = value * 0x45d9f3b;
        hash = (hash ^ (hash >>> 16)) * 0x45d9f3b;
        hash ^= hash >>> 16;
        return (hash & 0x7FFFFFFF) / (float)Integer.MAX_VALUE;
    }

    private static int parse(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }
}
