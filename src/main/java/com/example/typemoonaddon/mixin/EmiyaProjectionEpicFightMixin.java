package com.example.typemoonaddon.mixin;

import com.example.typemoonaddon.compat.EpicFightBridge;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Prevents Gallatin and the elemental sword from consuming Epic Fight's right-click. */
@Mixin(targets = "net.xxxjk.TYPE_MOON_WORLD.item.custom.EmiyaProjectionItem", remap = false)
public abstract class EmiyaProjectionEpicFightMixin {
    @Shadow(remap = false)
    public abstract String projectionId();

    @Inject(method = "use", at = @At("HEAD"), cancellable = true, remap = false)
    private void typemoonaddon$blockNoblePhantasmInBattleMode(
        Level level,
        Player player,
        InteractionHand hand,
        CallbackInfoReturnable<InteractionResultHolder<ItemStack>> callback
    ) {
        String projectionId = this.projectionId();
        if (("excalibur_gallatin".equals(projectionId) || "paracelsus_sword".equals(projectionId))
            && EpicFightBridge.isBattleMode(player)) {
            callback.setReturnValue(InteractionResultHolder.pass(player.getItemInHand(hand)));
        }
    }
}
