package io.github.typemoonaddon.client.renderer;

import io.github.typemoonaddon.TypeMoonAddon;
import io.github.typemoonaddon.client.model.ShadowFamiliarModel;
import io.github.typemoonaddon.entity.ShadowFamiliarEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public final class ShadowFamiliarRenderer extends MobRenderer<ShadowFamiliarEntity, ShadowFamiliarModel> {
    private static final ResourceLocation TEXTURE = TypeMoonAddon.id("textures/entity/shadow_familiar.png");

    public ShadowFamiliarRenderer(EntityRendererProvider.Context context) {
        super(context, new ShadowFamiliarModel(context.bakeLayer(ShadowFamiliarModel.LAYER_LOCATION)), 0.45F);
        this.addLayer(new ShadowFamiliarOutlineLayer(this, context.getModelSet()));
        this.addLayer(new ShadowFamiliarGlowLayer(this));
    }

    @Override
    public ResourceLocation getTextureLocation(ShadowFamiliarEntity entity) {
        return TEXTURE;
    }

    @Override
    protected float getFlipDegrees(ShadowFamiliarEntity entity) {
        return 0.0F;
    }
}
