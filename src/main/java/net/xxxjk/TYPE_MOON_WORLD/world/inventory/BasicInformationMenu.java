package net.xxxjk.TYPE_MOON_WORLD.world.inventory;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.xxxjk.TYPE_MOON_WORLD.init.TypeMoonWorldModMenus;
import org.jetbrains.annotations.NotNull;

public class BasicInformationMenu extends AbstractContainerMenu implements Supplier<Map<Integer, Slot>> {
   public static final HashMap<String, Object> guistate = new HashMap<>();
   public final Level world;
   public final Player entity;
   public int x;
   public int y;
   public int z;
   private ContainerLevelAccess access = ContainerLevelAccess.NULL;
   private final IItemHandler internal;
   private final Map<Integer, Slot> customSlots = new HashMap<>();
   private final boolean bound = false;
   private final Supplier<Boolean> boundItemMatcher = null;
   private final Entity boundEntity = null;
   private final BlockEntity boundBlockEntity = null;

   public BasicInformationMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
      super((MenuType)TypeMoonWorldModMenus.BASIC_INFORMATION.get(), id);
      this.entity = inv.player;
      this.world = inv.player.level();
      this.internal = new ItemStackHandler(0);
      if (extraData != null) {
         BlockPos pos = extraData.readBlockPos();
         this.x = pos.getX();
         this.y = pos.getY();
         this.z = pos.getZ();
         this.access = ContainerLevelAccess.create(this.world, pos);
      }
   }

   public boolean stillValid(@NotNull Player player) {
      return true;
   }

   @NotNull
   public ItemStack quickMoveStack(@NotNull Player playerIn, int index) {
      return ItemStack.EMPTY;
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
