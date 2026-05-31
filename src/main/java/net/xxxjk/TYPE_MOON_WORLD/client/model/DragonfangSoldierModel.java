package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.entity.DragonfangSoldierEntity;
import software.bernie.geckolib.model.GeoModel;

public class DragonfangSoldierModel extends GeoModel<DragonfangSoldierEntity> {
   @Override
   public ResourceLocation getModelResource(DragonfangSoldierEntity object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "geo/dragonfang_soldier.geo.json");
   }

   @Override
   public ResourceLocation getTextureResource(DragonfangSoldierEntity object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/entity/dragonfang_soldier.png");
   }

   @Override
   public ResourceLocation getAnimationResource(DragonfangSoldierEntity object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "animations/dragonfang_soldier.animation.json");
   }
}
