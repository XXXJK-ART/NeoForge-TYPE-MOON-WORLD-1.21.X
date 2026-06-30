package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.NamelessBowItem;
import software.bernie.geckolib.model.GeoModel;

public class NamelessBowModel extends GeoModel<NamelessBowItem> {
   @Override
   public ResourceLocation getModelResource(NamelessBowItem object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "geo/nameless_bow.geo.json");
   }

   @Override
   public ResourceLocation getTextureResource(NamelessBowItem object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/item/nameless_bow.png");
   }

   @Override
   public ResourceLocation getAnimationResource(NamelessBowItem object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "animations/nameless_bow.animation.json");
   }
}
