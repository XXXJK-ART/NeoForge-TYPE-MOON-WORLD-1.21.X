package com.example.typemoonaddon.mixin;

import com.example.typemoonaddon.magic.SakuraRuleBreakerDispelService;
import com.example.typemoonaddon.registry.AddonAttachments;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Prevents login repair from granting Grail magics before erosion reaches three. */
@Mixin(targets = "com.example.typemoonaddon.magic.SakuraTypeMoonIntegration", remap = false)
public abstract class GrailKnowledgeGateMixin {
    @Inject(method = "ensureGrailWormPower", at = @At("HEAD"), cancellable = true, remap = false)
    private static void typemoonaddon$gateGrailKnowledge(
        ServerPlayer player,
        CallbackInfoReturnable<Boolean> callback
    ) {
        var data = player.getData(AddonAttachments.IMAGINARY_SPACE.get());
        if (data.grailWormAscended() && !data.grailErosionFull()) {
            callback.setReturnValue(SakuraRuleBreakerDispelService.lockGrailMagicKnowledge(player));
        }
    }
}
