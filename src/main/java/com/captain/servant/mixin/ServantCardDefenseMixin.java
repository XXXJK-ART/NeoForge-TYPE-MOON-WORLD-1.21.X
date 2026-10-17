package com.captain.servant.mixin;

import com.captain.servant.spiritual.SpiritualizationService;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardDefenseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ServantCardDefenseHandler.class, remap = false)
public abstract class ServantCardDefenseMixin {
    @Inject(method = "handleIncomingDamage", at = @At("HEAD"), cancellable = true)
    private static void fatesupport$disableDefense(
            ServerPlayer player,
            TypeMoonWorldModVariables.PlayerVariables variables,
            LivingIncomingDamageEvent event,
            CallbackInfoReturnable<Boolean> callbackInfo
    ) {
        if (SpiritualizationService.isRestricted(player)) {
            callbackInfo.setReturnValue(false);
        }
    }
}
