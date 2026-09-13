package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.block.entity.SwordInStoneBlockEntity;
import software.bernie.geckolib.model.GeoModel;

public final class SwordInStoneBlockModel extends GeoModel<SwordInStoneBlockEntity> {
   @Override
   public ResourceLocation getModelResource(SwordInStoneBlockEntity object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "geo/sword_in_stone.geo.json");
   }

   @Override
   public ResourceLocation getTextureResource(SwordInStoneBlockEntity object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/block/sword_in_stone.png");
   }

   @Override
   public ResourceLocation getAnimationResource(SwordInStoneBlockEntity object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "animations/sword_in_stone.animation.json");
   }
}
