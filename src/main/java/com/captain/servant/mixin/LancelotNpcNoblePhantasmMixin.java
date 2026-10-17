package com.captain.servant.mixin;

import com.captain.servant.compat.superbwarfare.SuperbWarfareLoader;
import net.minecraft.world.entity.LivingEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatSystem;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.LancelotBerserkerEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ServantCombatSystem.class, remap = false)
public abstract class LancelotNpcNoblePhantasmMixin {
    @Inject(method = "respondToNoblePhantasm", at = @At("HEAD"), cancellable = true, require = 0)
    private static void fatesupport$counterWithA10(
            ServantEntity responder,
            ServantEntity attacker,
            LivingEntity target,
            boolean ranged,
            CallbackInfo callbackInfo
    ) {
        if (responder instanceof LancelotBerserkerEntity lancelot
                && SuperbWarfareLoader.tryCounterNoblePhantasm(lancelot, attacker, target)) {
            // A-10 has higher priority than the original Aroundight response.
            callbackInfo.cancel();
        }
    }
}
