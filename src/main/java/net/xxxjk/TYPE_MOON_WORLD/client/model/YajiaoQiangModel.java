package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.YajiaoQiangItem;
import software.bernie.geckolib.model.GeoModel;

public final class YajiaoQiangModel extends GeoModel<YajiaoQiangItem> {
   @Override public ResourceLocation getModelResource(YajiaoQiangItem item) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "geo/yajiao_qiang.geo.json");
   }
   @Override public ResourceLocation getTextureResource(YajiaoQiangItem item) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/item/yajiao_qiang.png");
   }
   @Override public ResourceLocation getAnimationResource(YajiaoQiangItem item) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "animations/yajiao_qiang.animation.json");
   }
}
