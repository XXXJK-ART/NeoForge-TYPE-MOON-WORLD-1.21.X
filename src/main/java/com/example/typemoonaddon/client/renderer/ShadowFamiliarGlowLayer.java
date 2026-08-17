package com.example.typemoonaddon.client.renderer;

import com.example.typemoonaddon.TypeMoonAddon;
import com.example.typemoonaddon.client.model.ShadowFamiliarModel;
import com.example.typemoonaddon.entity.SakuraShadowFamiliarEntity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EyesLayer;

final class ShadowFamiliarGlowLayer extends EyesLayer<SakuraShadowFamiliarEntity, ShadowFamiliarModel> {
    private static final RenderType GLOW = RenderType.eyes(
        TypeMoonAddon.id("textures/entity/shadow_familiar_glow.png")
    );

    ShadowFamiliarGlowLayer(RenderLayerParent<SakuraShadowFamiliarEntity, ShadowFamiliarModel> parent) {
        super(parent);
    }

    @Override
    public RenderType renderType() {
        return GLOW;
    }
}
