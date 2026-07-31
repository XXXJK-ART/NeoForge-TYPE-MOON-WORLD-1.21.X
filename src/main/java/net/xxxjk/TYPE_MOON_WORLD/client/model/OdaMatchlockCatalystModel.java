package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.OdaMatchlockCatalystItem;
import software.bernie.geckolib.model.GeoModel;

public final class OdaMatchlockCatalystModel extends GeoModel<OdaMatchlockCatalystItem> {
   @Override
   public ResourceLocation getModelResource(OdaMatchlockCatalystItem item) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "geo/oda_matchlock_gun.geo.json");
   }

   @Override
   public ResourceLocation getTextureResource(OdaMatchlockCatalystItem item) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/item/oda_matchlock_catalyst.png");
   }

   @Override
   public ResourceLocation getAnimationResource(OdaMatchlockCatalystItem item) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "animations/oda_matchlock_gun.animation.json");
   }
}
