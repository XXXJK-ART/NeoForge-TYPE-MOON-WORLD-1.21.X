package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.TsumukariMuramasaItem;
import software.bernie.geckolib.model.GeoModel;

@SuppressWarnings("deprecation")
public class TsumukariMuramasaModel extends GeoModel<TsumukariMuramasaItem> {
   public ResourceLocation getModelResource(TsumukariMuramasaItem object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "geo/red_sword.geo.json");
   }

   public ResourceLocation getTextureResource(TsumukariMuramasaItem object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/item/red.png");
   }

   public ResourceLocation getAnimationResource(TsumukariMuramasaItem object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "animations/red_sword.animation.json");
   }
}
