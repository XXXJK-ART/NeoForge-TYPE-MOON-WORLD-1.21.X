package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.DiarmuidSpearItem;
import software.bernie.geckolib.model.GeoModel;

public class DiarmuidSpearModel extends GeoModel<DiarmuidSpearItem> {
   @Override
   public ResourceLocation getModelResource(DiarmuidSpearItem object) {
      String id = object == null ? "gae_dearg" : object.spearType().id();
      return ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "geo/" + id + ".geo.json");
   }

   @Override
   public ResourceLocation getTextureResource(DiarmuidSpearItem object) {
      String id = object == null ? "gae_dearg" : object.spearType().id();
      return ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "textures/item/" + id + ".png");
   }

   @Override
   public ResourceLocation getAnimationResource(DiarmuidSpearItem object) {
      return ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "animations/gae_bulg.animation.json");
   }
}
