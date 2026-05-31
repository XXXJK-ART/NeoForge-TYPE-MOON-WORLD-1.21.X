package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.RuleBreakerItem;
import software.bernie.geckolib.model.GeoModel;

public class RuleBreakerModel extends GeoModel<RuleBreakerItem> {
   @Override
   public ResourceLocation getModelResource(RuleBreakerItem object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "geo/rule_breaker.geo.json");
   }

   @Override
   public ResourceLocation getTextureResource(RuleBreakerItem object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/item/rule_breaker.png");
   }

   @Override
   public ResourceLocation getAnimationResource(RuleBreakerItem object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "animations/rule_breaker.animation.json");
   }
}
