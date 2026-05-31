package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.GaeBulgItem;
import software.bernie.geckolib.model.GeoModel;

@SuppressWarnings("deprecation")
public class GaeBulgModel extends GeoModel<GaeBulgItem> {
   @Override
   public ResourceLocation getModelResource(GaeBulgItem object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "geo/gae_bulg.geo.json");
   }

   @Override
   public ResourceLocation getTextureResource(GaeBulgItem object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/item/gae_bulg.png");
   }

   @Override
   public ResourceLocation getAnimationResource(GaeBulgItem object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "animations/gae_bulg.animation.json");
   }
}
