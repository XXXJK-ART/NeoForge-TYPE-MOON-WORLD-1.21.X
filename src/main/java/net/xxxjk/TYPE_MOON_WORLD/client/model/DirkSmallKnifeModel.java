package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.DirkSmallKnifeItem;
import software.bernie.geckolib.model.GeoModel;

public class DirkSmallKnifeModel extends GeoModel<DirkSmallKnifeItem> {
   @Override
   public ResourceLocation getModelResource(DirkSmallKnifeItem object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "geo/dirk_small_knife.geo.json");
   }

   @Override
   public ResourceLocation getTextureResource(DirkSmallKnifeItem object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/item/dirk_small_knife.png");
   }

   @Override
   public ResourceLocation getAnimationResource(DirkSmallKnifeItem object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "animations/dirk_small_knife.animation.json");
   }
}
