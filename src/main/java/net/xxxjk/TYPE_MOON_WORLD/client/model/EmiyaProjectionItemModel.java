package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.EmiyaProjectionItem;
import software.bernie.geckolib.model.GeoModel;

public class EmiyaProjectionItemModel extends GeoModel<EmiyaProjectionItem> {
   @Override
   public ResourceLocation getModelResource(EmiyaProjectionItem object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "geo/" + object.projectionId() + ".geo.json");
   }

   @Override
   public ResourceLocation getTextureResource(EmiyaProjectionItem object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/item/" + object.projectionId() + ".png");
   }

   @Override
   public ResourceLocation getAnimationResource(EmiyaProjectionItem object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "animations/" + object.projectionId() + ".animation.json");
   }
}
