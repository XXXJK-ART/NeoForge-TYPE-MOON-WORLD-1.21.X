package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ArtoriaPendragonEntity;

public class ArtoriaPendragonModel extends BaseServantModel<ArtoriaPendragonEntity> {
   public ArtoriaPendragonModel() {
      super(
         ResourceLocation.fromNamespaceAndPath("typemoonworld", "geo/artoria_pendragon.geo.json"),
         ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/entity/artoria_pendragon.png"),
         ResourceLocation.fromNamespaceAndPath("typemoonworld", "animations/artoria_pendragon.animation.json")
      );
   }
}
