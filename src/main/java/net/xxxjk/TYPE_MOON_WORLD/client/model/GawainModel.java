package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.GawainEntity;

public class GawainModel extends BaseServantModel<GawainEntity> {
   public GawainModel() {
      super(
         ResourceLocation.fromNamespaceAndPath("typemoonworld", "geo/gawain.geo.json"),
         ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/entity/gawain.png"),
         ResourceLocation.fromNamespaceAndPath("typemoonworld", "animations/gawain.animation.json")
      );
   }
}
