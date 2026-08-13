package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.MacedonianRoundShieldItem;
import software.bernie.geckolib.model.GeoModel;

public final class MacedonianRoundShieldModel extends GeoModel<MacedonianRoundShieldItem> {
   @Override
   public ResourceLocation getModelResource(MacedonianRoundShieldItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "geo/macedonian_round_shield.geo.json");
   }

   @Override
   public ResourceLocation getTextureResource(MacedonianRoundShieldItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/item/macedonian_round_shield.png");
   }

   @Override
   public ResourceLocation getAnimationResource(MacedonianRoundShieldItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "animations/empty.animation.json");
   }
}
