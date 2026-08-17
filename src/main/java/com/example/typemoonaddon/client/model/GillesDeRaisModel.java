package com.example.typemoonaddon.client.model;

import com.example.typemoonaddon.TypeMoonAddon;
import com.example.typemoonaddon.entity.GillesDeRaisEntity;
import net.xxxjk.TYPE_MOON_WORLD.client.model.BaseServantModel;

public final class GillesDeRaisModel extends BaseServantModel<GillesDeRaisEntity> {
    public GillesDeRaisModel() {
        super(
                TypeMoonAddon.id("geo/gilles_de_rais_caster.geo.json"),
                TypeMoonAddon.id("textures/entity/gilles_de_rais_caster.png"),
                TypeMoonAddon.id("animations/gilles_de_rais_caster.animation.json")
        );
    }
}
