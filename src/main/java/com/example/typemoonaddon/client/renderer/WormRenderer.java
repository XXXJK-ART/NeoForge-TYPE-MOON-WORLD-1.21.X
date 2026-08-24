package com.example.typemoonaddon.client.renderer;

import com.example.typemoonaddon.TypeMoonAddon;
import com.example.typemoonaddon.client.model.WingedWormModel;
import com.example.typemoonaddon.worm.WormEntity;
import com.example.typemoonaddon.worm.WormType;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.EndermiteModel;
import net.minecraft.client.model.SilverfishModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public final class WormRenderer extends MobRenderer<WormEntity, EntityModel<WormEntity>> {
    private static final ResourceLocation SILVERFISH_TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/silverfish.png");
    private static final ResourceLocation ENDERMITE_TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/endermite.png");
    private static final ResourceLocation WINGED_TEXTURE = TypeMoonAddon.id("textures/entity/winged_worm.png");

    private final SilverfishModel<WormEntity> silverfishModel;
    private final EndermiteModel<WormEntity> endermiteModel;
    private final WingedWormModel wingedModel;

    public WormRenderer(EntityRendererProvider.Context context) {
        super(context, new SilverfishModel<>(context.bakeLayer(ModelLayers.SILVERFISH)), 0.3F);
        this.silverfishModel = (SilverfishModel<WormEntity>) this.model;
        this.endermiteModel = new EndermiteModel<>(context.bakeLayer(ModelLayers.ENDERMITE));
        this.wingedModel = new WingedWormModel(context.bakeLayer(WingedWormModel.LAYER_LOCATION));
    }

    @Override
    public void render(WormEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        EntityModel<WormEntity> previous = this.model;
        this.model = switch (entity.getVariant()) {
            case WINGED -> this.wingedModel;
            case ENDERMITE -> this.endermiteModel;
            default -> this.silverfishModel;
        };
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
        this.model = previous;
    }

    @Override
    protected float getFlipDegrees(WormEntity entity) {
        return 180.0F;
    }

    @Override
    protected void scale(WormEntity entity, PoseStack poseStack, float partialTickTime) {
        if (entity.getVariant() == WormType.WINGED) {
            poseStack.scale(0.5F, 0.5F, 0.5F);
        }
    }

    @Override
    public ResourceLocation getTextureLocation(WormEntity entity) {
        return switch (entity.getVariant()) {
            case WINGED -> WINGED_TEXTURE;
            case ENDERMITE -> ENDERMITE_TEXTURE;
            default -> SILVERFISH_TEXTURE;
        };
    }
}
