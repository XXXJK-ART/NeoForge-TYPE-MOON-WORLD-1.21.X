package com.example.typemoonaddon.client.renderer;

import com.example.typemoonaddon.TypeMoonAddon;
import com.example.typemoonaddon.client.model.ShadowFamiliarModel;
import com.example.typemoonaddon.entity.SakuraShadowFamiliarEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public final class ShadowFamiliarRenderer extends MobRenderer<SakuraShadowFamiliarEntity, ShadowFamiliarModel> {
    private static final ResourceLocation TEXTURE = TypeMoonAddon.id("textures/entity/shadow_familiar.png");

    public ShadowFamiliarRenderer(EntityRendererProvider.Context context) {
        super(context, new ShadowFamiliarModel(context.bakeLayer(ShadowFamiliarModel.LAYER_LOCATION)), 0.45F);
        this.addLayer(new ShadowFamiliarOutlineLayer(this, context.getModelSet()));
        this.addLayer(new ShadowFamiliarGlowLayer(this));
    }

    @Override
    public ResourceLocation getTextureLocation(SakuraShadowFamiliarEntity entity) {
        return TEXTURE;
    }

    @Override
    protected float getFlipDegrees(SakuraShadowFamiliarEntity entity) {
        return 0.0F;
    }
}
