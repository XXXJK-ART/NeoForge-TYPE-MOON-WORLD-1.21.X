package com.captain.servant.mixin;

import com.captain.servant.compat.superbwarfare.SuperbWarfareLoader;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.LancelotBerserkerEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.lancelot.LancelotCombatHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = LancelotCombatHelper.class, remap = false)
public abstract class LancelotNpcAroundightMixin {
    @Inject(method = "tryDrawAroundight", at = @At("HEAD"), cancellable = true, require = 0)
    private static void fatesupport$prioritizeA10(
            LancelotBerserkerEntity lancelot,
            net.minecraft.server.level.ServerLevel level,
            net.minecraft.world.entity.LivingEntity target,
            CallbackInfoReturnable<Boolean> callbackInfo
    ) {
        if (lancelot.getVehicle() != null && lancelot.getVehicle().getPersistentData()
                .getBoolean(SuperbWarfareLoader.VEHICLE_A10_TAG)) {
            callbackInfo.setReturnValue(true);
            return;
        }
        if (SuperbWarfareLoader.tryDeployNpcA10(lancelot, target)) {
            // A-10 is the higher-priority noble phantasm. Aroundight is only
            // allowed to run when A-10 cannot be deployed.
            callbackInfo.setReturnValue(true);
        }
    }
}
