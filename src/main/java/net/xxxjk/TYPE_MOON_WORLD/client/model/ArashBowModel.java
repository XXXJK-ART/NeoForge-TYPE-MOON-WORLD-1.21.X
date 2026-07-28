package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.ArashBowItem;
import software.bernie.geckolib.model.GeoModel;

public final class ArashBowModel extends GeoModel<ArashBowItem> {
   @Override public ResourceLocation getModelResource(ArashBowItem object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "geo/arash_bow.geo.json");
   }
   @Override public ResourceLocation getTextureResource(ArashBowItem object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/item/arash_bow.png");
   }
   @Override public ResourceLocation getAnimationResource(ArashBowItem object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "animations/arash_bow.animation.json");
   }
}
