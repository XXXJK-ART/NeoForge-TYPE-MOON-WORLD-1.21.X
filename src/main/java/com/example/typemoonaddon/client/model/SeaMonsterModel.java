package com.example.typemoonaddon.client.model;

import com.example.typemoonaddon.TypeMoonAddon;
import com.example.typemoonaddon.entity.SeaMonsterEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class SeaMonsterModel extends GeoModel<SeaMonsterEntity> {
    @Override
    public ResourceLocation getModelResource(SeaMonsterEntity animatable) {
        return TypeMoonAddon.id("geo/gilles_sea_monster.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(SeaMonsterEntity animatable) {
        return TypeMoonAddon.id("textures/entity/gilles_sea_monster.png");
    }

    @Override
    public ResourceLocation getAnimationResource(SeaMonsterEntity animatable) {
        return TypeMoonAddon.id("animations/gilles_sea_monster.animation.json");
    }
}
