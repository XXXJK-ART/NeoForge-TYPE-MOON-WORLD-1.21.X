package net.xxxjk.TYPE_MOON_WORLD.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.xxxjk.TYPE_MOON_WORLD.world.inventory.MagicCopyingTableMenu;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MagicCopyingTableBlockEntity extends BlockEntity implements MenuProvider {
   private final ItemStackHandler items = new ItemStackHandler(2) { @Override protected void onContentsChanged(int slot) { setChanged(); } };
   public MagicCopyingTableBlockEntity(BlockPos pos, BlockState state) { super((BlockEntityType)ModBlockEntities.MAGIC_COPYING_TABLE.get(), pos, state); }
   public ItemStackHandler getItems() { return items; }
   @Override protected void saveAdditional(@NotNull CompoundTag tag, @NotNull Provider provider) { super.saveAdditional(tag, provider); tag.put("Items", items.serializeNBT(provider)); }
   @Override protected void loadAdditional(@NotNull CompoundTag tag, @NotNull Provider provider) { super.loadAdditional(tag, provider); if (tag.contains("Items")) items.deserializeNBT(provider, tag.getCompound("Items")); }
   @Override public Component getDisplayName() { return Component.translatable("block.typemoonworld.magic_copying_table"); }
   @Nullable @Override public AbstractContainerMenu createMenu(int id, @NotNull Inventory inv, @NotNull Player player) { return new MagicCopyingTableMenu(id, inv, this, worldPosition); }
}
