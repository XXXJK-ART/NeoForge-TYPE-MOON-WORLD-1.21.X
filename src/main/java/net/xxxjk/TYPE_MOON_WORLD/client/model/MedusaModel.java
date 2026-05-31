package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.MedusaEntity;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.data.EntityModelData;

public class MedusaModel extends BaseServantModel<MedusaEntity> {
   public MedusaModel() {
      super(
         ResourceLocation.fromNamespaceAndPath("typemoonworld", "geo/medusa.geo.json"),
         ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/entity/medusa.png"),
         ResourceLocation.fromNamespaceAndPath("typemoonworld", "animations/medusa.animation.json")
      );
   }

   @Override
   public void setCustomAnimations(MedusaEntity entity, long instanceId, AnimationState<MedusaEntity> state) {
      super.setCustomAnimations(entity, instanceId, state);
      EntityModelData entityData = state.getData(DataTickets.ENTITY_MODEL_DATA);
      if (entityData != null) {
         float pitchRad = Mth.clamp(entityData.headPitch(), -40.0F, 40.0F) * (float)(Math.PI / 180.0);
         GeoBone hair1 = this.getAnimationProcessor().getBone("hair1");
         GeoBone hair2 = this.getAnimationProcessor().getBone("hair2");
         if (hair1 != null) {
            hair1.setRotX(-pitchRad * 0.55F);
         }
         if (hair2 != null) {
            hair2.setRotX(-pitchRad * 0.85F);
         }
      }

      GeoBone eyeMask = this.getAnimationProcessor().getBone("eye mask");
      if (eyeMask != null) {
         float scale = entity.isBlindfoldSealed() ? 1.0F : 0.0F;
         eyeMask.setScaleX(scale);
         eyeMask.setScaleY(scale);
         eyeMask.setScaleZ(scale);
      }

      if (entity.isRidingPegasus()) {
         GeoBone body = this.getAnimationProcessor().getBone("body");
         GeoBone rightArm = this.getAnimationProcessor().getBone("right arm");
         GeoBone leftArm = this.getAnimationProcessor().getBone("left arm");
         GeoBone rightLeg = this.getAnimationProcessor().getBone("right_leg");
         GeoBone leftLeg = this.getAnimationProcessor().getBone("left_leg");
         if (body != null) {
            body.setRotX(-0.2F);
         }
         if (rightArm != null) {
            rightArm.setRotX(-0.35F);
         }
         if (leftArm != null) {
            leftArm.setRotX(-0.35F);
         }
         if (rightLeg != null) {
            rightLeg.setRotX(-1.35F);
            rightLeg.setRotY(-0.314F);
            rightLeg.setRotZ(-0.078F);
         }
         if (leftLeg != null) {
            leftLeg.setRotX(-1.35F);
            leftLeg.setRotY(0.314F);
            leftLeg.setRotZ(0.078F);
         }
      }
   }

   @Override
   protected void applySpawnPoseFallback(MedusaEntity entity) {
      GeoBone root = this.getAnimationProcessor().getBone("bone");
      if (root != null) {
         root.setPosY(-6.0F);
         root.setScaleX(0.7F);
         root.setScaleY(0.75F);
         root.setScaleZ(0.75F);
      }
   }
}
