package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.AvalonItem;
import software.bernie.geckolib.model.GeoModel;

public class AvalonModel extends GeoModel<AvalonItem> {
    @Override
    public ResourceLocation getModelResource(AvalonItem object) {
        return ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "geo/avalon.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(AvalonItem object) {
        return ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "textures/item/avalon.png");
    }

    @Override
    public ResourceLocation getAnimationResource(AvalonItem object) {
        return ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "animations/avalon.animation.json");
    }
}
