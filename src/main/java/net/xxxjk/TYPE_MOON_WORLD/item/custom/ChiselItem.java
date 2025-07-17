package net.xxxjk.TYPE_MOON_WORLD.item.custom;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class ChiselItem extends Item {
    public ChiselItem(Properties properties) {
        super(properties);
    }

    public boolean hasCraftingRemainingItem(@NotNull ItemStack stack) {
        return true;
    }

    public @NotNull ItemStack getCraftingRemainingItem(ItemStack itemstack) {
        ItemStack retrieval = new ItemStack(this);
        retrieval.setDamageValue(itemstack.getDamageValue() + 1);
        return retrieval.getDamageValue() >= retrieval.getMaxDamage() ? ItemStack.EMPTY : retrieval;
    }

    public boolean isRepairable(@NotNull ItemStack itemstack) {
        return false;
    }
}
