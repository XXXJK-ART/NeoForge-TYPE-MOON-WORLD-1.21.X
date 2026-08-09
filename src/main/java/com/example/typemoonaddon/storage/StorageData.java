package com.example.typemoonaddon.storage;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.INBTSerializable;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

public final class StorageData implements INBTSerializable<CompoundTag> {
    public static final int MANA_MODE = 0;
    public static final int ITEM_MODE = 1;
    public static final int SLOT_COUNT = 27;
    public static final double MANA_CAPACITY = 10_000.0D;

    private static final String MODE_KEY = "Mode";
    private static final String MANA_KEY = "StoredMana";
    private static final String ITEMS_KEY = "Items";

    private int mode;
    private double storedMana;
    private final ItemStackHandler items = new ItemStackHandler(SLOT_COUNT);

    public int getMode() {
        return mode;
    }

    public void setMode(int mode) {
        this.mode = mode == ITEM_MODE ? ITEM_MODE : MANA_MODE;
    }

    public int cycleMode() {
        setMode(mode == MANA_MODE ? ITEM_MODE : MANA_MODE);
        return mode;
    }

    public double getStoredMana() {
        storedMana = sanitizeMana(storedMana);
        return storedMana;
    }

    public void setStoredMana(double storedMana) {
        this.storedMana = sanitizeMana(storedMana);
    }

    public ItemStackHandler getItems() {
        return items;
    }

    @Override
    public @NotNull CompoundTag serializeNBT(@NotNull HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putInt(MODE_KEY, getMode());
        tag.putDouble(MANA_KEY, getStoredMana());
        tag.put(ITEMS_KEY, items.serializeNBT(provider));
        return tag;
    }

    @Override
    public void deserializeNBT(@NotNull HolderLookup.Provider provider, @NotNull CompoundTag tag) {
        setMode(tag.contains(MODE_KEY, Tag.TAG_INT) ? tag.getInt(MODE_KEY) : MANA_MODE);
        setStoredMana(tag.contains(MANA_KEY, Tag.TAG_DOUBLE) ? tag.getDouble(MANA_KEY) : 0.0D);

        if (!tag.contains(ITEMS_KEY, Tag.TAG_COMPOUND)) {
            return;
        }

        CompoundTag itemTag = tag.getCompound(ITEMS_KEY).copy();
        itemTag.putInt("Size", SLOT_COUNT);
        try {
            items.deserializeNBT(provider, itemTag);
        } catch (RuntimeException ignored) {
            items.setSize(SLOT_COUNT);
        }
        for (int slot = 0; slot < items.getSlots(); slot++) {
            ItemStack stack = items.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                stack.limitSize(stack.getMaxStackSize());
            }
        }
    }

    private static double sanitizeMana(double value) {
        if (!Double.isFinite(value)) {
            return 0.0D;
        }
        return Math.max(0.0D, Math.min(MANA_CAPACITY, value));
    }
}
