package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class ServantModel extends GeoModel<ServantEntity> {
   @Override
   public ResourceLocation getModelResource(ServantEntity entity) {
      var def = entity.getDefinition();
      if (def != null && !def.modelGeometryPath().isEmpty()) {
         return ResourceLocation.parse(def.modelGeometryPath());
      }
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "geo/heracles.geo.json");
   }

   @Override
   public ResourceLocation getTextureResource(ServantEntity entity) {
      var def = entity.getDefinition();
      if (def != null && !def.texturePath().isEmpty()) {
         return ResourceLocation.parse(def.texturePath());
      }
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/entity/heracles.png");
   }

   @Override
   public ResourceLocation getAnimationResource(ServantEntity entity) {
      var def = entity.getDefinition();
      if (def != null && !def.animationPath().isEmpty()) {
         return ResourceLocation.parse(def.animationPath());
      }
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "animations/heracles.animation.json");
   }

   @Override
   public void setCustomAnimations(ServantEntity entity, long instanceId, AnimationState<ServantEntity> state) {
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
         rightArm.setRotX(Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 0.5F * limbSwingAmount);
         leftArm.setRotX(Mth.cos(limbSwing * 0.6662F) * 0.5F * limbSwingAmount);
      }
   }
}
