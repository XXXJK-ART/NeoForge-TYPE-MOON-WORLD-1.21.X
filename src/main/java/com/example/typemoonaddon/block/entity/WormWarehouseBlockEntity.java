package com.example.typemoonaddon.block.entity;

import com.example.typemoonaddon.worm.WormStackData;
import com.example.typemoonaddon.worm.WormType;
import com.example.typemoonaddon.worm.WormCaptureService;
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
            return slot >= 0 && slot < capacity && WormStackData.isWormItem(stack);
        }

        @Override
        public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            if (!isItemValid(slot, stack) || stack.isEmpty() || getStackInSlot(slot).getCount() >= 1) {
                return stack;
            }
            ItemStack single = stack.copyWithCount(1);
            ItemStack remainder = super.insertItem(slot, single, simulate);
            if (!remainder.isEmpty()) {
                return stack;
            }
            return stack.getCount() == 1 ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - 1);
        }

        @Override
        public void setStackInSlot(int slot, @NotNull ItemStack stack) {
            super.setStackInSlot(slot, stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1));
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
        int width = getBounds() == null ? 20 : Math.max(1, getBounds().width() - 2);
        for (int i = 0; i < items.getSlots(); i++) {
            ItemStack stack = items.getStackInSlot(i);
            if (!stack.isEmpty()) {
                int slotX = i % width;
                int slotZ = i / width;
                double dropX = (getBounds() == null ? worldPosition.getX() : getBounds().minX() + 1 + slotX) + 0.5D;
                double dropZ = (getBounds() == null ? worldPosition.getZ() : getBounds().minZ() + 1 + slotZ) + 0.5D;
                int count = stack.getCount();
                for (int worm = 0; worm < count; worm++) {
                    WormCaptureService.release(level, null, stack.copyWithCount(1),
                            dropX, worldPosition.getY() + 0.1D, dropZ);
                }
                items.setStackInSlot(i, ItemStack.EMPTY);
            }
        }
    }

    public void consumeGrowth(ServerLevel level, BlockPos pos, int capacity) {
        consumeGrowth(level, pos, capacity, 20, Math.max(1, (capacity + 19) / 20));
    }

    public void consumeGrowth(ServerLevel level, BlockPos pos, int capacity, int width, int height) {
        if (capacity <= 0 || items.getSlots() == 0) {
            return;
        }
        width = Math.max(1, Math.min(20, width));
        height = Math.max(1, Math.min(20, height));
        List<Integer> draws = new ArrayList<>();
        for (int slot = 0; slot < Math.min(capacity, items.getSlots()); slot++) draws.add(slot);
        int attempts = Math.min(FEED_DRAW_COUNT, draws.size());
        for (int i = 0; i < attempts; i++) {
            int selected = draws.remove(level.getRandom().nextInt(draws.size()));
            ItemStack eater = items.getStackInSlot(selected);
            if (!WormStackData.isWormItem(eater)) continue;
            WormType eaterType = WormStackData.type(eater);
            int eaterGu = WormStackData.gu(eater);
            int totalGu = 0;
            List<Integer> consumed = new ArrayList<>();
            int centerX = selected % width;
            int centerZ = selected / width;
            for (int slot = 0; slot < Math.min(capacity, items.getSlots()); slot++) {
                if (slot == selected) continue;
                int x = slot % width;
                int z = slot / width;
                if (z >= height) continue;
                if (Math.abs(x - centerX) > 1 || Math.abs(z - centerZ) > 1) continue;
                ItemStack candidate = items.getStackInSlot(slot);
                if (WormStackData.isWormItem(candidate) && WormStackData.gu(candidate) <= eaterGu) {
                    consumed.add(slot);
                    totalGu += WormStackData.gu(candidate);
                }
            }
            if (totalGu <= eaterGu) continue;
            for (int slot : consumed) items.setStackInSlot(slot, ItemStack.EMPTY);
            int gain = 1 + Math.floorDiv(totalGu - eaterGu, 5);
            int newGu = Math.min(100, eaterGu + gain);
            WormType resultType = mutate(level, eaterType);
            ItemStack result = WormStackData.create(WormStackData.itemFor(resultType), resultType, newGu,
                    WormStackData.owner(eater));
            WormStackData.copyCustomData(eater, result);
            WormStackData.set(result, resultType, newGu, WormStackData.owner(eater));
            items.setStackInSlot(selected, result);
        }
    }

    private static WormType mutate(ServerLevel level, WormType current) {
        if (current == WormType.SILVERFISH && level.random.nextFloat() < 0.25F) return WormType.FIREPROOF;
        if (current == WormType.ENDERMITE && level.random.nextFloat() < 0.50F) return WormType.DETECTION;
        return current;
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
