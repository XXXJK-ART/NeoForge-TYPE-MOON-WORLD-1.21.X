package net.xxxjk.TYPE_MOON_WORLD.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.xxxjk.TYPE_MOON_WORLD.client.CommandSpellVisualClient;
import net.xxxjk.TYPE_MOON_WORLD.client.FirearmPoseClient;
import net.xxxjk.TYPE_MOON_WORLD.client.BajiquanPoseClient;
import net.xxxjk.TYPE_MOON_WORLD.client.GanryuPoseClient;
import net.xxxjk.TYPE_MOON_WORLD.martial.BajiquanMove;
import net.xxxjk.TYPE_MOON_WORLD.martial.GanryuMove;
import net.xxxjk.TYPE_MOON_WORLD.entity.OdaMatchlockGunEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.MysticMagicianEntity;
import net.xxxjk.TYPE_MOON_WORLD.init.TypeMoonWorldModKeyMappings;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantMasterCarryService;
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
         if (player.getVehicle() instanceof OdaMatchlockGunEntity gun && gun.isMountMode()) {
            PlayerModel<?> model = (PlayerModel<?>)(Object)this;
            model.body.xRot = 0.0F;
            model.body.yRot = 0.0F;
            model.rightLeg.xRot = Mth.cos(limbSwing * 0.6662F) * 0.7F * limbSwingAmount;
            model.leftLeg.xRot = Mth.cos(limbSwing * 0.6662F + (float)Math.PI) * 0.7F * limbSwingAmount;
            model.rightLeg.yRot = 0.0F;
            model.leftLeg.yRot = 0.0F;
            model.rightLeg.zRot = 0.0F;
            model.leftLeg.zRot = 0.0F;
            model.rightPants.copyFrom(model.rightLeg);
            model.leftPants.copyFrom(model.leftLeg);
         }
         Minecraft minecraft = Minecraft.getInstance();
         boolean localPlayer = minecraft.player != null && minecraft.player.getId() == player.getId();
         boolean commandSpellPose = CommandSpellVisualClient.isCommandSpellPoseActive(player);
         boolean firearmPose = FirearmPoseClient.isPoseActive(player);
         if (localPlayer || commandSpellPose || firearmPose) {
            boolean ganderCharging = TypeMoonWorldModKeyMappings.KeyEventListener.isLocalGanderCharging();
            boolean gandrMachineGunCasting = TypeMoonWorldModKeyMappings.KeyEventListener.isLocalGandrMachineGunCasting();
            boolean tapCastPose = TypeMoonWorldModKeyMappings.KeyEventListener.isLocalTapCastPoseActive();
            boolean machineGunFiringPose = TypeMoonWorldModKeyMappings.KeyEventListener.isLocalMachineGunFiringPoseActive();
            if (!localPlayer) {
               ganderCharging = false;
               gandrMachineGunCasting = false;
               tapCastPose = false;
               machineGunFiringPose = false;
            }
            if (ganderCharging || gandrMachineGunCasting || tapCastPose || machineGunFiringPose || commandSpellPose || firearmPose) {
               PlayerModel<?> model = (PlayerModel<?>)(Object)this;
               float pitchRad = Mth.clamp(player.getXRot(), -80.0F, 80.0F) * (float) (Math.PI / 180.0);
               float yawRad = Mth.clamp(model.head.yRot, -1.1F, 1.1F);
               float raiseRot = commandSpellPose || firearmPose ? -1.55F + pitchRad * 0.9F : -1.35F + pitchRad * 0.85F;
               HumanoidArm castingArm = commandSpellPose || firearmPose ? HumanoidArm.RIGHT : TypeMoonWorldModKeyMappings.KeyEventListener.getLocalCastingArm();
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
         TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         BajiquanMove bajiquanMove = BajiquanPoseClient.getMove(player);
         if (bajiquanMove != null) {
            PlayerModel<?> model = (PlayerModel<?>)(Object)this;
            applyBajiquanPose(model, bajiquanMove);
         }
         GanryuMove ganryuMove = GanryuPoseClient.getMove(player);
         if (ganryuMove != null) applyGanryuPose((PlayerModel<?>)(Object)this, ganryuMove);
         if (vars.servant_card_transformed && "cursed_arm_hassan".equals(vars.servant_card_id) && limbSwingAmount > 0.05F) {
            PlayerModel<?> model = (PlayerModel<?>)(Object)this;
            model.rightArm.xRot = 0.0F;
            model.rightArm.yRot = 0.0F;
            model.rightArm.zRot = 0.0F;
            model.rightSleeve.copyFrom(model.rightArm);
         }
         PlayerModel<?> carryModel = (PlayerModel<?>)(Object)this;
         if (ServantMasterCarryService.isCarryingMaster(player)) {
            carryModel.rightArm.xRot = -1.28F;
            carryModel.rightArm.yRot = -0.38F;
            carryModel.rightArm.zRot = 0.18F;
            carryModel.leftArm.xRot = -1.05F;
            carryModel.leftArm.yRot = 0.48F;
            carryModel.leftArm.zRot = -0.24F;
            carryModel.rightSleeve.copyFrom(carryModel.rightArm);
            carryModel.leftSleeve.copyFrom(carryModel.leftArm);
         } else if (ServantMasterCarryService.isCarriedMaster(player)) {
            carryModel.rightArm.xRot = -0.72F;
            carryModel.leftArm.xRot = -0.72F;
            carryModel.rightLeg.xRot = -1.05F;
            carryModel.leftLeg.xRot = -1.05F;
            carryModel.rightLeg.zRot = 0.18F;
            carryModel.leftLeg.zRot = -0.18F;
            carryModel.rightSleeve.copyFrom(carryModel.rightArm);
            carryModel.leftSleeve.copyFrom(carryModel.leftArm);
            carryModel.rightPants.copyFrom(carryModel.rightLeg);
            carryModel.leftPants.copyFrom(carryModel.leftLeg);
         }
      }
   }

   private static void applyBajiquanPose(PlayerModel<?> model, BajiquanMove move) {
      switch (move) {
         case RIGHT_KICK, FINISHER_KICK, KNEE, DOWN_KICK -> {
            model.body.yRot = 0.35F;
            model.rightLeg.xRot = move == BajiquanMove.KNEE ? -1.25F : -0.15F;
            model.rightLeg.yRot = -0.3F;
            model.leftLeg.xRot = 0.2F;
            model.rightArm.xRot = -0.6F;
            model.leftArm.xRot = -0.4F;
         }
         case LEFT_KICK -> {
            model.body.yRot = -0.35F;
            model.leftLeg.xRot = -0.35F;
            model.leftLeg.yRot = 0.3F;
            model.rightLeg.xRot = 0.2F;
            model.rightArm.xRot = -0.4F;
            model.leftArm.xRot = -0.6F;
         }
         case PARRY, CLAMP -> {
            model.rightArm.xRot = -1.25F;
            model.rightArm.yRot = -0.75F;
            model.leftArm.xRot = -1.25F;
            model.leftArm.yRot = 0.75F;
            model.rightLeg.xRot = 0.25F;
            model.leftLeg.xRot = 0.25F;
         }
         case TREMOR, CHARGED_TREMOR, STOMP -> {
            model.body.xRot = 0.35F;
            model.rightArm.xRot = -2.1F;
            model.leftArm.xRot = -2.1F;
            model.rightLeg.xRot = 0.65F;
            model.leftLeg.xRot = 0.65F;
         }
         case DOUBLE_PALM, FIERCE_TIGER, PUSH -> {
            model.body.xRot = 0.15F;
            model.rightArm.xRot = -1.55F;
            model.leftArm.xRot = -1.55F;
            model.rightArm.yRot = -0.18F;
            model.leftArm.yRot = 0.18F;
         }
         case HIGH_JUMP, CHOP -> {
            model.rightArm.xRot = -2.55F;
            model.leftArm.xRot = -0.7F;
            model.rightLeg.xRot = -0.3F;
            model.leftLeg.xRot = 0.45F;
         }
         default -> {
            model.body.yRot = 0.18F;
            model.rightArm.xRot = -1.75F;
            model.rightArm.yRot = -0.22F;
            model.leftArm.xRot = -0.45F;
            model.leftArm.yRot = 0.4F;
            model.rightLeg.xRot = 0.25F;
            model.leftLeg.xRot = -0.12F;
         }
      }
      model.rightSleeve.copyFrom(model.rightArm);
      model.leftSleeve.copyFrom(model.leftArm);
      model.rightPants.copyFrom(model.rightLeg);
      model.leftPants.copyFrom(model.leftLeg);
   }

   private static void applyGanryuPose(PlayerModel<?> model, GanryuMove move) {
      switch (move) {
         case STANCE -> {
            model.body.yRot = 0.22F;
            model.rightArm.xRot = -1.18F;
            model.rightArm.yRot = -0.35F;
            model.leftArm.xRot = -0.95F;
            model.leftArm.yRot = 0.48F;
         }
         case SPARROW_THRUST, SPARROW_SLASH -> {
            model.body.yRot = -0.38F;
            model.rightArm.xRot = -1.55F;
            model.rightArm.yRot = -0.12F;
            model.leftArm.xRot = -1.15F;
         }
         case FLOWER_BUD, SPARROW_THRUST_SECOND -> {
            model.body.xRot = -0.18F;
            model.rightArm.xRot = -2.55F;
            model.leftArm.xRot = -1.85F;
         }
         case STONE_FLOWER, STONE_FLOWER_SECOND -> {
            model.body.xRot = 0.18F;
            model.rightArm.xRot = -2.9F;
            model.leftArm.xRot = -2.2F;
         }
         case SPRING_BUD, SPRING_BUD_SECOND -> {
            model.body.yRot = 0.65F;
            model.rightArm.xRot = -1.25F;
            model.rightArm.yRot = -1.0F;
            model.leftArm.xRot = -1.0F;
         }
         case TSUBAME_GAESHI -> {
            model.body.yRot = -0.8F;
            model.rightArm.xRot = -2.6F;
            model.rightArm.yRot = 0.85F;
            model.leftArm.xRot = -1.9F;
            model.leftArm.yRot = -0.55F;
         }
         default -> {}
      }
      model.rightSleeve.copyFrom(model.rightArm);
      model.leftSleeve.copyFrom(model.leftArm);
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
