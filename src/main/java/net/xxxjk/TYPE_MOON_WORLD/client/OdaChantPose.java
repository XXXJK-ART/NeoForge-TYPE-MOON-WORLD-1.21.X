package net.xxxjk.TYPE_MOON_WORLD.client;

import net.minecraft.client.model.PlayerModel;

/** Shared balanced standing pose while the chant matchlock lifts its rider. */
public final class OdaChantPose {
   private OdaChantPose() { }
   public static void apply(PlayerModel<?> model, float progress) {
      model.body.xRot = -.04F * progress;
      model.body.yRot = 0;
      model.rightLeg.xRot = 0;
      model.leftLeg.xRot = 0;
      model.rightLeg.yRot = 0;
      model.leftLeg.yRot = 0;
      model.rightLeg.zRot = 0;
      model.leftLeg.zRot = 0;
      model.rightArm.xRot = -.18F * progress;
      model.leftArm.xRot = -.18F * progress;
      model.rightArm.yRot = -.1F * progress;
      model.leftArm.yRot = .1F * progress;
      model.rightPants.copyFrom(model.rightLeg);
      model.leftPants.copyFrom(model.leftLeg);
      model.rightSleeve.copyFrom(model.rightArm);
      model.leftSleeve.copyFrom(model.leftArm);
      model.jacket.copyFrom(model.body);
   }
}
