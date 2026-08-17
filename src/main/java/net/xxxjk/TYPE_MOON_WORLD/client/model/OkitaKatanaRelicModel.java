package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.OkitaKatanaRelicItem;
import software.bernie.geckolib.model.GeoModel;

public final class OkitaKatanaRelicModel extends GeoModel<OkitaKatanaRelicItem> {
   @Override
   public ResourceLocation getModelResource(OkitaKatanaRelicItem item) {
      return ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "geo/katana.geo.json");
   }

   @Override
   public ResourceLocation getTextureResource(OkitaKatanaRelicItem item) {
      return ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "textures/item/katana.png");
   }

   @Override
   public ResourceLocation getAnimationResource(OkitaKatanaRelicItem item) {
      return ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "animations/katana.animation.json");
   }
}
