package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.SenkoMuramasaEntity;

public class SenkoMuramasaModel extends BaseServantModel<SenkoMuramasaEntity> {
   public SenkoMuramasaModel() {
      super(
         ResourceLocation.fromNamespaceAndPath("typemoonworld", "geo/senko_muramasa.geo.json"),
         ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/entity/senko_muramasa.png"),
         ResourceLocation.fromNamespaceAndPath("typemoonworld", "animations/senko_muramasa.animation.json")
      );
   }
}
