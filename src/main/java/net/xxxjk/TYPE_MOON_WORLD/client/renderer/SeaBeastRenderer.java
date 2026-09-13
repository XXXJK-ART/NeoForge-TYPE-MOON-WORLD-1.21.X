package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.client.model.SeaBeastWolfModel;
import net.xxxjk.TYPE_MOON_WORLD.entity.SeaBeastEntity;

/** Uses the vanilla wolf geometry, enlarged to three or four times its normal size. */
public final class SeaBeastRenderer extends MobRenderer<SeaBeastEntity, SeaBeastWolfModel> {
   private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/entity/sea_beast.png");

   public SeaBeastRenderer(EntityRendererProvider.Context context) {
      super(context, new SeaBeastWolfModel(context.bakeLayer(ModelLayers.WOLF)), 1.0F);
   }

   @Override
   protected void scale(SeaBeastEntity entity, PoseStack poseStack, float partialTickTime) {
      float scale = entity.getVisualScale();
      poseStack.scale(scale, scale, scale);
   }

   @Override
   protected float getShadowRadius(SeaBeastEntity entity) {
      // Keep the shadow proportional to the enlarged wolf mesh. The old x2
      // mesh used the renderer's 1.0 shadow radius as its baseline.
      return super.getShadowRadius(entity) * (entity.getVisualScale() * 0.5F);
   }

   @Override
   public ResourceLocation getTextureLocation(SeaBeastEntity entity) {
      return TEXTURE;
   }
}
