package net.xxxjk.TYPE_MOON_WORLD.chain.client.model;

import net.xxxjk.TYPE_MOON_WORLD.chain.entity.HeavenChainBindingEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class HeavenChainBindingModel extends GeoModel<HeavenChainBindingEntity> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(
        "typemoonworld", "geo/chains_of_heaven.geo.json"
    );
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
        "typemoonworld", "textures/entity/chains_of_heaven_silver.png"
    );
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(
        "typemoonworld", "animations/chains_of_heaven.animation.json"
    );

    @Override
    public ResourceLocation getModelResource(HeavenChainBindingEntity entity) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(HeavenChainBindingEntity entity) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(HeavenChainBindingEntity entity) {
        return ANIMATION;
    }
}

