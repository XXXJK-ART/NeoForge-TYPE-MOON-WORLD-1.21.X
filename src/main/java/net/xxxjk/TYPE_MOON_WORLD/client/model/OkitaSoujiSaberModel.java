package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.OkitaSoujiSaberEntity;

public class OkitaSoujiSaberModel extends BaseServantModel<OkitaSoujiSaberEntity> {
   public OkitaSoujiSaberModel() {
      super(
         ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "geo/okita_souji_saber.geo.json"),
         ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "textures/entity/okita_souji_saber.png"),
         ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "animations/okita_souji_saber.animation.json")
      );
   }
}
