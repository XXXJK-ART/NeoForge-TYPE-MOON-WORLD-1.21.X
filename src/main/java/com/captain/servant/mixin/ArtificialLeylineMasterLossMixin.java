package com.captain.servant.mixin;

import com.captain.servant.leyline.ArtificialLeylineFallbackService;
import net.minecraft.server.level.ServerPlayer;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.MasterServantLinkService;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = MasterServantLinkService.class, remap = false)
public abstract class ArtificialLeylineMasterLossMixin {
    @Inject(method = "onMasterLost", at = @At("HEAD"))
    private static void fatesupport$recordMasterLoss(
            ServerPlayer master,
            TypeMoonWorldModVariables.PlayerVariables masterVariables,
            CallbackInfo callbackInfo
    ) {
        ArtificialLeylineFallbackService.onMasterLost(master, masterVariables);
    }

    @Inject(method = "tickServant", at = @At("HEAD"), cancellable = true)
    private static void fatesupport$keepFallbackServantAlive(
            ServerPlayer servant,
            TypeMoonWorldModVariables.PlayerVariables servantVariables,
            CallbackInfo callbackInfo
    ) {
        if (ArtificialLeylineFallbackService.handleServantLinkTick(servant, servantVariables)) {
            callbackInfo.cancel();
        }
    }
}
