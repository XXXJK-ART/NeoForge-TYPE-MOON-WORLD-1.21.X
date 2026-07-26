package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.SpiderCutterItem;
import software.bernie.geckolib.model.GeoModel;

public final class SpiderCutterModel extends GeoModel<SpiderCutterItem> {
   @Override
   public ResourceLocation getModelResource(SpiderCutterItem item) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "geo/spider_cutter.geo.json");
   }

   @Override
   public ResourceLocation getTextureResource(SpiderCutterItem item) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/entity/spider_cutter.png");
   }

   @Override
   public ResourceLocation getAnimationResource(SpiderCutterItem item) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "animations/spider_cutter.animation.json");
   }
}
