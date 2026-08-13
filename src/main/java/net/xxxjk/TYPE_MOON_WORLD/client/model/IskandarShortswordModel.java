package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.IskandarShortswordItem;
import software.bernie.geckolib.model.GeoModel;

public final class IskandarShortswordModel extends GeoModel<IskandarShortswordItem> {
   @Override
   public ResourceLocation getModelResource(IskandarShortswordItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "geo/iskandar_shortsword.geo.json");
   }

   @Override
   public ResourceLocation getTextureResource(IskandarShortswordItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/item/iskandar_shortsword.png");
   }

   @Override
   public ResourceLocation getAnimationResource(IskandarShortswordItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "animations/iskandar_shortsword.animation.json");
   }
}
