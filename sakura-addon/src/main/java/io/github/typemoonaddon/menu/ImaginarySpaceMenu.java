package io.github.typemoonaddon.menu;

import io.github.typemoonaddon.config.GameplayConfig;
import io.github.typemoonaddon.data.ImaginarySpaceContainer;
import io.github.typemoonaddon.registry.ModMenus;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;

public final class ImaginarySpaceMenu extends AbstractContainerMenu {
    public static final int PREVIOUS_PAGE_BUTTON = 0;
    public static final int NEXT_PAGE_BUTTON = 1;

    private static final int SPACE_ROWS = 6;
    private static final int SPACE_COLUMNS = 9;
    private static final int SPACE_SLOTS = SPACE_ROWS * SPACE_COLUMNS;
    private static final int PLAYER_SLOTS = 36;

    private final Container space;
    private final DataSlot currentPage = DataSlot.standalone();
    private final DataSlot pageCount = DataSlot.standalone();

    public ImaginarySpaceMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(GameplayConfig.MENU_CAPACITY));
    }

    public ImaginarySpaceMenu(int containerId, Inventory playerInventory, ServerPlayer owner) {
        this(containerId, playerInventory, new ImaginarySpaceContainer(owner));
    }

    private ImaginarySpaceMenu(int containerId, Inventory playerInventory, Container space) {
        super(ModMenus.IMAGINARY_SPACE.get(), containerId);
        checkContainerSize(space, SPACE_SLOTS);
        this.space = space;
        space.startOpen(playerInventory.player);
        if (space instanceof ImaginarySpaceContainer pagedSpace) {
            currentPage.set(pagedSpace.page());
            pageCount.set(pagedSpace.pageCount());
        } else {
            currentPage.set(0);
            pageCount.set(1);
        }
        addDataSlot(currentPage);
        addDataSlot(pageCount);

        for (int row = 0; row < SPACE_ROWS; row++) {
            for (int column = 0; column < SPACE_COLUMNS; column++) {
                int slot = column + row * SPACE_COLUMNS;
                addSlot(new SpaceSlot(space, slot, 8 + column * 18, 18 + row * 18));
            }
        }

        int inventoryY = 140;
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, inventoryY + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, 8 + column * 18, inventoryY + 58));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return result;
        }

        ItemStack source = slot.getItem();
        result = source.copy();
        if (index < SPACE_SLOTS) {
            if (!moveItemStackTo(source, SPACE_SLOTS, SPACE_SLOTS + PLAYER_SLOTS, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            // Items can only enter through a validated absorption transaction.
            return ItemStack.EMPTY;
        }

        if (source.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        slot.onTake(player, source);
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return space.stillValid(player);
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        if (!(space instanceof ImaginarySpaceContainer pagedSpace) || !stillValid(player)) {
            return false;
        }

        int requestedPage = switch (buttonId) {
            case PREVIOUS_PAGE_BUTTON -> pagedSpace.page() - 1;
            case NEXT_PAGE_BUTTON -> pagedSpace.page() + 1;
            default -> -1;
        };
        if (!pagedSpace.selectPage(requestedPage)) {
            return false;
        }

        currentPage.set(pagedSpace.page());
        pageCount.set(pagedSpace.pageCount());
        broadcastFullState();
        return true;
    }

    @Override
    public void broadcastChanges() {
        if (space instanceof ImaginarySpaceContainer pagedSpace) {
            currentPage.set(pagedSpace.page());
            pageCount.set(pagedSpace.pageCount());
        }
        super.broadcastChanges();
    }

    public int currentPage() {
        return currentPage.get();
    }

    public int pageCount() {
        return pageCount.get();
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        space.stopOpen(player);
    }

    private static final class SpaceSlot extends Slot {
        private SpaceSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}
