package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.TempleStoneSwordAxeItem;
import software.bernie.geckolib.model.GeoModel;

@SuppressWarnings("deprecation")
public class TempleStoneSwordAxeModel extends GeoModel<TempleStoneSwordAxeItem> {
   public ResourceLocation getModelResource(TempleStoneSwordAxeItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "geo/shoot_down_a_hundred_heads.geo.json");
   }

   public ResourceLocation getTextureResource(TempleStoneSwordAxeItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/item/shoot_down_a_hundred_heads.png");
   }

   public ResourceLocation getAnimationResource(TempleStoneSwordAxeItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "animations/shoot_down_a_hundred_heads.animation.json");
   }
}
