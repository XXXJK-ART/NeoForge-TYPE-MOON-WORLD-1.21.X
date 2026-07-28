package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.NightingaleEntity;

public final class NightingaleModel extends BaseServantModel<NightingaleEntity> {
   public NightingaleModel() {
      super(
         ResourceLocation.fromNamespaceAndPath("typemoonworld", "geo/nightingale.geo.json"),
         ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/entity/nightingale.png"),
         ResourceLocation.fromNamespaceAndPath("typemoonworld", "animations/nightingale.animation.json")
      );
   }
}
