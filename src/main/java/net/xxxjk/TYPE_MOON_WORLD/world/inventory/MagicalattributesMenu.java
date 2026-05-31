package net.xxxjk.TYPE_MOON_WORLD.world.inventory;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.xxxjk.TYPE_MOON_WORLD.init.TypeMoonWorldModMenus;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.MagicCrestItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.MysticEyesItem;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import org.jetbrains.annotations.NotNull;

public class MagicalattributesMenu extends AbstractContainerMenu implements Supplier<Map<Integer, Slot>> {
   public static final HashMap<String, Object> guistate = new HashMap<>();
   public static final int SLOT_PIXEL_SIZE = 18;
   public static final int SLOT_SPACING = 18;
   public static final int SLOT_MYSTIC_EYES_X = 193;
   public static final int SLOT_MYSTIC_EYES_Y = 51;
   public static final int SLOT_MAGIC_CREST_X = 223;
   public static final int SLOT_MAGIC_CREST_Y = 51;
   public static final int PLAYER_INV_X = 150;
   public static final int PLAYER_INV_Y = 85;
   public static final int HOTBAR_X = 150;
   public static final int HOTBAR_Y = 143;
   public final Level world;
   public final Player entity;
   public int x;
   public int y;
   public int z;
   public int pageMode;
   private ContainerLevelAccess access = ContainerLevelAccess.NULL;
   private final IItemHandler internal;
   private final Map<Integer, Slot> customSlots = new HashMap<>();
   private final boolean bound = false;
   private final Supplier<Boolean> boundItemMatcher = null;
   private final Entity boundEntity = null;
   private final BlockEntity boundBlockEntity = null;
   private boolean syncingCrestSlot = false;

   public MagicalattributesMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
      super((MenuType)TypeMoonWorldModMenus.MAGICAL_ATTRIBUTES.get(), id);
      this.entity = inv.player;
      this.world = inv.player.level();
      this.internal = new ItemStackHandler(0);
      int pageModeFromBuf = 0;
      if (extraData != null) {
         BlockPos pos = extraData.readBlockPos();
         this.x = pos.getX();
         this.y = pos.getY();
         this.z = pos.getZ();
         this.access = ContainerLevelAccess.create(this.world, pos);
         pageModeFromBuf = extraData.readInt();
      }

