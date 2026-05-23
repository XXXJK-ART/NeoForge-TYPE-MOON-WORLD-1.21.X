package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.BizenNagamitsuItem;
import software.bernie.geckolib.model.GeoModel;

@SuppressWarnings("deprecation")
public class BizenNagamitsuModel extends GeoModel<BizenNagamitsuItem> {
   public ResourceLocation getModelResource(BizenNagamitsuItem object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "geo/clothesline_pole.geo.json");
   }

   public ResourceLocation getTextureResource(BizenNagamitsuItem object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/item/clothesline_pole.png");
   }

   public ResourceLocation getAnimationResource(BizenNagamitsuItem object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "animations/clothesline_pole.animation.json");
   }
}
