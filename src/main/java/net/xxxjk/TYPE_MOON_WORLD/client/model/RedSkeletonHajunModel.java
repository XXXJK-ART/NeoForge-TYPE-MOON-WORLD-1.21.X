package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.entity.RedSkeletonHajunEntity;
import software.bernie.geckolib.model.GeoModel;

public class RedSkeletonHajunModel extends GeoModel<RedSkeletonHajunEntity> {
   @Override
   public ResourceLocation getModelResource(RedSkeletonHajunEntity object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "geo/red_skeleton.geo.json");
   }

   @Override
   public ResourceLocation getTextureResource(RedSkeletonHajunEntity object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/entity/red_skeleton.png");
   }

   @Override
   public ResourceLocation getAnimationResource(RedSkeletonHajunEntity object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "animations/red_skeleton.animation.json");
   }
}
