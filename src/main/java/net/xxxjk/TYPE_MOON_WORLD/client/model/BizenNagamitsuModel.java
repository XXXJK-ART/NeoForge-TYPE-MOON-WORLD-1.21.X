package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.BizenNagamitsuItem;
import software.bernie.geckolib.model.GeoModel;

@SuppressWarnings("deprecation")
public class BizenNagamitsuModel extends GeoModel<BizenNagamitsuItem> {
   public ResourceLocation getModelResource(BizenNagamitsuItem object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "geo/bizen_nagamitsu.geo.json");
   }

   public ResourceLocation getTextureResource(BizenNagamitsuItem object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/item/bizen_nagamitsu.png");
   }

   public ResourceLocation getAnimationResource(BizenNagamitsuItem object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "animations/bizen_nagamitsu.animation.json");
   }
}
