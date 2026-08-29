package io.github.typemoonaddon.client.renderer;

import io.github.typemoonaddon.TypeMoonAddon;
import io.github.typemoonaddon.client.model.ShadowFamiliarModel;
import io.github.typemoonaddon.entity.ShadowFamiliarEntity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EyesLayer;

final class ShadowFamiliarGlowLayer extends EyesLayer<ShadowFamiliarEntity, ShadowFamiliarModel> {
    private static final RenderType GLOW = RenderType.eyes(
        TypeMoonAddon.id("textures/entity/shadow_familiar_glow.png")
    );

    ShadowFamiliarGlowLayer(RenderLayerParent<ShadowFamiliarEntity, ShadowFamiliarModel> parent) {
        super(parent);
    }

    @Override
    public RenderType renderType() {
        return GLOW;
    }
}
