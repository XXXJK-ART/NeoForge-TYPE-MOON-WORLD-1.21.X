package com.captain.servant.mixin;

import com.captain.servant.compat.superbwarfare.SuperbWarfareLoader;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardSkillUtils", remap = false)
public abstract class ServantCardSkillUtilsMixin {
    @Inject(
            method = "hitForwardArc(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/phys/Vec3;DF)V",
            at = @At("TAIL")
    )
    private static void fatesupport$damageVehiclesInForwardArc(
            ServerPlayer player,
            Vec3 direction,
            double range,
            float damage,
            CallbackInfo callbackInfo
    ) {
        SuperbWarfareLoader.damageVehiclesInForwardArc(player, direction, range, damage);
    }
}
