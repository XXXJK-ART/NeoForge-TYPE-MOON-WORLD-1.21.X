package com.captain.servant.mixin;

import com.captain.servant.lancelot.KnightArmoryService;
import net.minecraft.server.level.ServerPlayer;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardLancelotBerserkerSkills;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ServantCardLancelotBerserkerSkills.class, remap = false)
public abstract class LancelotKnightOfOwnerMixin {
    @Inject(method = "performKnightOfOwner", at = @At("HEAD"), cancellable = true)
    private static void fatesupport$integrateSuperbWarfare(
            ServerPlayer player,
            CallbackInfoReturnable<Boolean> callbackInfo
    ) {
        if (KnightArmoryService.ownerizeHeldSuperbWarfareGun(player)) {
            callbackInfo.setReturnValue(true);
        }
    }
}
