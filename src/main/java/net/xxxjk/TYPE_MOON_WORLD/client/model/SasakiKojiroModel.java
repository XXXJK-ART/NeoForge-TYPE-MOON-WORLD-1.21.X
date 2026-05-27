package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.SasakiKojiroEntity;
import software.bernie.geckolib.cache.object.GeoBone;

public class SasakiKojiroModel extends BaseServantModel<SasakiKojiroEntity> {
   public SasakiKojiroModel() {
      super(
         ResourceLocation.fromNamespaceAndPath("typemoonworld", "geo/sasaki_kojiro.geo.json"),
         ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/entity/sasaki_kojiro.png"),
         ResourceLocation.fromNamespaceAndPath("typemoonworld", "animations/sasaki_kojiro.animation.json")
      );
   }

   @Override
   protected void applySpawnPoseFallback(SasakiKojiroEntity entity) {
      GeoBone root = this.getAnimationProcessor().getBone("bone");
      if (root != null) {
         root.setPosY(-6.0F);
         root.setScaleX(0.7F);
         root.setScaleY(0.7F);
         root.setScaleZ(0.7F);
      }

      GeoBone body = this.getAnimationProcessor().getBone("body");
      if (body != null) {
         body.setScaleX(1.0F);
         body.setScaleY(1.1F);
         body.setScaleZ(1.1F);
      }

      GeoBone bone4 = this.getAnimationProcessor().getBone("bone4");
      if (bone4 != null) {
         bone4.setScaleX(1.0F);
         bone4.setScaleY(0.9F);
         bone4.setScaleZ(1.0F);
      }

      GeoBone rightArm = this.getAnimationProcessor().getBone("right arm");
      if (rightArm != null) {
         rightArm.setScaleX(1.1F);
         rightArm.setScaleY(1.0F);
         rightArm.setScaleZ(1.0F);
      }

      GeoBone leftArm = this.getAnimationProcessor().getBone("left arm");
      if (leftArm != null) {
         leftArm.setScaleX(1.1F);
         leftArm.setScaleY(1.0F);
         leftArm.setScaleZ(1.0F);
      }

      GeoBone leftLeg = this.getAnimationProcessor().getBone("left_leg");
      if (leftLeg != null) {
         leftLeg.setPosY(-1.0F);
         leftLeg.setScaleX(1.2F);
         leftLeg.setScaleY(1.1F);
         leftLeg.setScaleZ(1.0F);
      }

      GeoBone rightLeg = this.getAnimationProcessor().getBone("right_leg");
      if (rightLeg != null) {
         rightLeg.setPosY(-1.0F);
         rightLeg.setScaleX(1.2F);
         rightLeg.setScaleY(1.1F);
         rightLeg.setScaleZ(1.0F);
      }

      GeoBone bone5 = this.getAnimationProcessor().getBone("bone5");
      if (bone5 != null) {
         bone5.setScaleX(1.1F);
         bone5.setScaleY(1.1F);
         bone5.setScaleZ(1.0F);
      }
   }
}
