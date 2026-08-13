package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.MacedonianSpearItem;
import software.bernie.geckolib.model.GeoModel;

public final class MacedonianSpearModel extends GeoModel<MacedonianSpearItem> {
   @Override
   public ResourceLocation getModelResource(MacedonianSpearItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "geo/macedonian_spear.geo.json");
   }

   @Override
   public ResourceLocation getTextureResource(MacedonianSpearItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/item/macedonian_spear.png");
   }

   @Override
   public ResourceLocation getAnimationResource(MacedonianSpearItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "animations/empty.animation.json");
   }
}
