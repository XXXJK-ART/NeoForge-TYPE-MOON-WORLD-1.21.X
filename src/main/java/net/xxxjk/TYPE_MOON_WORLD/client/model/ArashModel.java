package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ArashEntity;

public final class ArashModel extends BaseServantModel<ArashEntity> {
   public ArashModel() {
      super(ResourceLocation.fromNamespaceAndPath("typemoonworld", "geo/arash.geo.json"),
         ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/entity/arash.png"),
         ResourceLocation.fromNamespaceAndPath("typemoonworld", "animations/arash.animation.json"));
   }
}
