package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.MuramasaBlockItem;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class MuramasaBlockItemRenderer extends GeoItemRenderer<MuramasaBlockItem> {
    public MuramasaBlockItemRenderer() {
        super(new MuramasaBlockItemModel());
    }
}

class MuramasaBlockItemModel extends GeoModel<MuramasaBlockItem> {
    @Override
    public ResourceLocation getModelResource(MuramasaBlockItem object) {
        return ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "geo/red_sword_block.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(MuramasaBlockItem object) {
        return ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "textures/block/red_sword_block.png");
    }

    @Override
    public ResourceLocation getAnimationResource(MuramasaBlockItem object) {
        return ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "animations/red_sword_block.animation.json");
    }
}
