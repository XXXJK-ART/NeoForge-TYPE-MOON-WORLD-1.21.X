package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.block.entity.MuramasaBlockEntity;
import software.bernie.geckolib.model.GeoModel;

public class MuramasaBlockModel extends GeoModel<MuramasaBlockEntity> {
    @Override
    public ResourceLocation getModelResource(MuramasaBlockEntity object) {
        return ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "geo/red_sword_block.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(MuramasaBlockEntity object) {
        return ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "textures/block/red_sword_block.png");
    }

    @Override
    public ResourceLocation getAnimationResource(MuramasaBlockEntity object) {
        return ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "animations/red_sword_block.animation.json");
    }
}
