package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.EmiyaArcherEntity;

public class EmiyaArcherModel extends BaseServantModel<EmiyaArcherEntity> {
   public EmiyaArcherModel() {
      super(
         ResourceLocation.fromNamespaceAndPath("typemoonworld", "geo/emiya_archer.geo.json"),
         ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/entity/emiya_archer.png"),
         ResourceLocation.fromNamespaceAndPath("typemoonworld", "animations/emiya_archer.animation.json")
      );
   }
}
