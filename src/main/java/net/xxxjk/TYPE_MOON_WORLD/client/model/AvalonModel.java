package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.AvalonItem;
import software.bernie.geckolib.model.GeoModel;

@SuppressWarnings("deprecation")
public class AvalonModel extends GeoModel<AvalonItem> {
   public ResourceLocation getModelResource(AvalonItem object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "geo/avalon.geo.json");
   }

   public ResourceLocation getTextureResource(AvalonItem object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/item/avalon.png");
   }

   public ResourceLocation getAnimationResource(AvalonItem object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "animations/avalon.animation.json");
   }
}
