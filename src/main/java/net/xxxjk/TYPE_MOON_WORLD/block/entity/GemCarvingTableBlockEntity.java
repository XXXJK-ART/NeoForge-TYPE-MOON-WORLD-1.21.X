package net.xxxjk.TYPE_MOON_WORLD.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.CarvedGemItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.ChiselItem;
import net.xxxjk.TYPE_MOON_WORLD.magic.jewel.GemEngravingService;
import net.xxxjk.TYPE_MOON_WORLD.world.inventory.GemCarvingTableMenu;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GemCarvingTableBlockEntity extends BlockEntity implements MenuProvider {
    private static final String KEY_ITEMS = "Items";
    public static final int SLOT_GEM = 0;
    public static final int SLOT_TOOL = 1;

    private final ItemStackHandler items = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (level != null) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return switch (slot) {
                case SLOT_GEM -> stack.getItem() instanceof CarvedGemItem
                        && GemEngravingService.getEngravedMagicId(stack) == null;
                case SLOT_TOOL -> stack.getItem() instanceof ChiselItem;
                default -> false;
            };
        }

        @Override
        public int getSlotLimit(int slot) {
            return switch (slot) {
                case SLOT_GEM -> 64;
                case SLOT_TOOL -> 1;
                default -> super.getSlotLimit(slot);
            };
        }
    };

    public GemCarvingTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GEM_CARVING_TABLE_BLOCK_ENTITY.get(), pos, state);
    }

    public ItemStackHandler getItems() {
        return items;
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put(KEY_ITEMS, items.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains(KEY_ITEMS)) {
            items.deserializeNBT(registries, tag.getCompound(KEY_ITEMS));
        }
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("block.typemoonworld.gem_carving_table");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, @NotNull Inventory playerInventory, @NotNull Player player) {
        return new GemCarvingTableMenu(containerId, playerInventory, this, worldPosition);
    }
}
