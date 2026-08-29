package io.github.typemoonaddon.magic;

import io.github.typemoonaddon.config.GameplayConfig;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

/**
 * Public ProjectionRegistry APIs expose projection execution hooks but no cost
 * lookup. This addon therefore owns a transparent, configurable approximation
 * instead of copying Type Moon World's private projection algorithm.
 */
public final class ProjectionManaCostService {
    public static double cost(ItemStack stack) {
        if (stack.isEmpty()) {
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
        double raw = stack.getCount() * rarity * density + durability + enchantment;
        return Math.max(1.0D, Math.ceil(raw * GameplayConfig.MANA_COST_MULTIPLIER.get()));
    }

    private ProjectionManaCostService() {
    }
}
