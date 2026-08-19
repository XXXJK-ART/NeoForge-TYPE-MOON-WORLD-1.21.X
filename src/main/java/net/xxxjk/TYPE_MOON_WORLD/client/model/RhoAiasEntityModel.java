package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.xxxjk.TYPE_MOON_WORLD.entity.RhoAiasEntity;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

public class RhoAiasEntityModel extends GeoModel<RhoAiasEntity> {
   @Override
   public ResourceLocation getModelResource(RhoAiasEntity object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "geo/rho_aias.geo.json");
   }

   @Override
   public ResourceLocation getTextureResource(RhoAiasEntity object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/item/rho_aias.png");
   }

   @Override
   public ResourceLocation getAnimationResource(RhoAiasEntity object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "animations/rho_aias.animation.json");
   }

   @Override
   public void setCustomAnimations(RhoAiasEntity entity, long instanceId, AnimationState<RhoAiasEntity> state) {
      super.setCustomAnimations(entity, instanceId, state);
      float facingRotation = -entity.getFacingYaw() * Mth.DEG_TO_RAD;
      rotateRoot("bone", facingRotation);
      rotateRoot("Rhoaias", facingRotation);
   }

   private void rotateRoot(String boneName, float facingRotation) {
      GeoBone root = this.getAnimationProcessor().getBone(boneName);
      if (root != null) {
         root.setRotY(facingRotation);
      }
   }
}
