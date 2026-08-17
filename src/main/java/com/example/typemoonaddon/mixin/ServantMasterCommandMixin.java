package com.example.typemoonaddon.mixin;

import com.example.typemoonaddon.magic.SakuraPollutionService;
import net.xxxjk.TYPE_MOON_WORLD.combat.ai.AiBrain;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Lets fully corrupted servants fight independently of Type Moon World's master leash. */
@Mixin(targets = "net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantTacticalController", remap = false)
public abstract class ServantMasterCommandMixin {
    @Inject(method = "submitMasterCommand", at = @At("HEAD"), cancellable = true, remap = false)
    private static void typemoonaddon$skipMasterLeash(
        ServantEntity servant,
        AiBrain brain,
        CallbackInfo callback
    ) {
        if (SakuraPollutionService.isFullyCorrupted(servant)) {
            callback.cancel();
        }
    }
}
