package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class BaseServantModel<T extends ServantEntity> extends GeoModel<T> {
   private final ResourceLocation modelResource;
   private final ResourceLocation textureResource;
   private final ResourceLocation animationResource;

   public BaseServantModel(ResourceLocation modelResource, ResourceLocation textureResource, ResourceLocation animationResource) {
      this.modelResource = modelResource;
      this.textureResource = textureResource;
      this.animationResource = animationResource;
   }

   @Override
   public ResourceLocation getModelResource(T entity) {
      return this.modelResource;
   }

   @Override
   public ResourceLocation getTextureResource(T entity) {
      return this.textureResource;
   }

   @Override
   public ResourceLocation getAnimationResource(T entity) {
      return this.animationResource;
   }

   @Override
   public void setCustomAnimations(T entity, long instanceId, AnimationState<T> state) {
      GeoBone head = this.getAnimationProcessor().getBone("head");
      if (head != null) {
         EntityModelData entityData = state.getData(DataTickets.ENTITY_MODEL_DATA);
         float yawDeg = Mth.clamp(entityData.netHeadYaw(), -40.0F, 40.0F);
         head.setRotY(yawDeg * (float) (Math.PI / 180.0));
         head.setRotX(entityData.headPitch() * (float) (Math.PI / 180.0));
      }

      float limbSwing = state.getLimbSwing();
      float limbSwingAmount = state.getLimbSwingAmount();
      GeoBone rightLeg = this.getAnimationProcessor().getBone("right leg");
      GeoBone leftLeg = this.getAnimationProcessor().getBone("left leg");
      GeoBone rightArm = this.getAnimationProcessor().getBone("right arm");
      GeoBone leftArm = this.getAnimationProcessor().getBone("left arm");
      if (rightLeg == null) rightLeg = this.getAnimationProcessor().getBone("right_leg");
      if (leftLeg == null) leftLeg = this.getAnimationProcessor().getBone("left_leg");
      if (rightArm == null) rightArm = this.getAnimationProcessor().getBone("right_arm");
      if (leftArm == null) leftArm = this.getAnimationProcessor().getBone("left_arm");

      if (rightLeg != null && leftLeg != null) {
         rightLeg.setRotX(Mth.cos(limbSwing * 0.6662F) * 0.7F * limbSwingAmount);
         leftLeg.setRotX(Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 0.7F * limbSwingAmount);
      }
      if (rightArm != null && leftArm != null) {
         float baseSwing = Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 0.5F * limbSwingAmount;
         if (entity.isAttackSwinging()) {
            float atkPhase = (float) entity.getAttackSwingTicks() / 12.0F;
            float atkX = Mth.sin(atkPhase * (float) Math.PI) * -2.8F;
            float atkZ = Mth.sin(atkPhase * (float) Math.PI) * 0.6F;
            rightArm.setRotX(baseSwing + atkX);
            rightArm.setRotZ(atkZ);
            leftArm.setRotX(Mth.cos(limbSwing * 0.6662F) * 0.5F * limbSwingAmount);
            leftArm.setRotZ(-atkZ * 0.3F);
         } else {
            rightArm.setRotX(baseSwing);
            leftArm.setRotX(Mth.cos(limbSwing * 0.6662F) * 0.5F * limbSwingAmount);
            rightArm.setRotZ(0);
            leftArm.setRotZ(0);
         }
      }
   }
}
