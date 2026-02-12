package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.TempleStoneSwordAxeItem;
import software.bernie.geckolib.model.GeoModel;

public class TempleStoneSwordAxeModel extends GeoModel<TempleStoneSwordAxeItem> {
    @Override
    public ResourceLocation getModelResource(TempleStoneSwordAxeItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "geo/shoot_down_a_hundred_heads.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(TempleStoneSwordAxeItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "textures/item/shoot_down_a_hundred_heads.png");
    }

    @Override
    public ResourceLocation getAnimationResource(TempleStoneSwordAxeItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "animations/shoot_down_a_hundred_heads.animation.json");
    }
}
