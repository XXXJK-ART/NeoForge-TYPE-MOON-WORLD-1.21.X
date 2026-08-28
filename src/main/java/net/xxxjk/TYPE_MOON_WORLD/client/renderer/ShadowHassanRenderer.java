package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ShadowHassanEntity;
import org.jetbrains.annotations.Nullable;

/** Steve's skeleton drives the mask; the base texture itself is fully transparent. */
public final class ShadowHassanRenderer extends HumanoidMobRenderer<ShadowHassanEntity, PlayerModel<ShadowHassanEntity>> {
   private static final ResourceLocation EMPTY = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "textures/entity/empty.png");

   public ShadowHassanRenderer(EntityRendererProvider.Context context) {
      super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.0F);
      this.addLayer(new HumanoidArmorLayer<>(this,
         new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
         new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)), context.getModelManager()));
   }

   @Override
   public void render(ShadowHassanEntity entity, float yaw, float partialTick, PoseStack poseStack,
                      MultiBufferSource buffers, int packedLight) {
      if (entity.isPresenceConcealed()) return;
      super.render(entity, yaw, partialTick, poseStack, buffers, packedLight);
   }

   @Override
   public ResourceLocation getTextureLocation(ShadowHassanEntity entity) {
      return EMPTY;
   }

   @Override
   @Nullable
   protected RenderType getRenderType(ShadowHassanEntity entity, boolean bodyVisible, boolean translucent, boolean glowing) {
      // The Hassan NPC is represented by its head mask only.  Do not submit
      // the transparent carrier model, otherwise its humanoid body can show
      // through depending on the active render type.
      return null;
   }
}
