package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.entity.RoninEntity;

public class RoninRenderer extends HumanoidMobRenderer<RoninEntity, PlayerModel<RoninEntity>> {
   private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/entity/ronin.png");
   public RoninRenderer(EntityRendererProvider.Context context) { super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F); }
   @Override public ResourceLocation getTextureLocation(RoninEntity entity) { return TEXTURE; }
}
