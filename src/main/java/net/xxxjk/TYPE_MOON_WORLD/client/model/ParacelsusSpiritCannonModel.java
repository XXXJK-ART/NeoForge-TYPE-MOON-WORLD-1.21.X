package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ParacelsusSpiritCannonEntity;
import software.bernie.geckolib.model.GeoModel;

public class ParacelsusSpiritCannonModel extends GeoModel<ParacelsusSpiritCannonEntity> {
   @Override
   public ResourceLocation getModelResource(ParacelsusSpiritCannonEntity object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "geo/oda_matchlock_gun.geo.json");
   }

   @Override
   public ResourceLocation getTextureResource(ParacelsusSpiritCannonEntity object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/entity/oda_matchlock_gun.png");
   }

   @Override
   public ResourceLocation getAnimationResource(ParacelsusSpiritCannonEntity object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "animations/oda_matchlock_gun.animation.json");
   }
}
