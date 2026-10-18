package com.captain.servant.mixin;

import com.captain.servant.emiya.EmiyaSupportService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.NamelessBowItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = NamelessBowItem.class, remap = false)
public abstract class EmiyaNamelessBowMixin {
    @Inject(method = "releaseUsing", at = @At("HEAD"), cancellable = true)
    private void fatesupport$shootNormalArrowForKanshouBakuya(
            ItemStack bow,
            Level level,
            LivingEntity user,
            int remainingUseTicks,
            CallbackInfo callbackInfo
    ) {
        if (!(user instanceof ServerPlayer player) || !EmiyaSupportService.shouldUseNormalProjection(player)) {
            return;
        }

        ItemStack offhand = player.getOffhandItem();
        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        try {
            ((NamelessBowItem) (Object) this).releaseUsing(bow, level, user, remainingUseTicks);
        } finally {
            player.setItemInHand(InteractionHand.OFF_HAND, offhand);
        }
        callbackInfo.cancel();
    }
}
