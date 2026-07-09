package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.ThompsonContenderItem;
import software.bernie.geckolib.model.GeoModel;

public class ThompsonContenderModel extends GeoModel<ThompsonContenderItem> {
   @Override
   public ResourceLocation getModelResource(ThompsonContenderItem object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "geo/thompson_contender.geo.json");
   }

   @Override
   public ResourceLocation getTextureResource(ThompsonContenderItem object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/item/thompson_contender.png");
   }

   @Override
   public ResourceLocation getAnimationResource(ThompsonContenderItem object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "animations/thompson_contender.animation.json");
   }
}
