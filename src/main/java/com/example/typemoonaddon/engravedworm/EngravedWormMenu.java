package com.example.typemoonaddon.engravedworm;

import com.example.typemoonaddon.registry.AddonItems;
import com.example.typemoonaddon.registry.AddonMenus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public final class EngravedWormMenu extends AbstractContainerMenu {
    public static final int GRID_X = 8;
    public static final int WORM_GRID_Y = 52;
    public static final int PLAYER_INV_Y = 182;
    public static final int HOTBAR_Y = 240;
    private static final int PLAYER_START = EngravedWormService.PAGE_SIZE;
    private final Player owner;
    private final SimpleContainer wormSlots = new SimpleContainer(EngravedWormService.PAGE_SIZE);
    private int page;

    public EngravedWormMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, inventory, Math.max(0, buffer.readVarInt()));
    }

    public EngravedWormMenu(int containerId, Inventory inventory, int page) {
        super(AddonMenus.ENGRAVED_WORMS.get(), containerId);
        this.owner = inventory.player;
        this.page = Math.max(0, page);
        buildSlots(inventory);
        updateWormSlotsFromAttachment();
    }

    private void buildSlots(Inventory inventory) {
        for (int slot = 0; slot < EngravedWormService.PAGE_SIZE; slot++) {
            int localSlot = slot;
            addSlot(new Slot(wormSlots, slot, GRID_X + (slot % 9) * 18, WORM_GRID_Y + (slot / 9) * 18) {
                @Override
                public void set(@NotNull ItemStack stack) {
                    if (owner instanceof ServerPlayer player) {
                        if (stack.isEmpty()) {
                            EngravedWormService.remove(player, getAbsoluteSlot());
                        } else if (stack.is(AddonItems.ENGRAVED_WORM.get()) && EngravedWormData.owner(stack) != null) {
                            player.getData(EngravedWormAttachments.INVENTORY.get()).set(getAbsoluteSlot(), stack);
                            EngravedWormService.recalculate(player);
                        }
                        updateWormSlotsFromAttachment();
                    }
                    setChanged();
                }

                @Override
                public ItemStack remove(int amount) {
                    if (!(owner instanceof ServerPlayer player)) {
                        return ItemStack.EMPTY;
                    }
                    ItemStack removed = EngravedWormService.remove(player, getAbsoluteSlot());
                    updateWormSlotsFromAttachment();
                    return removed.isEmpty() ? ItemStack.EMPTY : removed.copyWithCount(Math.min(amount, removed.getCount()));
                }

                @Override
                public boolean mayPlace(@NotNull ItemStack stack) {
                    return stack.is(AddonItems.ENGRAVED_WORM.get())
                            && EngravedWormData.owner(stack) != null
                            && getItem().isEmpty();
                }

                @Override
                public boolean mayPickup(Player player) {
                    ItemStack stack = getItem();
                    return !stack.isEmpty()
                            && (!(player instanceof ServerPlayer serverPlayer)
                            || EngravedWormData.isBoundTo(stack, serverPlayer.getUUID()));
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }

                private int getAbsoluteSlot() {
                    return EngravedWormMenu.this.page * EngravedWormService.PAGE_SIZE + localSlot;
                }
            });
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, GRID_X + column * 18, PLAYER_INV_Y + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, GRID_X + column * 18, HOTBAR_Y));
        }
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        if (owner instanceof ServerPlayer player) {
            this.page = Math.max(0, Math.min(EngravedWormService.maxPage(player), page));
            updateWormSlotsFromAttachment();
            broadcastChanges();
        }
    }

    public int getMaxPage() {
        return owner instanceof ServerPlayer player ? EngravedWormService.maxPage(player) : 0;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return player == owner && player.isAlive() && !player.isRemoved();
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        if (index < 0 || index >= slots.size() || !(player instanceof ServerPlayer serverPlayer)) {
            return ItemStack.EMPTY;
        }
        Slot source = slots.get(index);
        if (!source.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = source.getItem();
        ItemStack copy = stack.copy();
        if (index < EngravedWormService.PAGE_SIZE) {
            if (!moveItemStackTo(stack, PLAYER_START, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.is(AddonItems.ENGRAVED_WORM.get()) && EngravedWormData.owner(stack) != null) {
            int target = firstEmptySlot(serverPlayer);
            if (target < 0 || !EngravedWormService.insert(serverPlayer, target, stack.copyWithCount(1))) {
                return ItemStack.EMPTY;
            }
            updateWormSlotsFromAttachment();
            stack.shrink(1);
        } else {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            source.setByPlayer(ItemStack.EMPTY);
        } else {
            source.setChanged();
        }
        return copy;
    }

    private static int firstEmptySlot(ServerPlayer player) {
        EngravedWormInventoryData data = player.getData(EngravedWormAttachments.INVENTORY.get());
        for (int i = 0; i < data.size(); i++) {
            if (data.get(i).isEmpty()) {
                return i;
            }
        }
        return data.size();
    }

    private void updateWormSlotsFromAttachment() {
        if (!(owner instanceof ServerPlayer player)) {
            return;
        }
        EngravedWormInventoryData data = player.getData(EngravedWormAttachments.INVENTORY.get());
        for (int slot = 0; slot < EngravedWormService.PAGE_SIZE; slot++) {
            ItemStack stack = data.get(page * EngravedWormService.PAGE_SIZE + slot);
            wormSlots.setItem(slot, stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1));
        }
    }
}
