package com.captain.servant.mixin;

import com.captain.servant.compat.superbwarfare.SuperbWarfareLoader;
import net.minecraft.server.level.ServerPlayer;
import net.xxxjk.TYPE_MOON_WORLD.servant.lancelot.LancelotCombatHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = LancelotCombatHelper.class, remap = false)
public abstract class LancelotCombatHelperMixin {
    @Inject(method = "countHeldKnightOfOwnerItems(Lnet/minecraft/world/entity/LivingEntity;)I", at = @At("RETURN"), cancellable = true, require = 0)
    private static void fatesupport$countNpcOwnedVehicles(
            net.minecraft.world.entity.LivingEntity owner,
            CallbackInfoReturnable<Integer> callbackInfo
    ) {
        if (!(owner instanceof ServerPlayer) && owner != null) {
            callbackInfo.setReturnValue(Math.min(LancelotCombatHelper.KNIGHT_OF_OWNER_ITEM_LIMIT,
                    callbackInfo.getReturnValueI() + SuperbWarfareLoader.countOwnedVehicles(owner)));
        }
    }

    @Inject(method = "countHeldKnightOfOwnerItems(Lnet/minecraft/server/level/ServerPlayer;)I", at = @At("RETURN"), cancellable = true, require = 0)
    private static void fatesupport$countOwnedVehicles(
            ServerPlayer player,
            CallbackInfoReturnable<Integer> callbackInfo
    ) {
        callbackInfo.setReturnValue(Math.min(LancelotCombatHelper.KNIGHT_OF_OWNER_ITEM_LIMIT,
                callbackInfo.getReturnValueI() + SuperbWarfareLoader.countOwnedVehicles(player)));
    }
}
