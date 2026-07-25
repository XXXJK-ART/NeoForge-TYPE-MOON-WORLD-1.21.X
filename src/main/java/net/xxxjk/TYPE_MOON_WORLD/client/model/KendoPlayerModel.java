package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.xxxjk.TYPE_MOON_WORLD.martial.KendoNpcCombatController;
import net.xxxjk.TYPE_MOON_WORLD.martial.NpcActionPose;

/** Player-shaped model with readable sword strokes for kendo NPCs. */
public class KendoPlayerModel<T extends LivingEntity> extends PlayerModel<T> {
   public KendoPlayerModel(ModelPart root, boolean slim) { super(root, slim); }

   @Override public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
      super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
      if (!(entity instanceof NpcActionPose action) || action.getNpcActionPose() == 0) return;
      float phase = Mth.clamp(action.getNpcActionPoseTicks() / 10.0F, 0.0F, 1.0F);
      switch (action.getNpcActionPose()) {
         case KendoNpcCombatController.POSE_DRAW -> {
            this.body.yRot = -0.45F;
            this.rightArm.xRot = -1.55F;
            this.rightArm.yRot = 0.75F;
            this.leftArm.xRot = -0.75F;
            this.leftArm.yRot = -0.35F;
            this.rightLeg.xRot = 0.22F;
            this.leftLeg.xRot = -0.28F;
         }
         case KendoNpcCombatController.POSE_THRUST -> {
            this.body.yRot = -0.18F;
            this.rightArm.xRot = -1.9F;
            this.rightArm.yRot = -0.08F;
            this.leftArm.xRot = -1.45F;
            this.leftArm.yRot = 0.12F;
            this.rightLeg.xRot = -0.45F;
            this.leftLeg.xRot = 0.62F;
         }
         case KendoNpcCombatController.POSE_GUARD -> {
            this.body.yRot = 0.28F;
            this.rightArm.xRot = -1.35F;
            this.rightArm.yRot = -0.65F;
            this.leftArm.xRot = -1.0F;
            this.leftArm.yRot = 0.42F;
         }
         case KendoNpcCombatController.POSE_RETREAT -> {
            this.body.xRot = 0.12F;
            this.rightArm.xRot = -0.65F;
            this.leftArm.xRot = -0.48F;
            this.rightLeg.xRot = -0.5F;
            this.leftLeg.xRot = -0.5F;
         }
         default -> {
            float swing = 1.0F + (1.0F - phase) * 0.35F;
            this.body.yRot = 0.48F;
            this.rightArm.xRot = -2.45F * swing;
            this.rightArm.yRot = -0.18F;
            this.leftArm.xRot = -1.25F;
            this.leftArm.yRot = 0.28F;
            this.rightLeg.xRot = 0.48F;
            this.leftLeg.xRot = -0.3F;
         }
      }
      this.rightSleeve.copyFrom(this.rightArm);
      this.leftSleeve.copyFrom(this.leftArm);
      this.rightPants.copyFrom(this.rightLeg);
      this.leftPants.copyFrom(this.leftLeg);
   }
}
