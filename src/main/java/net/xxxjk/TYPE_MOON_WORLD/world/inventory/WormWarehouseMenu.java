package net.xxxjk.TYPE_MOON_WORLD.world.inventory;

import com.example.typemoonaddon.block.entity.WormWarehouseBlockEntity;
import com.example.typemoonaddon.registry.AddonMenus;
import com.example.typemoonaddon.worm.WormWarehouseService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

public final class WormWarehouseMenu extends AbstractContainerMenu {
    public static final int PAGE_SIZE = WormWarehouseService.PAGE_SIZE;
    private static final int PLAYER_START = WormWarehouseService.MAX_SLOTS;
    private final Player owner;
    private final WormWarehouseBlockEntity blockEntity;
    private final ContainerData data;
    private int page;
    private int clientCapacity = 1;

    public WormWarehouseMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buffer) {
        this(containerId, playerInventory, resolve(buffer, playerInventory));
    }

    private static WormWarehouseBlockEntity resolve(FriendlyByteBuf buffer, Inventory playerInventory) {
        BlockPos pos = buffer.readBlockPos();
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        if (!(blockEntity instanceof WormWarehouseBlockEntity warehouse)) {
            throw new IllegalStateException("Worm warehouse block entity missing");
        }
        return warehouse;
    }

    public WormWarehouseMenu(int containerId, Inventory playerInventory, WormWarehouseBlockEntity blockEntity) {
        super(AddonMenus.WORM_WAREHOUSE.get(), containerId);
        this.owner = playerInventory.player;
        this.blockEntity = blockEntity;
        this.data = new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> page;
                    case 1 -> owner.level().isClientSide ? clientCapacity : WormWarehouseMenu.this.blockEntity.getCapacity();
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                if (index == 0) {
                    page = value;
                } else if (index == 1) {
                    clientCapacity = value;
                }
            }

            @Override
            public int getCount() {
                return 2;
            }
        };
        addDataSlots(data);
        buildSlots(playerInventory);
    }

    private void buildSlots(Inventory playerInventory) {
        int columns = 9;
        for (int slot = 0; slot < WormWarehouseService.MAX_SLOTS; slot++) {
            final int slotIndex = slot;
            final int local = slotIndex % PAGE_SIZE;
            final int pageIndex = slotIndex / PAGE_SIZE;
            int x = 8 + (local % columns) * 18;
            int y = 18 + (local / columns) * 18;
            addSlot(new SlotItemHandler(blockEntity.getItems(), slotIndex, x, y) {
                @Override
                public boolean isActive() {
                    return WormWarehouseMenu.this.getPage() == pageIndex && slotIndex < WormWarehouseMenu.this.getCapacity();
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }
            });
        }

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, 148 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, 8 + column * 18, 206));
        }
    }

    public void setPage(int page) {
        data.set(0, Math.max(0, Math.min(getMaxPage(), page)));
    }

    public int getPage() {
        return Math.max(0, Math.min(getMaxPage(), data.get(0)));
    }

    public int getMaxPage() {
        return Math.max(0, (getCapacity() - 1) / PAGE_SIZE);
    }

    public int getCapacity() {
        return Math.max(1, Math.min(WormWarehouseService.MAX_SLOTS, data.get(1)));
    }

    public int getOccupiedSlots() {
        return blockEntity.getOccupiedSlots();
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return player == owner && owner.isAlive() && !owner.isRemoved();
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        if (index < 0 || index >= slots.size()) {
            return ItemStack.EMPTY;
        }
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < WormWarehouseService.MAX_SLOTS) {
            if (!moveItemStackTo(stack, PLAYER_START, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, WormWarehouseService.MAX_SLOTS, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == copy.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return copy;
    }
}
