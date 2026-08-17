package com.example.typemoonaddon.mixin;

import com.example.typemoonaddon.compat.TypeMoonContractBridge;
import com.example.typemoonaddon.magic.SakuraPollutionService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Rejects built-in target command spells before they apply an effect or consume a spell. */
@Mixin(targets = "net.xxxjk.TYPE_MOON_WORLD.servant.card.MasterStateManager", remap = false)
public abstract class CommandSpellPollutionMixin {
    @Inject(method = "useCommandSpell", at = @At("HEAD"), cancellable = true, remap = false)
    private static void typemoonaddon$blockPollutedServantCommandSpell(
        ServerPlayer master,
        int action,
        CallbackInfoReturnable<Boolean> callback
    ) {
        if (action < 0 || action > 2) {
            return;
        }
        LivingEntity target = TypeMoonContractBridge.boundServant(master);
        if (SakuraPollutionService.blocksCommandSpell(target)) {
            callback.setReturnValue(false);
        }
    }
}
