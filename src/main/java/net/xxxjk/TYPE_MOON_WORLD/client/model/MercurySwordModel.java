package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.MercurySwordItem;
import software.bernie.geckolib.model.GeoModel;

public class MercurySwordModel extends GeoModel<MercurySwordItem> {
   @Override
   public ResourceLocation getModelResource(MercurySwordItem object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "geo/mercury_sword.geo.json");
   }

   @Override
   public ResourceLocation getTextureResource(MercurySwordItem object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/item/mercury_sword.png");
   }

   @Override
   public ResourceLocation getAnimationResource(MercurySwordItem object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "animations/mercury_sword.animation.json");
   }
}
