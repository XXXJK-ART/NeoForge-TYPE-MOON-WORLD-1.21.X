package com.captain.servant.mixin;

import com.captain.servant.emiya.EmiyaSupportService;
import net.minecraft.server.level.ServerPlayer;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardEmiyaSkills;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ServantCardEmiyaSkills.class, remap = false)
public abstract class EmiyaServantCardSkillsMixin {
    @Inject(method = "copyOpponentWeapon", at = @At("HEAD"), cancellable = true)
    private static void fatesupport$applyCopyRules(
            ServerPlayer player,
            CallbackInfoReturnable<Boolean> callbackInfo
    ) {
        Boolean result = EmiyaSupportService.handleCopy(player);
        if (result != null) {
            callbackInfo.setReturnValue(result);
        }
    }

    @Inject(method = "cycleLoadout", at = @At("HEAD"), cancellable = true)
    private static void fatesupport$applyCycleRules(
            ServerPlayer player,
            TypeMoonWorldModVariables.PlayerVariables variables,
            CallbackInfo callbackInfo
    ) {
        if (EmiyaSupportService.handleCycleLoadout(player, variables)) {
            callbackInfo.cancel();
        }
    }
}
