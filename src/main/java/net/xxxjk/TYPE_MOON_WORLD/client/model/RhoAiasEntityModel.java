package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.entity.RhoAiasEntity;
import software.bernie.geckolib.model.GeoModel;

public class RhoAiasEntityModel extends GeoModel<RhoAiasEntity> {
   @Override
   public ResourceLocation getModelResource(RhoAiasEntity object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "geo/rho_aias.geo.json");
   }

   @Override
   public ResourceLocation getTextureResource(RhoAiasEntity object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/item/rho_aias.png");
   }

   @Override
   public ResourceLocation getAnimationResource(RhoAiasEntity object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "animations/rho_aias.animation.json");
   }
}
