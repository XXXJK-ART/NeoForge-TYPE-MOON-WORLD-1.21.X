package net.xxxjk.TYPE_MOON_WORLD.chain.client.model;

import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.chain.entity.HeavenChainEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class HeavenChainModel extends GeoModel<HeavenChainEntity> {
    private static final ResourceLocation MODEL = TYPE_MOON_WORLD.id("geo/heaven_chain_head.geo.json");
    private static final ResourceLocation TEXTURE = TYPE_MOON_WORLD.id("textures/entity/heaven_chain_head.png");
    private static final ResourceLocation ANIMATION = TYPE_MOON_WORLD.id("animations/heaven_chain.animation.json");

    @Override
    public ResourceLocation getModelResource(HeavenChainEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(HeavenChainEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(HeavenChainEntity animatable) {
        return ANIMATION;
    }
}

