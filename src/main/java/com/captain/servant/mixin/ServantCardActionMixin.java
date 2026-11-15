package com.captain.servant.mixin;

import com.captain.servant.lancelot.KnightArmoryService;
import com.captain.servant.compat.superbwarfare.SuperbWarfareLoader;
import com.captain.servant.spiritual.SpiritualizationService;
import net.minecraft.server.level.ServerPlayer;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardLancelotBerserkerSkills;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardTransformManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ServantCardTransformManager.class, remap = false)
public abstract class ServantCardActionMixin {
    @Inject(method = "skillTranslationKey(Ljava/lang/String;IZ)Ljava/lang/String;", at = @At("HEAD"), cancellable = true, require = 0)
    private static void fatesupport$knightArmoryTranslation(
            String servantId,
            int slot,
            boolean crouching,
            CallbackInfoReturnable<String> callbackInfo
    ) {
        if ("lancelot_berserker".equals(servantId) && slot == 8 && crouching) {
            callbackInfo.setReturnValue("skill.fatesupport.knight_armory");
        }
    }

    @Inject(method = "triggerAction", at = @At("HEAD"), cancellable = true, require = 0)
    private static void fatesupport$blockActions(ServerPlayer player, int slot, CallbackInfoReturnable<Boolean> callbackInfo) {
        if (SpiritualizationService.isRestricted(player)) {
            callbackInfo.setReturnValue(false);
            return;
        }
        if (slot == 8 && player.isCrouching() && ServantCardLancelotBerserkerSkills.isActiveCard(player)) {
            callbackInfo.setReturnValue(KnightArmoryService.open(player));
            return;
        }
        if (slot == 8 && ServantCardLancelotBerserkerSkills.isActiveCard(player)
                && !ServantCardLancelotBerserkerSkills.isAroundightActive(player)
                && !ServantCardLancelotBerserkerSkills.hasKnightOfOwnerTarget(player)) {
            int vehicleResult = SuperbWarfareLoader.beginVehicleOwnerization(player);
            if (vehicleResult != 0) {
                callbackInfo.setReturnValue(vehicleResult > 0);
            }
        }
    }

    @Inject(method = "handleHoldAction", at = @At("HEAD"), cancellable = true, require = 0)
    private static void fatesupport$blockHoldActions(
            ServerPlayer player,
            int slot,
            boolean held,
            CallbackInfo callbackInfo
    ) {
        if (SpiritualizationService.isRestricted(player)) {
            callbackInfo.cancel();
        }
    }
}
