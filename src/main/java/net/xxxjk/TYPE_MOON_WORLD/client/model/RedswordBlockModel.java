package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.block.entity.RedswordBlockEntity;
import software.bernie.geckolib.model.GeoModel;

public class RedswordBlockModel extends GeoModel<RedswordBlockEntity> {
    @Override
    public ResourceLocation getModelResource(RedswordBlockEntity object) {
        return ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "geo/red_sword_block.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(RedswordBlockEntity object) {
        return ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "textures/block/red_sword_block.png");
    }

    @Override
    public ResourceLocation getAnimationResource(RedswordBlockEntity object) {
        return ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "animations/red_sword_block.animation.json");
    }
}