      this.pageMode = pageModeFromBuf;
      final TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)this.entity
         .getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      vars.ensureMagicSystemInitialized();
      this.addSlot(new SlotItemHandler(vars.mysticEyesInventory, 0, 193, 51) {
         public boolean isActive() {
            return MagicalattributesMenu.this.pageMode == 0;
         }

         public boolean mayPlace(@NotNull ItemStack stack) {
            return stack.getItem() instanceof MysticEyesItem;
         }

         public void onTake(Player player, ItemStack stack) {
            if (player instanceof LivingEntity) {
               player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 1200, 0));
            }

            super.onTake(player, stack);
         }
      });
      this.addSlot(new SlotItemHandler(vars.magicCrestInventory, 0, 223, 51) {
         public boolean isActive() {
            return MagicalattributesMenu.this.pageMode == 0;
         }

         public boolean mayPlace(@NotNull ItemStack stack) {
            ItemStack implanted = vars.magicCrestInventory.getStackInSlot(0);
            return implanted.isEmpty() && stack.getItem() instanceof MagicCrestItem;
         }

         public boolean mayPickup(Player player) {
            ItemStack implanted = vars.magicCrestInventory.getStackInSlot(0);
            return implanted.isEmpty();
         }

         public void setChanged() {
            super.setChanged();
            if (!MagicalattributesMenu.this.syncingCrestSlot) {
               ItemStack implanted = vars.magicCrestInventory.getStackInSlot(0);
               if (!implanted.isEmpty() && implanted.getItem() instanceof MagicCrestItem) {
                  MagicalattributesMenu.this.syncingCrestSlot = true;

                  try {
                     vars.mergeImplantedCrestEntries(TypeMoonWorldModVariables.PlayerVariables.readCrestEntriesFromStack(implanted));
                     vars.syncSelfCrestEntriesFromKnowledge();
                     TypeMoonWorldModVariables.PlayerVariables.writeCrestEntriesToStack(implanted, vars.crest_entries);
                     vars.pruneInvalidCrestWheelReferences();
                     vars.rebuildSelectedMagicsFromActiveWheel();
                     vars.syncPlayerVariables(MagicalattributesMenu.this.entity);
                  } finally {
                     MagicalattributesMenu.this.syncingCrestSlot = false;
                  }
               }
            }
         }
      });

      for (int i = 0; i < 3; i++) {
         for (int j = 0; j < 9; j++) {
            this.addSlot(new Slot(inv, j + i * 9 + 9, 150 + j * 18, 85 + i * 18) {
               public boolean isActive() {
                  return MagicalattributesMenu.this.pageMode == 0;
               }
            });
         }
      }

      for (int k = 0; k < 9; k++) {
         this.addSlot(new Slot(inv, k, 150 + k * 18, 143) {
            public boolean isActive() {
               return MagicalattributesMenu.this.pageMode == 0;
            }
         });
      }
   }

   public void setPage(int page) {
      this.pageMode = page == 1 ? 1 : 0;
   }

   public void clicked(int slotId, int dragType, @NotNull ClickType clickType, @NotNull Player player) {
      TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      vars.ensureMagicSystemInitialized();
      if (slotId == 1) {
         ItemStack implanted = vars.magicCrestInventory.getStackInSlot(0);
         boolean slotLocked = !implanted.isEmpty();
         if (slotLocked && (clickType == ClickType.SWAP || clickType == ClickType.THROW || clickType == ClickType.QUICK_MOVE)) {
            return;
         }

         if (clickType == ClickType.PICKUP && slotLocked) {
            ItemStack carried = this.getCarried();
            if (!carried.isEmpty() && carried.getItem() instanceof MagicCrestItem && implanted.getItem() instanceof MagicCrestItem) {
               this.mergeSingleCrestIntoImplanted(player, vars, implanted, carried);
               return;
            }
         }
      }

      super.clicked(slotId, dragType, clickType, player);
   }

   private void mergeSingleCrestIntoImplanted(Player player, TypeMoonWorldModVariables.PlayerVariables vars, ItemStack implanted, ItemStack carried) {
      ItemStack single = carried.copy();
      single.setCount(1);
      vars.mergeImplantedCrestEntries(TypeMoonWorldModVariables.PlayerVariables.readCrestEntriesFromStack(single));
      vars.syncSelfCrestEntriesFromKnowledge();
      TypeMoonWorldModVariables.PlayerVariables.writeCrestEntriesToStack(implanted, vars.crest_entries);
      vars.pruneInvalidCrestWheelReferences();
      vars.rebuildSelectedMagicsFromActiveWheel();
      carried.shrink(1);
      if (carried.isEmpty()) {
         this.setCarried(ItemStack.EMPTY);
      } else {
         this.setCarried(carried);
      }

      vars.syncPlayerVariables(player);
   }

   public boolean stillValid(@NotNull Player player) {
      return true;
   }

   @NotNull
   public ItemStack quickMoveStack(@NotNull Player playerIn, int index) {
      ItemStack itemstack = ItemStack.EMPTY;
      Slot slot = (Slot)this.slots.get(index);
      TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)playerIn.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      vars.ensureMagicSystemInitialized();
      if (slot != null && slot.hasItem()) {
         ItemStack itemstack1 = slot.getItem();
         itemstack = itemstack1.copy();
         if (index == 1) {
            return ItemStack.EMPTY;
         }

         if (index == 0) {
            if (!this.moveItemStackTo(itemstack1, 2, 38, true)) {
               return ItemStack.EMPTY;
            }
         } else if (index >= 2) {
            boolean moved = false;
            if (itemstack1.getItem() instanceof MysticEyesItem && ((Slot)this.slots.get(0)).mayPlace(itemstack1)) {
               moved = this.moveItemStackTo(itemstack1, 0, 1, false);
            } else if (itemstack1.getItem() instanceof MagicCrestItem) {
               ItemStack implanted = vars.magicCrestInventory.getStackInSlot(0);
               if (implanted.isEmpty()) {
                  moved = this.moveItemStackTo(itemstack1, 1, 2, false);
                  if (moved && !vars.magicCrestInventory.getStackInSlot(0).isEmpty()) {
                     vars.mergeImplantedCrestEntries(
                        TypeMoonWorldModVariables.PlayerVariables.readCrestEntriesFromStack(vars.magicCrestInventory.getStackInSlot(0))
                     );
                     vars.syncSelfCrestEntriesFromKnowledge();
                     TypeMoonWorldModVariables.PlayerVariables.writeCrestEntriesToStack(vars.magicCrestInventory.getStackInSlot(0), vars.crest_entries);
                  }
               } else {
                  ItemStack single = itemstack1.copy();
                  single.setCount(1);
                  vars.mergeImplantedCrestEntries(TypeMoonWorldModVariables.PlayerVariables.readCrestEntriesFromStack(single));
                  vars.syncSelfCrestEntriesFromKnowledge();
                  TypeMoonWorldModVariables.PlayerVariables.writeCrestEntriesToStack(implanted, vars.crest_entries);
                  itemstack1.shrink(1);
                  moved = true;
               }
            }

            if (!moved) {
               return ItemStack.EMPTY;
            }
         }

         if (itemstack1.isEmpty()) {
            slot.set(ItemStack.EMPTY);
         } else {
            slot.setChanged();
         }

         if (itemstack1.getCount() == itemstack.getCount()) {
            return ItemStack.EMPTY;
         }

         slot.onTake(playerIn, itemstack1);
         vars.rebuildSelectedMagicsFromActiveWheel();
         vars.syncPlayerVariables(playerIn);
      }

      return itemstack;
   }

   public Map<Integer, Slot> get() {
      return this.customSlots;
   }

   public IItemHandler getInternal() {
      return this.internal;
   }

   public ContainerLevelAccess getAccess() {
      return this.access;
   }
}
