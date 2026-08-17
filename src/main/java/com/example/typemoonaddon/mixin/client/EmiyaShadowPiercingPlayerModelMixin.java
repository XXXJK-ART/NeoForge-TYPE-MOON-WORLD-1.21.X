package com.example.typemoonaddon.mixin.client;

import com.example.typemoonaddon.client.EmiyaShadowPiercingAnimation;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerModel.class)
public abstract class EmiyaShadowPiercingPlayerModelMixin {
    @Inject(
        method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V",
        at = @At("TAIL")
    )
    private void typemoonaddon$applyShadowPiercingPose(
        LivingEntity entity,
        float limbSwing,
        float limbSwingAmount,
        float ageInTicks,
        float netHeadYaw,
        float headPitch,
        CallbackInfo callback
    ) {
        EmiyaShadowPiercingAnimation.applyPreparedModel(entity, (PlayerModel<?>)(Object)this);
    }
}
