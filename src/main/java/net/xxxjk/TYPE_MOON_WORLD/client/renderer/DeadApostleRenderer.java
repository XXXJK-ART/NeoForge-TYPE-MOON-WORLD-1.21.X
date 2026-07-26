package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.entity.deadapostle.DeadApostleEntity;

public class DeadApostleRenderer<T extends DeadApostleEntity> extends HumanoidMobRenderer<T, PlayerModel<T>> {
   private final ResourceLocation texture;

   public DeadApostleRenderer(EntityRendererProvider.Context context, String textureName) {
      super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
      this.texture = ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/entity/" + textureName + ".png");
   }

   @Override
   public ResourceLocation getTextureLocation(T entity) {
      return texture;
   }
}
