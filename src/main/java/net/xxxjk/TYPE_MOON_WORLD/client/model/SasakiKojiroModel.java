package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.SasakiKojiroEntity;

public class SasakiKojiroModel extends BaseServantModel<SasakiKojiroEntity> {
   public SasakiKojiroModel() {
      super(
         ResourceLocation.fromNamespaceAndPath("typemoonworld", "geo/sasaki_kojiro.geo.json"),
         ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/entity/sasaki_kojiro.png"),
         ResourceLocation.fromNamespaceAndPath("typemoonworld", "animations/sasaki_kojiro.animation.json")
      );
   }
}
