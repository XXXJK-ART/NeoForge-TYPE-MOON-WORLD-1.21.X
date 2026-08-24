package com.example.typemoonaddon.block.entity;

import com.example.typemoonaddon.registry.AddonItems;
import com.example.typemoonaddon.worm.WormStackData;
import com.example.typemoonaddon.worm.WormType;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.xxxjk.TYPE_MOON_WORLD.world.inventory.WormWarehouseMenu;
import org.jetbrains.annotations.NotNull;

public final class WormWarehouseBlockEntity extends BlockEntity implements MenuProvider {
    private static final String KEY_ITEMS = "Items";
    private static final String KEY_CAPACITY = "Capacity";
    private static final String KEY_MIN_X = "MinX";
    private static final String KEY_MAX_X = "MaxX";
    private static final String KEY_MIN_Z = "MinZ";
    private static final String KEY_MAX_Z = "MaxZ";
    private static final int FEED_DRAW_COUNT = 3;
    private static final int GU_GAIN_PER_FEED = 1;
    private static final int MAX_GROWTH_ROLL_PERCENT = 30;
    private static final int SPECIAL_MUTATION_MIN_GU = 50;
    private static final int SPECIAL_MUTATION_ROLL_PERCENT = 8;
    private static final int DETECTION_MUTATION_WEIGHT = 1;
    private static final int FIREPROOF_MUTATION_WEIGHT = 1;
    private final ItemStackHandler items = new ItemStackHandler(com.example.typemoonaddon.worm.WormWarehouseService.MAX_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return stack.getItem() == AddonItems.WORM.get();
        }
    };
    private int capacity = 1;
    private int minX;
    private int maxX;
    private int minZ;
    private int maxZ;

    public WormWarehouseBlockEntity(BlockPos pos, BlockState state) {
        super((BlockEntityType<?>) AddonBlockEntities.WORM_WAREHOUSE.get(), pos, state);
    }

    public ItemStackHandler getItems() {
        return items;
    }

    public void setCapacity(int capacity) {
        this.capacity = Math.max(1, Math.min(com.example.typemoonaddon.worm.WormWarehouseService.MAX_SLOTS, capacity));
    }

    public void setBounds(int minX, int maxX, int minZ, int maxZ) {
        this.minX = minX;
        this.maxX = maxX;
        this.minZ = minZ;
        this.maxZ = maxZ;
        setChanged();
    }

    @Nullable
    public com.example.typemoonaddon.worm.WormWarehouseService.Bounds getBounds() {
        if (minX >= maxX || minZ >= maxZ) {
            return null;
        }
        return new com.example.typemoonaddon.worm.WormWarehouseService.Bounds(worldPosition.getY(), minX, maxX, minZ, maxZ);
    }

    public int getCapacity() {
        return capacity;
    }

    public boolean isEmpty() {
        for (int i = 0; i < items.getSlots(); i++) {
            if (!items.getStackInSlot(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public int getOccupiedSlots() {
        int occupiedSlots = 0;
        for (int i = 0; i < items.getSlots(); i++) {
            if (!items.getStackInSlot(i).isEmpty()) {
                occupiedSlots++;
            }
        }
        return occupiedSlots;
    }

    public void clearInventory() {
        for (int i = 0; i < items.getSlots(); i++) {
            items.setStackInSlot(i, ItemStack.EMPTY);
        }
    }

    public void dropContents(ServerLevel level) {
        for (int i = 0; i < items.getSlots(); i++) {
            ItemStack stack = items.getStackInSlot(i);
            if (!stack.isEmpty()) {
                net.minecraft.world.Containers.dropItemStack(
                        level,
                        worldPosition.getX() + 0.5D,
                        worldPosition.getY() + 0.5D,
                        worldPosition.getZ() + 0.5D,
                        stack.copy()
                );
                items.setStackInSlot(i, ItemStack.EMPTY);
            }
        }
    }

    public void consumeGrowth(ServerLevel level, BlockPos pos, int capacity) {
        if (capacity <= 0) {
            return;
        }
        List<Integer> candidates = new ArrayList<>();
        for (int slot = 0; slot < Math.min(capacity, items.getSlots()); slot++) {
            if (!items.getStackInSlot(slot).isEmpty()) {
                candidates.add(slot);
            }
        }
        if (candidates.isEmpty()) {
            return;
        }
        int attempts = Math.min(FEED_DRAW_COUNT, candidates.size());
        for (int i = 0; i < attempts; i++) {
            int slot = candidates.remove(level.getRandom().nextInt(candidates.size()));
            ItemStack stack = items.getStackInSlot(slot);
            if (!stack.isEmpty() && stack.getItem() == AddonItems.WORM.get()) {
                int gu = WormStackData.gu(stack);
                if (level.getRandom().nextInt(100) >= Math.min(MAX_GROWTH_ROLL_PERCENT, gu)) {
                    continue;
                }
                WormStackData.set(stack, mutatedType(level, WormStackData.type(stack), gu),
                        gu + GU_GAIN_PER_FEED, WormStackData.owner(stack));
            }
        }
    }

    private static WormType mutatedType(ServerLevel level, WormType current, int gu) {
        if (current == WormType.FIREPROOF || current == WormType.DETECTION || gu < SPECIAL_MUTATION_MIN_GU
                || level.getRandom().nextInt(100) >= SPECIAL_MUTATION_ROLL_PERCENT) {
            return current;
        }
        int totalWeight = Math.max(1, DETECTION_MUTATION_WEIGHT + FIREPROOF_MUTATION_WEIGHT);
        return level.getRandom().nextInt(totalWeight) < DETECTION_MUTATION_WEIGHT
                ? WormType.DETECTION
                : WormType.FIREPROOF;
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, @NotNull Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put(KEY_ITEMS, items.serializeNBT(registries));
        tag.putInt(KEY_CAPACITY, capacity);
        tag.putInt(KEY_MIN_X, minX);
        tag.putInt(KEY_MAX_X, maxX);
        tag.putInt(KEY_MIN_Z, minZ);
        tag.putInt(KEY_MAX_Z, maxZ);
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, @NotNull Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains(KEY_ITEMS)) {
            items.deserializeNBT(registries, tag.getCompound(KEY_ITEMS));
        }
        capacity = Math.max(1, Math.min(com.example.typemoonaddon.worm.WormWarehouseService.MAX_SLOTS, tag.getInt(KEY_CAPACITY)));
        minX = tag.getInt(KEY_MIN_X);
        maxX = tag.getInt(KEY_MAX_X);
        minZ = tag.getInt(KEY_MIN_Z);
        maxZ = tag.getInt(KEY_MAX_Z);
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("menu.typemoonworld.worm_warehouse");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, @NotNull Inventory playerInventory, @NotNull Player player) {
        return new WormWarehouseMenu(containerId, playerInventory, this);
    }
}
