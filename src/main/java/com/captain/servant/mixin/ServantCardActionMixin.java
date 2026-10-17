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
    @Inject(method = "skillTranslationKey(Ljava/lang/String;IZ)Ljava/lang/String;", at = @At("HEAD"), cancellable = true)
    private static void fatesupport$knightArmoryTranslation(
            String servantId,
            int slot,
            boolean crouching,
            CallbackInfoReturnable<String> callbackInfo
    ) {
        if ("lancelot_berserker".equals(servantId) && slot == 6) {
            callbackInfo.setReturnValue("skill.fatesupport.knight_armory");
        } else if ("lancelot_berserker".equals(servantId) && slot == 7) {
            callbackInfo.setReturnValue("skill.fatesupport.knight_a10");
        }
    }

    @Inject(method = "triggerAction", at = @At("HEAD"), cancellable = true)
    private static void fatesupport$blockActions(ServerPlayer player, int slot, CallbackInfoReturnable<Boolean> callbackInfo) {
        if (SpiritualizationService.isRestricted(player)) {
            callbackInfo.setReturnValue(false);
            return;
        }
        if (slot == 6 && ServantCardLancelotBerserkerSkills.isActiveCard(player)) {
            callbackInfo.setReturnValue(KnightArmoryService.open(player));
            return;
        }

        if (slot == 7 && ServantCardLancelotBerserkerSkills.isActiveCard(player)) {
            callbackInfo.setReturnValue(KnightArmoryService.summonA10Skill(player));
            return;
        }
        if (slot == 8 && ServantCardLancelotBerserkerSkills.isActiveCard(player)
                && !ServantCardLancelotBerserkerSkills.isAroundightActive(player)) {
            int vehicleResult = SuperbWarfareLoader.beginVehicleOwnerization(player);
            if (vehicleResult != 0) {
                callbackInfo.setReturnValue(vehicleResult > 0);
            }
        }
    }

    @Inject(method = "handleHoldAction", at = @At("HEAD"), cancellable = true)
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
