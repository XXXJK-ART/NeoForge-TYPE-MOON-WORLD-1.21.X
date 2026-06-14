package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.OdaNobunagaEntity;

public class OdaNobunagaModel extends BaseServantModel<OdaNobunagaEntity> {
   public OdaNobunagaModel() {
      super(
         ResourceLocation.fromNamespaceAndPath("typemoonworld", "geo/oda_nobunaga.geo.json"),
         ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/entity/oda_nobunaga.png"),
         ResourceLocation.fromNamespaceAndPath("typemoonworld", "animations/oda_nobunaga.animation.json")
      );
   }
}
