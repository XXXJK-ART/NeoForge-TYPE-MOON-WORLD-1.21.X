package net.xxxjk.TYPE_MOON_WORLD.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.MysticMagicianEntity;
import net.xxxjk.TYPE_MOON_WORLD.init.TypeMoonWorldModKeyMappings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({PlayerModel.class})
public abstract class PlayerModelMixin<T extends LivingEntity> {
   @Inject(
      method = {"setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V"},
      at = {@At("TAIL")}
   )
   private void applyGanderChargePose(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
      if (entity instanceof AbstractClientPlayer player) {
         Minecraft minecraft = Minecraft.getInstance();
         if (minecraft.player != null && minecraft.player.getId() == player.getId()) {
            boolean ganderCharging = TypeMoonWorldModKeyMappings.KeyEventListener.isLocalGanderCharging();
            boolean gandrMachineGunCasting = TypeMoonWorldModKeyMappings.KeyEventListener.isLocalGandrMachineGunCasting();
            boolean tapCastPose = TypeMoonWorldModKeyMappings.KeyEventListener.isLocalTapCastPoseActive();
            boolean machineGunFiringPose = TypeMoonWorldModKeyMappings.KeyEventListener.isLocalMachineGunFiringPoseActive();
            if (ganderCharging || gandrMachineGunCasting || tapCastPose || machineGunFiringPose) {
               PlayerModel<?> model = (PlayerModel<?>)(Object)this;
               float pitchRad = Mth.clamp(player.getXRot(), -80.0F, 80.0F) * (float) (Math.PI / 180.0);
               float yawRad = Mth.clamp(model.head.yRot, -1.1F, 1.1F);
               float raiseRot = -1.35F + pitchRad * 0.85F;
               HumanoidArm castingArm = TypeMoonWorldModKeyMappings.KeyEventListener.getLocalCastingArm();
               if (castingArm == HumanoidArm.LEFT) {
                  model.leftArm.xRot = raiseRot;
                  model.leftArm.yRot = yawRad + 0.08F;
                  model.leftArm.zRot = -0.02F;
                  model.leftSleeve.copyFrom(model.leftArm);
               } else {
                  model.rightArm.xRot = raiseRot;
                  model.rightArm.yRot = yawRad - 0.08F;
                  model.rightArm.zRot = 0.02F;
                  model.rightSleeve.copyFrom(model.rightArm);
               }
            }
         }
      }
   }

   @Inject(
      method = {"setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V"},
      at = {@At("TAIL")}
   )
   private void applyMysticMagicianCastingPose(
      T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci
   ) {
      if (entity instanceof MysticMagicianEntity magician) {
         PlayerModel<?> model = (PlayerModel<?>)(Object)this;
         if (magician.isMeleeSkillPoseActive()) {
            int pose = magician.getMeleeSkillPose();
            switch (pose) {
               case MysticMagicianEntity.MELEE_POSE_PUNCH:
                  model.rightArm.xRot = -1.7F;
                  model.rightArm.yRot = -0.15F;
                  model.rightArm.zRot = 0.05F;
                  model.leftArm.xRot = 0.25F;
                  model.leftArm.yRot = 0.35F;
                  model.leftArm.zRot = -0.18F;
                  model.body.yRot = 0.08F;
                  break;
               case MysticMagicianEntity.MELEE_POSE_WHIP_KICK:
                  model.body.yRot = 0.42F;
                  model.rightLeg.xRot = -0.15F;
                  model.rightLeg.yRot = 0.4F;
                  model.leftLeg.xRot = 0.95F;
                  model.leftLeg.yRot = -0.22F;
                  model.rightArm.xRot = -0.5F;
                  model.leftArm.xRot = -0.35F;
                  break;
               case MysticMagicianEntity.MELEE_POSE_UPPER_THROW:
                  model.rightArm.xRot = -2.2F;
                  model.rightArm.yRot = -0.22F;
                  model.leftArm.xRot = -2.2F;
                  model.leftArm.yRot = 0.22F;
                  model.rightLeg.xRot = 0.35F;
                  model.leftLeg.xRot = 0.35F;
                  break;
               case MysticMagicianEntity.MELEE_POSE_SLAM:
                  model.rightArm.xRot = -2.75F;
                  model.rightArm.yRot = -0.05F;
                  model.leftArm.xRot = -2.75F;
                  model.leftArm.yRot = 0.05F;
                  model.body.xRot = 0.2F;
                  model.rightLeg.xRot = -0.25F;
                  model.leftLeg.xRot = -0.25F;
                  break;
               default:
                  break;
            }

            model.rightSleeve.copyFrom(model.rightArm);
            model.leftSleeve.copyFrom(model.leftArm);
            model.rightPants.copyFrom(model.rightLeg);
            model.leftPants.copyFrom(model.leftLeg);
         } else if (magician.isCastingPoseActive()) {
            float pitchRad = Mth.clamp(magician.getXRot(), -80.0F, 80.0F) * (float)(Math.PI / 180.0);
            float yawRad = Mth.clamp(model.head.yRot, -1.1F, 1.1F);
            float raiseRot = -1.35F + pitchRad * 0.85F;
            model.rightArm.xRot = raiseRot;
            model.rightArm.yRot = yawRad - 0.08F;
            model.rightArm.zRot = 0.02F;
            model.rightSleeve.copyFrom(model.rightArm);
         }
      }
   }
}
