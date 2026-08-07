package com.example.typemoonaddon.storage;

import com.example.typemoonaddon.registry.AddonMenus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

public final class StorageMenu extends AbstractContainerMenu {
    private static final int STORAGE_START = 0;
    private static final int STORAGE_END = StorageData.SLOT_COUNT;
    private static final int PLAYER_INV_START = STORAGE_END;
    private static final int PLAYER_INV_END = PLAYER_INV_START + 27;
    private static final int HOTBAR_START = PLAYER_INV_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final Player owner;

    public StorageMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf ignored) {
        this(containerId, playerInventory);
    }

    public StorageMenu(int containerId, Inventory playerInventory) {
        super(AddonMenus.STORAGE.get(), containerId);
        this.owner = playerInventory.player;
        StorageData storage = owner.getData(StorageAttachments.PLAYER_STORAGE);

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                int slot = column + row * 9;
                addSlot(new SlotItemHandler(storage.getItems(), slot, 8 + column * 18, 18 + row * 18));
            }
        }

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, 85 + row * 18));
            }
        }

        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, 8 + column * 18, 143));
        }
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return player == owner && player.isAlive() && !player.isRemoved();
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        if (index < 0 || index >= slots.size()) {
            return ItemStack.EMPTY;
        }

        Slot sourceSlot = slots.get(index);
        if (!sourceSlot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack original = sourceStack.copy();
        if (index < STORAGE_END) {
            if (!moveItemStackTo(sourceStack, PLAYER_INV_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(sourceStack, STORAGE_START, STORAGE_END, false)) {
            return ItemStack.EMPTY;
        }

        if (sourceStack.isEmpty()) {
            sourceSlot.setByPlayer(ItemStack.EMPTY);
        } else {
            sourceSlot.setChanged();
        }

        if (sourceStack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }
        sourceSlot.onTake(player, sourceStack);
        return original;
    }
}
