package net.xxxjk.TYPE_MOON_WORLD.world.inventory;

import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.IItemHandler;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.Entity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.BlockPos;

import net.xxxjk.TYPE_MOON_WORLD.init.TypeMoonWorldModMenus;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;
import java.util.Map;
import java.util.HashMap;

public class MagicalattributesMenu extends AbstractContainerMenu implements Supplier<Map<Integer, Slot>> {
    public final static HashMap<String, Object> guistate = new HashMap<>();
    public final Level world;
    public final Player entity;
    public int x, y, z;
    private ContainerLevelAccess access = ContainerLevelAccess.NULL;
    private final IItemHandler internal;
    private final Map<Integer, Slot> customSlots = new HashMap<>();
    private final boolean bound = false;
    private final Supplier<Boolean> boundItemMatcher = null;
    private final Entity boundEntity = null;
    private final BlockEntity boundBlockEntity = null;

    public MagicalattributesMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
        super(TypeMoonWorldModMenus.MAGICAL_ATTRIBUTES.get(), id);
        this.entity = inv.player;
        this.world = inv.player.level();
        this.internal = new ItemStackHandler(0);
        BlockPos pos;
        if (extraData != null) {
            pos = extraData.readBlockPos();
            this.x = pos.getX();
            this.y = pos.getY();
            this.z = pos.getZ();
            access = ContainerLevelAccess.create(world, pos);
        }
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return true;
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player playerIn, int index) {
        return ItemStack.EMPTY;
    }

    public Map<Integer, Slot> get() {
        return customSlots;
    }

    public IItemHandler getInternal() {
        return internal;
    }

    public ContainerLevelAccess getAccess() {
        return access;
    }
}