package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.entity.GordiusWheelEntity;
import software.bernie.geckolib.model.GeoModel;

public final class GordiusWheelModel extends GeoModel<GordiusWheelEntity> {
   @Override
   public ResourceLocation getModelResource(GordiusWheelEntity animatable) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "geo/gordius_wheel.geo.json");
   }

   @Override
   public ResourceLocation getTextureResource(GordiusWheelEntity animatable) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/entity/gordius_wheel.png");
   }

   @Override
   public ResourceLocation getAnimationResource(GordiusWheelEntity animatable) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "animations/gordius_wheel.animation.json");
   }
}
