package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.entity.OkitaShinsengumiEntity;

public class OkitaShinsengumiRenderer extends HumanoidMobRenderer<OkitaShinsengumiEntity, PlayerModel<OkitaShinsengumiEntity>> {
   private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "textures/entity/shinsengumi.png");

   public OkitaShinsengumiRenderer(EntityRendererProvider.Context context) {
      super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
   }

   @Override
   public ResourceLocation getTextureLocation(OkitaShinsengumiEntity entity) {
      return TEXTURE;
   }
}
