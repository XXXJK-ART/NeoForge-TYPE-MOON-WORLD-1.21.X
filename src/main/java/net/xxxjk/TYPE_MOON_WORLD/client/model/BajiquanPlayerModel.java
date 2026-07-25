package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;
import net.xxxjk.TYPE_MOON_WORLD.martial.BajiquanNpcCombatController;
import net.xxxjk.TYPE_MOON_WORLD.martial.NpcActionPose;

public class BajiquanPlayerModel<T extends LivingEntity> extends PlayerModel<T> {
   public BajiquanPlayerModel(ModelPart root, boolean slim) { super(root, slim); }

   @Override public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
      super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
      if (entity instanceof NpcActionPose action && action.getNpcActionPose() != 0) {
         switch (action.getNpcActionPose()) {
            case BajiquanNpcCombatController.POSE_KICK -> {
               this.body.yRot = 0.42F;
               this.rightArm.xRot = -0.55F;
               this.leftArm.xRot = -0.32F;
               this.rightLeg.xRot = -1.35F;
               this.leftLeg.xRot = 0.35F;
            }
            case BajiquanNpcCombatController.POSE_ELBOW -> {
               this.body.yRot = -0.38F;
               this.rightArm.xRot = -1.9F;
               this.rightArm.yRot = -0.55F;
               this.leftArm.xRot = -0.75F;
               this.leftArm.yRot = 0.38F;
            }
            case BajiquanNpcCombatController.POSE_SHOULDER, BajiquanNpcCombatController.POSE_PUSH -> {
               this.body.yRot = -0.22F;
               this.rightArm.xRot = -1.2F;
               this.leftArm.xRot = -1.15F;
               this.rightLeg.xRot = -0.35F;
               this.leftLeg.xRot = 0.35F;
            }
            case BajiquanNpcCombatController.POSE_TREMOR -> {
               this.body.xRot = 0.18F;
               this.rightArm.xRot = -2.45F;
               this.leftArm.xRot = -2.25F;
               this.rightLeg.xRot = -0.22F;
               this.leftLeg.xRot = -0.22F;
            }
            default -> {
               this.body.yRot = 0.16F;
               this.rightArm.xRot = -1.8F;
               this.rightArm.yRot = -0.2F;
               this.leftArm.xRot = -0.7F;
               this.leftArm.yRot = 0.28F;
            }
         }
         this.rightSleeve.copyFrom(this.rightArm);
         this.leftSleeve.copyFrom(this.leftArm);
         this.rightPants.copyFrom(this.rightLeg);
         this.leftPants.copyFrom(this.leftLeg);
         return;
      }
      if (this.attackTime <= 0.0F) return;
      float phase = this.attackTime < 0.5F ? this.attackTime * 2.0F : (1.0F - this.attackTime) * 2.0F;
      this.body.yRot = 0.35F * phase;
      this.rightArm.xRot = -1.75F * phase;
      this.rightArm.yRot = -0.25F;
      this.leftArm.xRot = -0.55F * phase;
      this.leftArm.yRot = 0.35F;
      this.rightLeg.xRot = 0.75F * phase;
      this.leftLeg.xRot = -0.2F * phase;
      this.rightSleeve.copyFrom(this.rightArm);
      this.leftSleeve.copyFrom(this.leftArm);
      this.rightPants.copyFrom(this.rightLeg);
      this.leftPants.copyFrom(this.leftLeg);
   }
}
