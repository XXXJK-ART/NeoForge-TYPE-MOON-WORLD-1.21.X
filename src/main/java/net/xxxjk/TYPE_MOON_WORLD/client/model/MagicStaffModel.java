package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.GemType;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.MagicStaffItem;
import software.bernie.geckolib.model.GeoModel;

public class MagicStaffModel extends GeoModel<MagicStaffItem> {
   @Override
   public ResourceLocation getModelResource(MagicStaffItem object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "geo/ruby_staff.geo.json");
   }

   @Override
   public ResourceLocation getTextureResource(MagicStaffItem object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/item/" + textureName(object) + ".png");
   }

   @Override
   public ResourceLocation getAnimationResource(MagicStaffItem object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "animations/ruby_staff.animation.json");
   }

   private static String textureName(MagicStaffItem item) {
      GemType type = item == null ? null : item.getGemType();
      if (type == null) {
         return "staff";
      }
      return switch (type) {
         case SAPPHIRE -> "sapphire_staff";
         case EMERALD -> "emerald_staff";
         case CYAN -> "cyan_staff";
         case TOPAZ -> "topaz_staff";
         default -> "staff";
      };
   }
}
