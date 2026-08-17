package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.BaobhanSithHarpItem;
import software.bernie.geckolib.model.GeoModel;

public final class BaobhanSithHarpModel extends GeoModel<BaobhanSithHarpItem> {
   @Override
   public ResourceLocation getModelResource(BaobhanSithHarpItem object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "geo/baobhan_sith_harp.geo.json");
   }

   @Override
   public ResourceLocation getTextureResource(BaobhanSithHarpItem object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/item/baobhan_sith_harp.png");
   }

   @Override
   public ResourceLocation getAnimationResource(BaobhanSithHarpItem object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "animations/empty.animation.json");
   }
}
