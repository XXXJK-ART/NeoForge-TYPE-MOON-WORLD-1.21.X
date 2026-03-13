package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.ExcaliburItem;
import software.bernie.geckolib.model.GeoModel;

@SuppressWarnings("deprecation")
public class ExcaliburModel extends GeoModel<ExcaliburItem> {
   public ResourceLocation getModelResource(ExcaliburItem object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "geo/excalibur.geo.json");
   }

   public ResourceLocation getTextureResource(ExcaliburItem object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/item/excalibur.png");
   }

   public ResourceLocation getAnimationResource(ExcaliburItem object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "animations/excalibur.animation.json");
   }
}
