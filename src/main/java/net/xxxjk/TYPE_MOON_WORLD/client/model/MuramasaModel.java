package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.MuramasaItem;
import software.bernie.geckolib.model.GeoModel;

public class MuramasaModel extends GeoModel<MuramasaItem> {
    @Override
    public ResourceLocation getModelResource(MuramasaItem object) {
        return ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "geo/red_sword.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(MuramasaItem object) {
        return ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "textures/item/red.png");
    }

    @Override
    public ResourceLocation getAnimationResource(MuramasaItem object) {
        return ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "animations/red_sword.animation.json");
    }
}
