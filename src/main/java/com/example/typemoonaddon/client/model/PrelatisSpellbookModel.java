package com.example.typemoonaddon.client.model;

import com.example.typemoonaddon.TypeMoonAddon;
import com.example.typemoonaddon.item.PrelatisSpellbookItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class PrelatisSpellbookModel extends GeoModel<PrelatisSpellbookItem> {
    private static final ResourceLocation MODEL = TypeMoonAddon.id("geo/prelatis_spellbook.geo.json");
    private static final ResourceLocation TEXTURE = TypeMoonAddon.id("textures/item/prelatis_spellbook.png");
    private static final ResourceLocation ANIMATION = TypeMoonAddon.id("animations/prelatis_spellbook.animation.json");

    @Override
    public ResourceLocation getModelResource(PrelatisSpellbookItem item) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(PrelatisSpellbookItem item) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(PrelatisSpellbookItem item) {
        return ANIMATION;
    }
}
