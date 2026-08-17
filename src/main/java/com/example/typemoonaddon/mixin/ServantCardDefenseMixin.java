package com.example.typemoonaddon.mixin;

import com.example.typemoonaddon.shadowlogic.magic.ShadowBindingService;
import com.example.typemoonaddon.shadowlogic.magic.ShadowArtService;
import com.example.typemoonaddon.magic.MagicOutputService;
import com.example.typemoonaddon.magic.SakuraSpiritualDamageService;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables.PlayerVariables;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Suppresses servant-card defense and dodge rolls against a binding source's allies. */
@Mixin(targets = "net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardDefenseHandler", remap = false)
public abstract class ServantCardDefenseMixin {
    @Inject(method = "handleIncomingDamage", at = @At("HEAD"), cancellable = true, remap = false)
    private static void typemoonaddon$skipFriendlyBindingCardDefense(
        ServerPlayer player,
        PlayerVariables variables,
        LivingIncomingDamageEvent event,
        CallbackInfoReturnable<Boolean> callback
    ) {
        if (SakuraSpiritualDamageService.isCollapse(event.getSource())
            || MagicOutputService.isDamage(event.getSource())
            || ShadowArtService.isUnavoidableInBlackMud(player, event.getSource())
            || ShadowBindingService.isFriendlyDamage(player, event.getSource())) {
            callback.setReturnValue(false);
        }
    }
}
