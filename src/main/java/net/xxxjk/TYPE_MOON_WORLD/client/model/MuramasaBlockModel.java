package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.block.entity.MuramasaBlockEntity;
import software.bernie.geckolib.model.GeoModel;

@SuppressWarnings("deprecation")
public class MuramasaBlockModel extends GeoModel<MuramasaBlockEntity> {
   public ResourceLocation getModelResource(MuramasaBlockEntity object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "geo/red_sword_block.geo.json");
   }

   public ResourceLocation getTextureResource(MuramasaBlockEntity object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/block/red_sword_block.png");
   }

   public ResourceLocation getAnimationResource(MuramasaBlockEntity object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "animations/red_sword_block.animation.json");
   }
}
