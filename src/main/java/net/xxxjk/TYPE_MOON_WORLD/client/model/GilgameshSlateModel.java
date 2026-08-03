package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.GilgameshSlateItem;
import software.bernie.geckolib.model.GeoModel;

public final class GilgameshSlateModel extends GeoModel<GilgameshSlateItem> {
   @Override public ResourceLocation getModelResource(GilgameshSlateItem item) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "geo/gilgamesh_slate.geo.json");
   }
   @Override public ResourceLocation getTextureResource(GilgameshSlateItem item) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/item/gilgamesh_slate.png");
   }
   @Override public ResourceLocation getAnimationResource(GilgameshSlateItem item) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "animations/gilgamesh_slate.animation.json");
   }
}
