package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.UshiwakamaruRiderEntity;

public final class UshiwakamaruRiderModel extends BaseServantModel<UshiwakamaruRiderEntity> {
   public UshiwakamaruRiderModel() {
      super(
         ResourceLocation.fromNamespaceAndPath("typemoonworld", "geo/ushiwakamaru_rider.geo.json"),
         ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/entity/ushiwakamaru_rider.png"),
         ResourceLocation.fromNamespaceAndPath("typemoonworld", "animations/ushiwakamaru_rider.animation.json")
      );
   }
}
