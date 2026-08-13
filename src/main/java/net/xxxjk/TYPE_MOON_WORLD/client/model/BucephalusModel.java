package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.entity.BucephalusEntity;
import software.bernie.geckolib.model.GeoModel;

public final class BucephalusModel extends GeoModel<BucephalusEntity> {
   @Override
   public ResourceLocation getModelResource(BucephalusEntity animatable) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "geo/bucephalus.geo.json");
   }

   @Override
   public ResourceLocation getTextureResource(BucephalusEntity animatable) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/entity/bucephalus.png");
   }

   @Override
   public ResourceLocation getAnimationResource(BucephalusEntity animatable) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "animations/bucephalus.animation.json");
   }
}
