package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.entity.BajiquanMasterEntity;
import net.xxxjk.TYPE_MOON_WORLD.client.model.BajiquanPlayerModel;

public class BajiquanMasterRenderer extends HumanoidMobRenderer<BajiquanMasterEntity, PlayerModel<BajiquanMasterEntity>> {
   private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/entity/bajiquan_master.png");
   public BajiquanMasterRenderer(EntityRendererProvider.Context context) {
      super(context, new BajiquanPlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
   }
   @Override public ResourceLocation getTextureLocation(BajiquanMasterEntity entity) { return TEXTURE; }
}
