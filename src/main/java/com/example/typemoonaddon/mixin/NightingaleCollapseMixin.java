package com.example.typemoonaddon.mixin;

import com.example.typemoonaddon.magic.SakuraSpiritualDamageService;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps Nightingale's safety circle from canceling spiritual-origin collapse. */
@Mixin(targets = "net.xxxjk.TYPE_MOON_WORLD.event.NightingaleEvents", remap = false)
public abstract class NightingaleCollapseMixin {
    @Inject(
        method = {"protectSafetyCircleStart", "protectSafetyCircleEnd"},
        at = @At("HEAD"),
        cancellable = true,
        remap = false
    )
    private static void typemoonaddon$skipCollapseProtection(
        LivingIncomingDamageEvent event,
        CallbackInfo callback
    ) {
        if (SakuraSpiritualDamageService.isCollapse(event.getSource())) {
            callback.cancel();
        }
    }
}
