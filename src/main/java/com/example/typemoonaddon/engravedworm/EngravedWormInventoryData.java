package com.example.typemoonaddon.engravedworm;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.INBTSerializable;
import org.jetbrains.annotations.NotNull;

public final class EngravedWormInventoryData implements INBTSerializable<CompoundTag> {
    private static final String WORMS_KEY = "EngravedWorms";
    private static final String APPLIED_MANA_BONUS_KEY = "AppliedManaBonus";

    private final List<ItemStack> worms = new ArrayList<>();
    private double appliedManaBonus;

    public int size() {
        return worms.size();
    }

    public ItemStack get(int slot) {
        return slot >= 0 && slot < worms.size() ? worms.get(slot) : ItemStack.EMPTY;
    }

    public void set(int slot, ItemStack stack) {
        if (slot < 0) {
            return;
        }
        while (worms.size() <= slot) {
            worms.add(ItemStack.EMPTY);
        }
        ItemStack single = stack == null || stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
        worms.set(slot, single);
        trimTrailingEmptySlots();
    }

    public double getAppliedManaBonus() {
        return Math.max(0.0D, appliedManaBonus);
    }

    public void clear() {
        worms.clear();
        appliedManaBonus = 0.0D;
    }

    public void setAppliedManaBonus(double value) {
        appliedManaBonus = Double.isFinite(value) ? Math.max(0.0D, value) : 0.0D;
    }

    @Override
    public @NotNull CompoundTag serializeNBT(@NotNull HolderLookup.Provider provider) {
        CompoundTag root = new CompoundTag();
        ListTag serialized = new ListTag();
        for (ItemStack worm : worms) {
            CompoundTag entry = new CompoundTag();
            if (!worm.isEmpty()) {
                entry.put("Stack", worm.save(provider));
            }
            serialized.add(entry);
        }
        root.put(WORMS_KEY, serialized);
        root.putDouble(APPLIED_MANA_BONUS_KEY, getAppliedManaBonus());
        return root;
    }

    @Override
    public void deserializeNBT(@NotNull HolderLookup.Provider provider, @NotNull CompoundTag root) {
        worms.clear();
        if (root.contains(WORMS_KEY, Tag.TAG_LIST)) {
            ListTag serialized = root.getList(WORMS_KEY, Tag.TAG_COMPOUND);
            for (int i = 0; i < serialized.size(); i++) {
                CompoundTag entry = serialized.getCompound(i);
                ItemStack stack = entry.contains("Stack", Tag.TAG_COMPOUND)
                        ? ItemStack.parse(provider, entry.getCompound("Stack")).orElse(ItemStack.EMPTY)
                        : ItemStack.EMPTY;
                worms.add(stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1));
            }
        }
        setAppliedManaBonus(root.getDouble(APPLIED_MANA_BONUS_KEY));
        trimTrailingEmptySlots();
    }

    private void trimTrailingEmptySlots() {
        for (int i = worms.size() - 1; i >= 0 && worms.get(i).isEmpty(); i--) {
            worms.remove(i);
        }
    }
}
