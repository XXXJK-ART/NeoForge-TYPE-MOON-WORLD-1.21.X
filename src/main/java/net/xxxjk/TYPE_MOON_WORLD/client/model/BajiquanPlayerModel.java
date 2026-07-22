package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;

public class BajiquanPlayerModel<T extends LivingEntity> extends PlayerModel<T> {
   public BajiquanPlayerModel(ModelPart root, boolean slim) { super(root, slim); }

   @Override public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
      super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
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
