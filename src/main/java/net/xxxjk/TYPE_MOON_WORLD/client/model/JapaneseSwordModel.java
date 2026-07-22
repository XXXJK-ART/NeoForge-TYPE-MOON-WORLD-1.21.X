package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.JapaneseSwordItem;
import software.bernie.geckolib.model.GeoModel;

public class JapaneseSwordModel extends GeoModel<JapaneseSwordItem> {
   @Override
   public ResourceLocation getModelResource(JapaneseSwordItem item) {
      return asset(item, "geo/", ".geo.json");
   }

   @Override
   public ResourceLocation getTextureResource(JapaneseSwordItem item) {
      return asset(item, "textures/item/", ".png");
   }

   @Override
   public ResourceLocation getAnimationResource(JapaneseSwordItem item) {
      return asset(item, "animations/", ".animation.json");
   }

   private static ResourceLocation asset(JapaneseSwordItem item, String directory, String extension) {
      return ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, directory + item.assetId() + extension);
   }
}
