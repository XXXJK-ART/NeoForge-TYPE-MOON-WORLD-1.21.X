package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.GilgameshEntity;

public class GilgameshModel extends BaseServantModel<GilgameshEntity> {
   public GilgameshModel() {
      super(
         ResourceLocation.fromNamespaceAndPath("typemoonworld", "geo/gilgamesh.geo.json"),
         ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/entity/gilgamesh.png"),
         ResourceLocation.fromNamespaceAndPath("typemoonworld", "animations/gilgamesh.animation.json")
      );
   }
}
