package com.example.typemoonaddon.client.renderer;

import com.example.typemoonaddon.TypeMoonAddon;
import com.example.typemoonaddon.entity.SummonedSpiritEntity;
import net.minecraft.client.model.SilverfishModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public final class SummonedSpiritRenderer<T extends SummonedSpiritEntity>
        extends MobRenderer<T, SilverfishModel<T>> {
    private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/silverfish.png");

    public SummonedSpiritRenderer(EntityRendererProvider.Context context, float shadowRadius) {
        super(context, new SilverfishModel<>(context.bakeLayer(ModelLayers.SILVERFISH)), shadowRadius);
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return TEXTURE;
    }
}
