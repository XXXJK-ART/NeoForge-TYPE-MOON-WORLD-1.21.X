package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.ExcaliburItem;
import software.bernie.geckolib.model.GeoModel;

public class ExcaliburModel extends GeoModel<ExcaliburItem> {
    @Override
    public ResourceLocation getModelResource(ExcaliburItem object) {
        return ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "geo/excalibur.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(ExcaliburItem object) {
        return ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "textures/item/excalibur.png");
    }

    @Override
    public ResourceLocation getAnimationResource(ExcaliburItem object) {
        return ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "animations/excalibur.animation.json");
    }
}
