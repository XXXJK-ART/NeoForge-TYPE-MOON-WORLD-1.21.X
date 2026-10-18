package com.captain.servant.mixin;

import com.captain.servant.leyline.ArtificialLeylineFallbackService;
import net.minecraft.server.level.ServerPlayer;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardManaService;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ServantCardManaService.class, remap = false)
public abstract class ArtificialLeylineManaMixin {
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private static void fatesupport$drainFallbackMana(
            ServerPlayer servant,
            TypeMoonWorldModVariables.PlayerVariables servantVariables,
            CallbackInfo callbackInfo
    ) {
        if (ArtificialLeylineFallbackService.handleManaTick(servant, servantVariables)) {
            callbackInfo.cancel();
        }
    }
}
