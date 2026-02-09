package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.TsumukariMuramasaItem;
import software.bernie.geckolib.model.GeoModel;

public class TsumukariMuramasaModel extends GeoModel<TsumukariMuramasaItem> {
    @Override
    public ResourceLocation getModelResource(TsumukariMuramasaItem object) {
        return ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "geo/red_sword.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(TsumukariMuramasaItem object) {
        return ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "textures/item/red.png");
    }

    @Override
    public ResourceLocation getAnimationResource(TsumukariMuramasaItem object) {
        return ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "animations/red_sword.animation.json");
    }
}
