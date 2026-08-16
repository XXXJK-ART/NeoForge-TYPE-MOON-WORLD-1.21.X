package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.entity.LeffLaynorFlaurosEntity;

public class LeffLaynorFlaurosRenderer extends HumanoidMobRenderer<LeffLaynorFlaurosEntity, PlayerModel<LeffLaynorFlaurosEntity>> {
   private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/entity/leff_laynor_flauros.png");

   public LeffLaynorFlaurosRenderer(EntityRendererProvider.Context context) {
      super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
   }

   @Override
   public ResourceLocation getTextureLocation(LeffLaynorFlaurosEntity entity) {
      return TEXTURE;
   }
}
