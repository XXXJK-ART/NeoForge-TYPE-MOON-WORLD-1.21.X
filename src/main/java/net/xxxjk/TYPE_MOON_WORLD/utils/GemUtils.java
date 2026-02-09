package net.xxxjk.TYPE_MOON_WORLD.utils;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.FullManaCarvedGemItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.GemType;

public class GemUtils {
    public static ItemStack consumeGem(Player player, GemType type) {
        // Check main inventory
        for (int i = 0; i < player.getInventory().items.size(); i++) {
            ItemStack stack = player.getInventory().items.get(i);
            if (isCorrectGem(stack, type)) {
                ItemStack result = stack.copy();
                result.setCount(1);
                stack.shrink(1);
                if (stack.isEmpty()) {
                     player.getInventory().removeItem(stack);
                }
                return result;
            }
        }
        // Check offhand
        ItemStack offhand = player.getOffhandItem();
        if (isCorrectGem(offhand, type)) {
            ItemStack result = offhand.copy();
            result.setCount(1);
            offhand.shrink(1);
            return result;
        }
        return ItemStack.EMPTY;
    }

    private static boolean isCorrectGem(ItemStack stack, GemType type) {
        if (stack.getItem() instanceof FullManaCarvedGemItem gemItem) {
            return gemItem.getType() == type;
        }
        return false;
    }
}
