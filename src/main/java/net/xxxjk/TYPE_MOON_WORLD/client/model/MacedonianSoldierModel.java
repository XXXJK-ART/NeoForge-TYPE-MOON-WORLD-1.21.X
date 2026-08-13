package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.entity.MacedonianSoldierEntity;
import software.bernie.geckolib.model.GeoModel;

public final class MacedonianSoldierModel extends GeoModel<MacedonianSoldierEntity> {
   @Override
   public ResourceLocation getModelResource(MacedonianSoldierEntity object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "geo/macedonian_soldier.geo.json");
   }

   @Override
   public ResourceLocation getTextureResource(MacedonianSoldierEntity object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/entity/macedonian_soldier.png");
   }

   @Override
   public ResourceLocation getAnimationResource(MacedonianSoldierEntity animatable) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "animations/macedonian_soldier.animation.json");
   }
}
