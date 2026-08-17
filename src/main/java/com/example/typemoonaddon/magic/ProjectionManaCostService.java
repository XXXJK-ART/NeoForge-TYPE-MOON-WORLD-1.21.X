package com.example.typemoonaddon.magic;

import net.minecraft.world.item.ItemStack;

public final class ProjectionManaCostService {
    private ProjectionManaCostService() {
    }

    public static double cost(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0.0D;
        }
        double rarity = switch (stack.getRarity()) {
            case COMMON -> 1.0D;
            case UNCOMMON -> 1.5D;
            case RARE -> 2.5D;
            case EPIC -> 4.0D;
        };
        double density = Math.max(1.0D, 64.0D / stack.getMaxStackSize());
        double durability = stack.isDamageableItem() ? 4.0D : 0.0D;
        double enchantment = stack.isEnchanted() ? 6.0D : 0.0D;
        return Math.max(1.0D, Math.ceil((stack.getCount() * rarity * density + durability + enchantment)));
    }
}
