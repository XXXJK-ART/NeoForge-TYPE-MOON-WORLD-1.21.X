package com.captain.servant.mixin;

import com.captain.servant.compat.superbwarfare.SuperbWarfareLoader;
import net.minecraft.server.level.ServerPlayer;
import net.xxxjk.TYPE_MOON_WORLD.servant.lancelot.LancelotCombatHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = LancelotCombatHelper.class, remap = false)
public abstract class LancelotCombatHelperMixin {
    @Inject(method = "countHeldKnightOfOwnerItems", at = @At("RETURN"), cancellable = true)
    private static void fatesupport$countOwnedVehicles(
            ServerPlayer player,
            CallbackInfoReturnable<Integer> callbackInfo
    ) {
        callbackInfo.setReturnValue(callbackInfo.getReturnValueI()
                + SuperbWarfareLoader.countOwnedVehicles(player));
    }
}
