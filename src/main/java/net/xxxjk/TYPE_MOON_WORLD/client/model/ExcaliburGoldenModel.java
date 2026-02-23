package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.ExcaliburGoldenItem;
import software.bernie.geckolib.model.GeoModel;

public class ExcaliburGoldenModel extends GeoModel<ExcaliburGoldenItem> {
    @Override
    public ResourceLocation getModelResource(ExcaliburGoldenItem object) {
        return ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "geo/excalibur2.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(ExcaliburGoldenItem object) {
        return ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "textures/item/excalibur2.png");
    }

    @Override
    public ResourceLocation getAnimationResource(ExcaliburGoldenItem object) {
        return ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "animations/excalibur2.animation.json");
    }
}
