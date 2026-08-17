package com.example.typemoonaddon.mixin;

import com.example.typemoonaddon.magic.MatouSakuraMasterProfile;
import com.example.typemoonaddon.registry.AddonItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Restores addon state and the dedicated card item when Sakura's profile is released. */
@Mixin(targets = "net.xxxjk.TYPE_MOON_WORLD.servant.card.MasterCardProfile", remap = false)
public abstract class MasterCardProfileMixin {
    @Inject(method = "restoreOriginalState", at = @At("HEAD"), remap = false)
    private static void typemoonaddon$restoreSakuraState(
        ServerPlayer player,
        TypeMoonWorldModVariables.PlayerVariables variables,
        CallbackInfoReturnable<Boolean> callback
    ) {
        if (MatouSakuraMasterProfile.variant(variables.master_card_id) != null) {
            MatouSakuraMasterProfile.restoreAddonState(player);
        }
    }

    @Inject(method = "createCardStack", at = @At("HEAD"), cancellable = true, remap = false)
    private static void typemoonaddon$restoreSakuraCard(
        String masterId,
        CallbackInfoReturnable<ItemStack> callback
    ) {
        MatouSakuraMasterProfile.Variant variant = MatouSakuraMasterProfile.variant(masterId);
        if (variant == null) {
            return;
        }
        callback.setReturnValue(new ItemStack(switch (variant) {
            case STAY_NIGHT -> AddonItems.MASTER_CARD_MATOU_SAKURA.get();
            case ALTER -> AddonItems.MASTER_CARD_MATOU_SAKURA_ALTER.get();
            case FHA -> AddonItems.MASTER_CARD_MATOU_SAKURA_FHA.get();
        }));
    }
}
