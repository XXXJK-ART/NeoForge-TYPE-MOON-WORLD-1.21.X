package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.HeshikiriHasebeItem;
import software.bernie.geckolib.model.GeoModel;

@SuppressWarnings("deprecation")
public class HeshikiriHasebeModel extends GeoModel<HeshikiriHasebeItem> {
   @Override
   public ResourceLocation getModelResource(HeshikiriHasebeItem object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "geo/heshikiri_hasebe.geo.json");
   }

   @Override
   public ResourceLocation getTextureResource(HeshikiriHasebeItem object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/entity/heshikiri_hasebe.png");
   }

   @Override
   public ResourceLocation getAnimationResource(HeshikiriHasebeItem object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "animations/heshikiri_hasebe.animation.json");
   }
}
