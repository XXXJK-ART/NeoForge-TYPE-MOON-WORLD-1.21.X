package com.example.typemoonaddon.client.renderer;

import com.example.typemoonaddon.worm.WormEntity;
import net.minecraft.client.model.SilverfishModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public final class WormRenderer extends MobRenderer<WormEntity, SilverfishModel<WormEntity>> {
    private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/silverfish.png");

    public WormRenderer(EntityRendererProvider.Context context) {
        super(context, new SilverfishModel<>(context.bakeLayer(ModelLayers.SILVERFISH)), 0.3F);
    }

    @Override
    protected float getFlipDegrees(WormEntity entity) {
        return 180.0F;
    }

    @Override
    public ResourceLocation getTextureLocation(WormEntity entity) {
        return TEXTURE;
    }
}
