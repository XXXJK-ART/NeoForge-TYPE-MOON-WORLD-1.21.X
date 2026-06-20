package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.entity.ChainsOfHeavenBindingEntity;
import software.bernie.geckolib.model.GeoModel;

public class ChainsOfHeavenBindingModel extends GeoModel<ChainsOfHeavenBindingEntity> {
   @Override
   public ResourceLocation getModelResource(ChainsOfHeavenBindingEntity object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "geo/chains_of_heaven.geo.json");
   }

   @Override
   public ResourceLocation getTextureResource(ChainsOfHeavenBindingEntity object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/entity/chains_of_heaven.png");
   }

   @Override
   public ResourceLocation getAnimationResource(ChainsOfHeavenBindingEntity object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "animations/chains_of_heaven.animation.json");
   }
}
