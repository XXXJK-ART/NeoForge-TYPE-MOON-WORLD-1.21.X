package com.example.typemoonaddon.client.model;

import com.example.typemoonaddon.TypeMoonAddon;
import com.example.typemoonaddon.entity.HugeSeaMonsterEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class HugeSeaMonsterModel extends GeoModel<HugeSeaMonsterEntity> {
    @Override
    public ResourceLocation getModelResource(HugeSeaMonsterEntity animatable) {
        return TypeMoonAddon.id("geo/gilles_huge_sea_monster.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(HugeSeaMonsterEntity animatable) {
        return TypeMoonAddon.id("textures/entity/gilles_huge_sea_monster.png");
    }

    @Override
    public ResourceLocation getAnimationResource(HugeSeaMonsterEntity animatable) {
        return TypeMoonAddon.id("animations/gilles_huge_sea_monster.animation.json");
    }
}
