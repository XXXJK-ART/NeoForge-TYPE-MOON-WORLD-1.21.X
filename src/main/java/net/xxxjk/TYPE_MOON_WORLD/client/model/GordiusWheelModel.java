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
   private static final float FRONT_SCALE = 1.5F;
   private static final float REAR_SCALE = 2.0F;
   private static final float FRONT_SCALE_GROUND_OFFSET = -4.0F;
   private static final float REAR_SCALE_GROUND_OFFSET = -8.0F;
   private static final double REAR_RENDER_MAX_HORIZONTAL_OFFSET = 24.0;
   private static final double REAR_RENDER_MAX_VERTICAL_OFFSET = 8.0;

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

      GeoBone front = firstBone("前端", "鍓嶇");
      if (front != null) {
         front.setScaleX(FRONT_SCALE);
         front.setScaleY(FRONT_SCALE);
         front.setScaleZ(FRONT_SCALE);
         front.setPosY(FRONT_SCALE_GROUND_OFFSET);
      }

      GeoBone rear = firstBone("后端", "鍚庣");
      if (rear != null) {
         rear.setScaleX(REAR_SCALE);
         rear.setScaleY(REAR_SCALE);
         rear.setScaleZ(REAR_SCALE);
         Vec3 offset = rearRenderOffset(entity, state.getPartialTick());
         rear.setPosX((float)offset.x);
         rear.setPosY(REAR_SCALE_GROUND_OFFSET + (float)offset.y);
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

      float wheelRot = (entity.tickCount + state.getPartialTick()) * (entity.isCharging() || entity.isDiving() ? 1.25F : 0.72F);
      animateWheel("车轮", wheelRot);
      animateWheel("杞﹁疆", wheelRot);
      animateWheel("wheel", wheelRot);
   }

   private Vec3 rearRenderOffset(GordiusWheelEntity entity, float partialTick) {
      return Vec3.ZERO;
   }

   private void animateLeg(String name, float rotX) {
      GeoBone leg = bone(name);
      if (leg != null) {
         leg.setRotX(rotX);
      }
   }

   private void animateWheel(String name, float rotX) {
      GeoBone wheel = bone(name);
      if (wheel != null) {
         wheel.setRotX(rotX);
      }
   }

   private GeoBone firstBone(String... names) {
      for (String name : names) {
         GeoBone candidate = bone(name);
         if (candidate != null) {
            return candidate;
         }
      }
      return null;
   }

   private GeoBone bone(String name) {
      return this.getAnimationProcessor().getBone(name);
   }
}
