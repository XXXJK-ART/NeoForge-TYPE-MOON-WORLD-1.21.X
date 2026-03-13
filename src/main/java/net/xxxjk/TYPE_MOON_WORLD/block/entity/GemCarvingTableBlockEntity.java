package net.xxxjk.TYPE_MOON_WORLD.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
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
      protected void onContentsChanged(int slot) {
         GemCarvingTableBlockEntity.this.setChanged();
         if (GemCarvingTableBlockEntity.this.level != null) {
            GemCarvingTableBlockEntity.this.level
               .sendBlockUpdated(
                  GemCarvingTableBlockEntity.this.worldPosition,
                  GemCarvingTableBlockEntity.this.getBlockState(),
                  GemCarvingTableBlockEntity.this.getBlockState(),
                  3
               );
         }
      }

      public boolean isItemValid(int slot, @NotNull ItemStack stack) {
         return switch (slot) {
            case 0 -> stack.getItem() instanceof CarvedGemItem && GemEngravingService.getEngravedMagicId(stack) == null;
            case 1 -> stack.getItem() instanceof ChiselItem;
            default -> false;
         };
      }

      public int getSlotLimit(int slot) {
         return switch (slot) {
            case 0 -> 64;
            case 1 -> 1;
            default -> super.getSlotLimit(slot);
         };
      }
   };

   public GemCarvingTableBlockEntity(BlockPos pos, BlockState state) {
      super((BlockEntityType)ModBlockEntities.GEM_CARVING_TABLE_BLOCK_ENTITY.get(), pos, state);
   }

   public ItemStackHandler getItems() {
      return this.items;
   }

   protected void saveAdditional(@NotNull CompoundTag tag, @NotNull Provider registries) {
      super.saveAdditional(tag, registries);
      tag.put("Items", this.items.serializeNBT(registries));
   }

   protected void loadAdditional(@NotNull CompoundTag tag, @NotNull Provider registries) {
      super.loadAdditional(tag, registries);
      if (tag.contains("Items")) {
         this.items.deserializeNBT(registries, tag.getCompound("Items"));
      }
   }

   @NotNull
   public Component getDisplayName() {
      return Component.translatable("block.typemoonworld.gem_carving_table");
   }

   @Nullable
   public AbstractContainerMenu createMenu(int containerId, @NotNull Inventory playerInventory, @NotNull Player player) {
      return new GemCarvingTableMenu(containerId, playerInventory, this, this.worldPosition);
   }
}
