package io.github.typemoonaddon.client.model;

import io.github.typemoonaddon.TypeMoonAddon;
import io.github.typemoonaddon.item.CursedArmorRenderItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class CursedArmorModel extends GeoModel<CursedArmorRenderItem> {
    private static final ResourceLocation MODEL = TypeMoonAddon.id("geo/cursed_armor.geo.json");
    private static final ResourceLocation TEXTURE = TypeMoonAddon.id("textures/armor/cursed_armor.png");
    private static final ResourceLocation ANIMATION = TypeMoonAddon.id("animations/cursed_armor.animation.json");

    @Override
    public ResourceLocation getModelResource(CursedArmorRenderItem item) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(CursedArmorRenderItem item) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(CursedArmorRenderItem item) {
        return ANIMATION;
    }
}
