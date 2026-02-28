package net.xxxjk.TYPE_MOON_WORLD.magic.jewel;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.xxxjk.TYPE_MOON_WORLD.constants.MagicConstants;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.FullManaCarvedGemItem;

public class MagicJewelShoot {
    public static void execute(Entity entity) {
        if (!(entity instanceof ServerPlayer player)) return;
        if (tryUseHeldFullGem(player)) return;
        player.displayClientMessage(Component.translatable(MagicConstants.MSG_MAGIC_ANY_GEM_NEED), true);
    }

    static boolean tryUseHeldFullGem(ServerPlayer player) {
        ItemStack main = player.getMainHandItem();
        if (main.getItem() instanceof FullManaCarvedGemItem fullGem) {
            GemUseService.useFilledGem(player.level(), player, InteractionHand.MAIN_HAND, main, fullGem.getType());
            return true;
        }

        ItemStack off = player.getOffhandItem();
        if (off.getItem() instanceof FullManaCarvedGemItem fullGem) {
            GemUseService.useFilledGem(player.level(), player, InteractionHand.OFF_HAND, off, fullGem.getType());
            return true;
        }
        return false;
    }
}
