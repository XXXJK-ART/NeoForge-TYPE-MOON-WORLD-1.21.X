package net.xxxjk.TYPE_MOON_WORLD.chain.client.model;

import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.chain.entity.EnumaChainCrownEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class EnumaChainCrownModel extends GeoModel<EnumaChainCrownEntity> {
    private static final ResourceLocation MODEL = TYPE_MOON_WORLD.id("geo/heaven_chain_head.geo.json");
    private static final ResourceLocation TEXTURE = TYPE_MOON_WORLD.id("textures/entity/heaven_chain_head.png");
    private static final ResourceLocation ANIMATION = TYPE_MOON_WORLD.id("animations/heaven_chain.animation.json");

    @Override
    public ResourceLocation getModelResource(EnumaChainCrownEntity entity) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(EnumaChainCrownEntity entity) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(EnumaChainCrownEntity entity) {
        return ANIMATION;
    }
}

