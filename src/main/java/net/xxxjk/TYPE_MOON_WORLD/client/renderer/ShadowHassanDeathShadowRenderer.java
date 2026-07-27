package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ShadowHassanDeathShadowEntity;

public final class ShadowHassanDeathShadowRenderer extends HumanoidMobRenderer<ShadowHassanDeathShadowEntity, PlayerModel<ShadowHassanDeathShadowEntity>> {
   private static final ResourceLocation BLACK = ResourceLocation.withDefaultNamespace("textures/block/black_concrete.png");

   public ShadowHassanDeathShadowRenderer(EntityRendererProvider.Context context) {
      super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.0F);
   }

   @Override
   public ResourceLocation getTextureLocation(ShadowHassanDeathShadowEntity entity) {
      return BLACK;
   }
}
