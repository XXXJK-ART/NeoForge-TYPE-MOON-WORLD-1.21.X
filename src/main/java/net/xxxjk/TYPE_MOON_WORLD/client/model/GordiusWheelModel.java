package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.entity.GordiusWheelEntity;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

public final class GordiusWheelModel extends GeoModel<GordiusWheelEntity> {
   private static final float MODEL_GROUND_LIFT = 8.0F;
   private static final float FULL_SCALE = 2.0F;

   @Override
   public ResourceLocation getModelResource(GordiusWheelEntity animatable) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "geo/gordius_wheel.geo.json");
   }

   @Override
   public ResourceLocation getTextureResource(GordiusWheelEntity animatable) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/entity/gordius_wheel.png");
   }

   @Override
   public ResourceLocation getAnimationResource(GordiusWheelEntity animatable) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "animations/gordius_wheel.animation.json");
   }

   @Override
   public void setCustomAnimations(GordiusWheelEntity entity, long instanceId, AnimationState<GordiusWheelEntity> state) {
      super.setCustomAnimations(entity, instanceId, state);
      GeoBone root = bone("bone4");
      if (root != null) {
         root.setPosY(MODEL_GROUND_LIFT);
      }

      GeoBone front = bone("鍓嶇");
      if (front != null) {
         front.setScaleX(FULL_SCALE);
         front.setScaleY(FULL_SCALE);
         front.setScaleZ(FULL_SCALE);
      }

      GeoBone rear = bone("鍚庣");
      if (rear != null) {
         rear.setScaleX(FULL_SCALE);
         rear.setScaleY(FULL_SCALE);
         rear.setScaleZ(FULL_SCALE);
         Vec3 offset = rearRenderOffset(entity, state.getPartialTick());
         rear.setPosX((float)offset.x);
         rear.setPosY((float)offset.y);
         rear.setPosZ((float)offset.z);
      }

      float speed = entity.isCharging() || entity.isDiving() ? 1.85F : 1.0F;
      float stride = (entity.tickCount + state.getPartialTick()) * 0.58F * speed;
      float legSwing = (entity.isCharging() || entity.isDiving() ? 0.62F : 0.42F) * Mth.sin(stride);
      animateLeg("Left front leg", legSwing);
      animateLeg("Right hind leg", legSwing);
      animateLeg("Right front leg", -legSwing);
      animateLeg("Left hind leg", -legSwing);
      animateLeg("Left_front_leg2", -legSwing);
      animateLeg("Right_hind_leg2", -legSwing);
      animateLeg("Right_front_leg2", legSwing);
      animateLeg("Left_hind_leg2", legSwing);

      GeoBone wheel = bone("杞﹁疆");
      if (wheel != null) {
         wheel.setRotX((entity.tickCount + state.getPartialTick()) * (entity.isCharging() || entity.isDiving() ? 1.25F : 0.72F));
      }
   }

   private Vec3 rearRenderOffset(GordiusWheelEntity entity, float partialTick) {
      Vec3 actual = entity.getRearBodyAnchor(partialTick);
      Vec3 ideal = entity.getIdealRearBodyAnchor();
      Vec3 delta = actual.subtract(ideal);
      float yaw = -entity.getYRot() * Mth.DEG_TO_RAD;
      double cos = Math.cos(yaw);
      double sin = Math.sin(yaw);
      double localX = delta.x * cos - delta.z * sin;
      double localZ = delta.x * sin + delta.z * cos;
      double localY = entity.isFlyingMode() ? 0.0 : delta.y;
      return new Vec3(localX * 16.0, localY * 16.0, localZ * 16.0);
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
