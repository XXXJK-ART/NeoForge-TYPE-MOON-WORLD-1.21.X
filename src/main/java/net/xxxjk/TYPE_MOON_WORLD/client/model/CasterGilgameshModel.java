package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.CasterGilgameshEntity;

public final class CasterGilgameshModel extends BaseServantModel<CasterGilgameshEntity> {
   public CasterGilgameshModel() {
      super(
         ResourceLocation.fromNamespaceAndPath("typemoonworld", "geo/caster_gilgamesh.geo.json"),
         ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/entity/caster_gilgamesh.png"),
         ResourceLocation.fromNamespaceAndPath("typemoonworld", "animations/caster_gilgamesh.animation.json")
      );
   }
}
