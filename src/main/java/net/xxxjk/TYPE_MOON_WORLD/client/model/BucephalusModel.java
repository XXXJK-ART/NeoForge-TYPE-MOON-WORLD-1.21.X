package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.xxxjk.TYPE_MOON_WORLD.entity.BucephalusEntity;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

public final class BucephalusModel extends GeoModel<BucephalusEntity> {
   private static final float MODEL_GROUND_LIFT = 8.0F;

   @Override
   public ResourceLocation getModelResource(BucephalusEntity animatable) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "geo/bucephalus.geo.json");
   }

   @Override
   public ResourceLocation getTextureResource(BucephalusEntity animatable) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/entity/bucephalus.png");
   }

   @Override
   public ResourceLocation getAnimationResource(BucephalusEntity animatable) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "animations/bucephalus.animation.json");
   }

   @Override
   public void setCustomAnimations(BucephalusEntity entity, long instanceId, AnimationState<BucephalusEntity> state) {
      super.setCustomAnimations(entity, instanceId, state);
      GeoBone root = bone("bone");
      if (root == null) {
         root = bone("root");
      }
      if (root != null) {
         root.setPosY(MODEL_GROUND_LIFT);
      }

      float stride = (entity.tickCount + state.getPartialTick()) * (entity.isCharging() ? 0.88F : 0.48F);
      float swing = (entity.isCharging() ? 0.72F : 0.42F) * Mth.sin(stride);
      animateLeg("Left front leg", swing);
      animateLeg("Right hind leg", swing);
      animateLeg("Right front leg", -swing);
      animateLeg("Left hind leg", -swing);
   }

   private void animateLeg(String name, float rotX) {
      GeoBone leg = bone(name);
      if (leg != null) {
         leg.setRotX(rotX);
      }
   }

   private GeoBone bone(String name) {
      return this.getAnimationProcessor().getBone(name);
   }
}
