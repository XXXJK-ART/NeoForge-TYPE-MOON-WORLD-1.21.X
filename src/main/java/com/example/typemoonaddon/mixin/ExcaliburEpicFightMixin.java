package com.example.typemoonaddon.mixin;

import com.example.typemoonaddon.compat.EpicFightBridge;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Leaves right-click available to Epic Fight instead of starting Excalibur's charge. */
@Mixin(targets = "net.xxxjk.TYPE_MOON_WORLD.item.custom.ExcaliburItem", remap = false)
public abstract class ExcaliburEpicFightMixin {
    @Inject(method = "use", at = @At("HEAD"), cancellable = true, remap = false)
    private void typemoonaddon$blockNoblePhantasmInBattleMode(
        Level level,
        Player player,
        InteractionHand hand,
        CallbackInfoReturnable<InteractionResultHolder<ItemStack>> callback
    ) {
        if (EpicFightBridge.isBattleMode(player)) {
            callback.setReturnValue(InteractionResultHolder.pass(player.getItemInHand(hand)));
        }
    }
}
