package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.RubyStaffItem;
import software.bernie.geckolib.model.GeoModel;

public class RubyStaffModel extends GeoModel<RubyStaffItem> {
   @Override
   public ResourceLocation getModelResource(RubyStaffItem object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "geo/magic_staff.geo.json");
   }

   @Override
   public ResourceLocation getTextureResource(RubyStaffItem object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/item/ruby_staff.png");
   }

   @Override
   public ResourceLocation getAnimationResource(RubyStaffItem object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "animations/ruby_staff.animation.json");
   }
}
