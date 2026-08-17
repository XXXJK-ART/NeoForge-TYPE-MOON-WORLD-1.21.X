package com.example.typemoonaddon.mixin;

import com.example.typemoonaddon.magic.SakuraRuleBreakerDispelService;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Adds persistent addon states to every Rule Breaker source and Medea's target evaluation. */
@Mixin(targets = "net.xxxjk.TYPE_MOON_WORLD.servant.entity.MedeaCombatHelper", remap = false)
public abstract class MedeaRuleBreakerMixin {
    @Inject(method = "applyRuleBreakerHit", at = @At("HEAD"), remap = false)
    private static void typemoonaddon$dispelAddonState(
        LivingEntity target,
        LivingEntity attacker,
        CallbackInfo callback
    ) {
        SakuraRuleBreakerDispelService.dispel(target);
    }

    @Inject(method = "ruleBreakerWouldBeEffective", at = @At("HEAD"), cancellable = true, remap = false)
    private static void typemoonaddon$recognizeAddonState(
        LivingEntity target,
        CallbackInfoReturnable<Boolean> callback
    ) {
        if (SakuraRuleBreakerDispelService.canDispel(target)) {
            callback.setReturnValue(true);
        }
    }
}
