package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.xxxjk.TYPE_MOON_WORLD.entity.RyougiShikiEntity;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

@SuppressWarnings("deprecation")
public class RyougiShikiModel extends GeoModel<RyougiShikiEntity> {
   public ResourceLocation getModelResource(RyougiShikiEntity object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "geo/ryougi_shiki.geo.json");
   }

   public ResourceLocation getTextureResource(RyougiShikiEntity object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/entity/ryougi_shiki.png");
   }

   public ResourceLocation getAnimationResource(RyougiShikiEntity object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "animations/ryougi_shiki.animation.json");
   }

   public void setCustomAnimations(RyougiShikiEntity animatable, long instanceId, AnimationState<RyougiShikiEntity> animationState) {
      GeoBone head = this.getAnimationProcessor().getBone("head");
      if (head != null) {
         EntityModelData entityData = (EntityModelData)animationState.getData(DataTickets.ENTITY_MODEL_DATA);
         head.setRotY(entityData.netHeadYaw() * (float) (Math.PI / 180.0));
         head.setRotX(entityData.headPitch() * (float) (Math.PI / 180.0));
      }

      GeoBone rightLeg = this.getAnimationProcessor().getBone("right leg");
      GeoBone leftLeg = this.getAnimationProcessor().getBone("left leg");
      if (rightLeg != null && leftLeg != null) {
         float limbSwing = animationState.getLimbSwing();
         float limbSwingAmount = animationState.getLimbSwingAmount();
         rightLeg.setRotX(Mth.cos(limbSwing * 0.6662F) * 0.7F * limbSwingAmount);
         leftLeg.setRotX(Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 0.7F * limbSwingAmount);
      }

      GeoBone rightArm = this.getAnimationProcessor().getBone("right arm");
      GeoBone leftArm = this.getAnimationProcessor().getBone("left arm");
      if (rightArm != null && leftArm != null) {
         float limbSwing = animationState.getLimbSwing();
         float limbSwingAmount = animationState.getLimbSwingAmount();
         rightArm.setRotX(Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 2.0F * limbSwingAmount * 0.5F);
         if (animatable.hasLeftArm()) {
            leftArm.setRotX(Mth.cos(limbSwing * 0.6662F) * 2.0F * limbSwingAmount * 0.5F);
         } else {
            leftArm.setRotX(0.0F);
         }

         if (animatable.isDefending()) {
            rightArm.setRotX(0.9F);
            rightArm.setRotY(0.4F);
         }
      }

      String animationName = "1";
      if (animatable.hasLeftArm() && animatable.hasSword()) {
         animationName = "1";
      } else if (animatable.hasLeftArm() && !animatable.hasSword()) {
         animationName = "2";
      } else if (!animatable.hasLeftArm() && animatable.hasSword()) {
         animationName = "3";
      } else if (!animatable.hasLeftArm() && !animatable.hasSword()) {
         animationName = "4";
      }
   }
}
