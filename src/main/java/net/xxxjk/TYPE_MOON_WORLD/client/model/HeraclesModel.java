package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.HeraclesEntity;

public class HeraclesModel extends BaseServantModel<HeraclesEntity> {
   public HeraclesModel() {
      super(
         ResourceLocation.fromNamespaceAndPath("typemoonworld", "geo/heracles.geo.json"),
         ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/entity/heracles.png"),
         ResourceLocation.fromNamespaceAndPath("typemoonworld", "animations/heracles.animation.json")
      );
   }
}
