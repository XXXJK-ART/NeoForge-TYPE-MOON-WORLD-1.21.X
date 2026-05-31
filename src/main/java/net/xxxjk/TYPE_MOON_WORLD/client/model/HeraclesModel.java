package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.HeraclesEntity;
import software.bernie.geckolib.cache.object.GeoBone;

public class HeraclesModel extends BaseServantModel<HeraclesEntity> {
   public HeraclesModel() {
      super(
         ResourceLocation.fromNamespaceAndPath("typemoonworld", "geo/heracles.geo.json"),
         ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/entity/heracles.png"),
         ResourceLocation.fromNamespaceAndPath("typemoonworld", "animations/heracles.animation.json")
      );
   }

   @Override
   protected void applySpawnPoseFallback(HeraclesEntity entity) {
      GeoBone root = this.getAnimationProcessor().getBone("bone");
      if (root != null) {
         root.setPosY(-2.0F);
         root.setScaleX(0.9F);
         root.setScaleY(0.9F);
         root.setScaleZ(1.0F);
      }

      GeoBone body = this.getAnimationProcessor().getBone("body");
      if (body != null) {
         body.setScaleX(1.2F);
         body.setScaleY(1.2F);
         body.setScaleZ(1.0F);
      }

      GeoBone bone4 = this.getAnimationProcessor().getBone("bone4");
      if (bone4 != null) {
         bone4.setPosY(1.0F);
      }

      GeoBone rightArm = this.getAnimationProcessor().getBone("right arm");
      if (rightArm != null) {
         rightArm.setPosX(-2.0F);
         rightArm.setPosY(-0.5F);
         rightArm.setScaleX(1.1F);
         rightArm.setScaleY(1.0F);
         rightArm.setScaleZ(1.0F);
      }

      GeoBone leftArm = this.getAnimationProcessor().getBone("left arm");
      if (leftArm != null) {
         leftArm.setPosX(2.0F);
         leftArm.setPosY(-0.5F);
         leftArm.setScaleX(1.1F);
         leftArm.setScaleY(1.0F);
         leftArm.setScaleZ(1.0F);
      }

      GeoBone leftLeg = this.getAnimationProcessor().getBone("left leg");
      if (leftLeg != null) {
         leftLeg.setScaleX(1.3F);
         leftLeg.setScaleY(1.0F);
         leftLeg.setScaleZ(1.0F);
      }

      GeoBone rightLeg = this.getAnimationProcessor().getBone("right leg");
      if (rightLeg != null) {
         rightLeg.setScaleX(1.3F);
         rightLeg.setScaleY(1.0F);
         rightLeg.setScaleZ(1.0F);
      }

      GeoBone bone13 = this.getAnimationProcessor().getBone("bone13");
      if (bone13 != null) {
         bone13.setScaleX(1.1F);
         bone13.setScaleY(1.0F);
         bone13.setScaleZ(1.0F);
      }

      GeoBone bone11 = this.getAnimationProcessor().getBone("bone11");
      if (bone11 != null) {
         bone11.setScaleX(1.1F);
         bone11.setScaleY(1.0F);
         bone11.setScaleZ(1.0F);
      }

      GeoBone bone5 = this.getAnimationProcessor().getBone("bone5");
      if (bone5 != null) {
         bone5.setScaleX(1.1F);
         bone5.setScaleY(1.0F);
         bone5.setScaleZ(1.0F);
      }

      GeoBone bone9 = this.getAnimationProcessor().getBone("bone9");
      if (bone9 != null) {
         bone9.setScaleX(1.1F);
         bone9.setScaleY(1.0F);
         bone9.setScaleZ(1.0F);
      }
   }
}
