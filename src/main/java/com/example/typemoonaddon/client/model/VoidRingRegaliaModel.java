package com.example.typemoonaddon.client.model;

import com.example.typemoonaddon.TypeMoonAddon;
import com.example.typemoonaddon.item.VoidRingRegaliaItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class VoidRingRegaliaModel extends GeoModel<VoidRingRegaliaItem> {
    private static final ResourceLocation MODEL = TypeMoonAddon.id("geo/void_ring_regalia.geo.json");
    private static final ResourceLocation TEXTURE = TypeMoonAddon.id("textures/armor/void_ring_regalia.png");
    private static final ResourceLocation ANIMATION = TypeMoonAddon.id("animations/void_ring_regalia.animation.json");

    @Override
    public ResourceLocation getModelResource(VoidRingRegaliaItem item) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(VoidRingRegaliaItem item) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(VoidRingRegaliaItem item) {
        return ANIMATION;
    }
}
