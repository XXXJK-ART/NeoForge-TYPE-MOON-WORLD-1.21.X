package com.example.typemoonaddon.mixin;

import com.example.typemoonaddon.magic.MatouKariyaMasterProfile;
import com.example.typemoonaddon.registry.AddonItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Restores addon state and the dedicated card item when Kariya's profile is released. */
@Mixin(targets = "net.xxxjk.TYPE_MOON_WORLD.servant.card.MasterCardProfile", remap = false)
public abstract class MasterCardProfileMixin {
    @Inject(method = "restoreOriginalState", at = @At("HEAD"), remap = false)
    private static void typemoonaddon$restoreKariyaState(
        ServerPlayer player,
        TypeMoonWorldModVariables.PlayerVariables variables,
        CallbackInfoReturnable<Boolean> callback
    ) {
        if (MatouKariyaMasterProfile.is(variables.master_card_id)) {
            MatouKariyaMasterProfile.restore(player);
        }
    }

    @Inject(method = "createCardStack", at = @At("HEAD"), cancellable = true, remap = false)
    private static void typemoonaddon$restoreKariyaCard(
        String masterId,
        CallbackInfoReturnable<ItemStack> callback
    ) {
        if (MatouKariyaMasterProfile.is(masterId)) {
            callback.setReturnValue(new ItemStack(AddonItems.MASTER_CARD_MATOU_KARIYA.get()));
        }
    }
}
