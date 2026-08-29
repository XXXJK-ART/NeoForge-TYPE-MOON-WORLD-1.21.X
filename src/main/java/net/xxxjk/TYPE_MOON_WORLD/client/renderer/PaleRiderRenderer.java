package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.PaleRiderEntity;
import org.jetbrains.annotations.Nullable;

/**
 * Pale Rider intentionally has no authored body model.  Keep a stable vanilla
 * render carrier so the empty data model can never enter GeckoLib's renderer.
 * Its actual presence is represented by the horsemen, crows, particles and
 * domain effects spawned by the combat helper.
 */
public final class PaleRiderRenderer extends HumanoidMobRenderer<PaleRiderEntity, PlayerModel<PaleRiderEntity>> {
   private static final ResourceLocation EMPTY = ResourceLocation.fromNamespaceAndPath(
      TYPE_MOON_WORLD.MOD_ID, "textures/entity/empty.png");

   public PaleRiderRenderer(EntityRendererProvider.Context context) {
      super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.0F);
      this.addLayer(new HumanoidArmorLayer<>(this,
         new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
         new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)), context.getModelManager()));
   }

   @Override
   public ResourceLocation getTextureLocation(PaleRiderEntity entity) {
      return EMPTY;
   }

   @Override
   @Nullable
   protected RenderType getRenderType(PaleRiderEntity entity, boolean bodyVisible, boolean translucent, boolean glowing) {
      // Pale Rider has no humanoid body model; its presence is represented by
      // the horsemen, crows and domain effects.
      return null;
   }
}
