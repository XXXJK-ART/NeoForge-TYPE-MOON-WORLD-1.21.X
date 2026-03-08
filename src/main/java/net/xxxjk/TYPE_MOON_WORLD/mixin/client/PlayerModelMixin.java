package net.xxxjk.TYPE_MOON_WORLD.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.xxxjk.TYPE_MOON_WORLD.init.TypeMoonWorldModKeyMappings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerModel.class)
public abstract class PlayerModelMixin<T extends LivingEntity> {

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void applyGanderChargePose(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        if (!(entity instanceof AbstractClientPlayer player)) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.player.getId() != player.getId()) {
            return;
        }
        boolean ganderCharging = TypeMoonWorldModKeyMappings.KeyEventListener.isLocalGanderCharging();
        boolean gandrMachineGunCasting = TypeMoonWorldModKeyMappings.KeyEventListener.isLocalGandrMachineGunCasting();
        boolean tapCastPose = TypeMoonWorldModKeyMappings.KeyEventListener.isLocalTapCastPoseActive();
        boolean machineGunFiringPose = TypeMoonWorldModKeyMappings.KeyEventListener.isLocalMachineGunFiringPoseActive();
        if (!ganderCharging && !gandrMachineGunCasting && !tapCastPose && !machineGunFiringPose) {
            return;
        }

        PlayerModel<?> model = (PlayerModel<?>)(Object)this;
        float pitchRad = Mth.clamp(player.getXRot(), -80.0F, 80.0F) * ((float)Math.PI / 180.0F);
        float yawRad = Mth.clamp(model.head.yRot, -1.1F, 1.1F);
        float raiseRot = -1.35F + (pitchRad * 0.85F);
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
