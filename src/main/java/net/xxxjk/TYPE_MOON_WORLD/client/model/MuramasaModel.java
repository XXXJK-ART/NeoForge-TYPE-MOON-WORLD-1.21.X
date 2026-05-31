package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.MuramasaItem;
import software.bernie.geckolib.model.GeoModel;

@SuppressWarnings("deprecation")
public class MuramasaModel extends GeoModel<MuramasaItem> {
   public ResourceLocation getModelResource(MuramasaItem object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "geo/red_sword.geo.json");
   }

   public ResourceLocation getTextureResource(MuramasaItem object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/item/red.png");
   }

   public ResourceLocation getAnimationResource(MuramasaItem object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "animations/red_sword.animation.json");
   }
}
