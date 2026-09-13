package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.BrokenExcaliburItem;
import software.bernie.geckolib.model.GeoModel;

public final class BrokenExcaliburModel extends GeoModel<BrokenExcaliburItem> {
   @Override
   public ResourceLocation getModelResource(BrokenExcaliburItem object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "geo/broken_excalibur.geo.json");
   }

   @Override
   public ResourceLocation getTextureResource(BrokenExcaliburItem object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/item/broken_excalibur.png");
   }

   @Override
   public ResourceLocation getAnimationResource(BrokenExcaliburItem object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "animations/broken_excalibur.animation.json");
   }
}
