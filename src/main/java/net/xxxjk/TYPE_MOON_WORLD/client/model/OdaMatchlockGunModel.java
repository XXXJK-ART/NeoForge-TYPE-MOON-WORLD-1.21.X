package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.entity.OdaMatchlockGunEntity;
import software.bernie.geckolib.model.GeoModel;

public class OdaMatchlockGunModel extends GeoModel<OdaMatchlockGunEntity> {
   @Override
   public ResourceLocation getModelResource(OdaMatchlockGunEntity object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "geo/oda_matchlock_gun.geo.json");
   }

   @Override
   public ResourceLocation getTextureResource(OdaMatchlockGunEntity object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/entity/oda_matchlock_gun.png");
   }

   @Override
   public ResourceLocation getAnimationResource(OdaMatchlockGunEntity object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "animations/oda_matchlock_gun.animation.json");
   }
}
