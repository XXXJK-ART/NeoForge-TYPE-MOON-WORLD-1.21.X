package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.RedswordItem;
import software.bernie.geckolib.model.GeoModel;

public class RedswordModel extends GeoModel<RedswordItem> {
    @Override
    public ResourceLocation getModelResource(RedswordItem object) {
        return ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "geo/red_sword.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(RedswordItem object) {
        return ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "textures/item/red.png");
    }

    @Override
    public ResourceLocation getAnimationResource(RedswordItem object) {
        return ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "animations/red_sword.animation.json");
    }
}
