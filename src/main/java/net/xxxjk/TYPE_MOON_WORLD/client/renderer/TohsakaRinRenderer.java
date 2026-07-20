package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.entity.TohsakaRinEntity;

public class TohsakaRinRenderer extends HumanoidMobRenderer<TohsakaRinEntity, PlayerModel<TohsakaRinEntity>> {
   private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/entity/tohsaka_rin.png");
   public TohsakaRinRenderer(EntityRendererProvider.Context context) {
      super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM), true), 0.5F);
   }
   @Override public ResourceLocation getTextureLocation(TohsakaRinEntity entity) { return TEXTURE; }
}
