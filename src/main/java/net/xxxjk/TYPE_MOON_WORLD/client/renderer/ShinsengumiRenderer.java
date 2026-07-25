package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.entity.ShinsengumiEntity;

public class ShinsengumiRenderer extends HumanoidMobRenderer<ShinsengumiEntity, PlayerModel<ShinsengumiEntity>> {
   private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/entity/shinsengumi.png");
   public ShinsengumiRenderer(EntityRendererProvider.Context context) { super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F); }
   @Override public ResourceLocation getTextureLocation(ShinsengumiEntity entity) { return TEXTURE; }
}
