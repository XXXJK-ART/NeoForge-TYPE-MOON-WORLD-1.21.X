package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.ExcaliburGoldenItem;
import software.bernie.geckolib.model.GeoModel;

@SuppressWarnings("deprecation")
public class ExcaliburGoldenModel extends GeoModel<ExcaliburGoldenItem> {
   public ResourceLocation getModelResource(ExcaliburGoldenItem object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "geo/excalibur2.geo.json");
   }

   public ResourceLocation getTextureResource(ExcaliburGoldenItem object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/item/excalibur2.png");
   }

   public ResourceLocation getAnimationResource(ExcaliburGoldenItem object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "animations/excalibur2.animation.json");
   }
}
