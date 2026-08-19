package com.example.typemoonaddon.client.model;

import com.example.typemoonaddon.TypeMoonAddon;
import com.example.typemoonaddon.item.CursedArmorRenderItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class GillesDeRaisArmorModel extends GeoModel<CursedArmorRenderItem> {
    private static final ResourceLocation MODEL = TypeMoonAddon.id("geo/gilles_de_rais_caster_armor.geo.json");
    private static final ResourceLocation TEXTURE = TypeMoonAddon.id("textures/armor/gilles_de_rais_caster_armor.png");
    private static final ResourceLocation ANIMATION = TypeMoonAddon.id("animations/gilles_de_rais_caster_armor.animation.json");

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
