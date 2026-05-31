package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.HecatesStaffItem;
import software.bernie.geckolib.model.GeoModel;

public class HecatesStaffModel extends GeoModel<HecatesStaffItem> {
   @Override
   public ResourceLocation getModelResource(HecatesStaffItem object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "geo/hecates_staff.geo.json");
   }

   @Override
   public ResourceLocation getTextureResource(HecatesStaffItem object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/item/hecates_staff.png");
   }

   @Override
   public ResourceLocation getAnimationResource(HecatesStaffItem object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "animations/hecates_staff.animation.json");
   }
}
