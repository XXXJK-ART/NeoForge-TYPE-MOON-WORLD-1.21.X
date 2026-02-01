package net.xxxjk.TYPE_MOON_WORLD.world.inventory;

import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

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
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.MysticEyesItem;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;
import java.util.Map;
import java.util.HashMap;

public class MagicalattributesMenu extends AbstractContainerMenu implements Supplier<Map<Integer, Slot>> {
    public final static HashMap<String, Object> guistate = new HashMap<>();
    public final Level world;
    public final Player entity;
    public int x, y, z;
    public int pageMode;
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
        int pageModeFromBuf = 0;
        if (extraData != null) {
            pos = extraData.readBlockPos();
            this.x = pos.getX();
            this.y = pos.getY();
            this.z = pos.getZ();
            access = ContainerLevelAccess.create(world, pos);
            pageModeFromBuf = extraData.readInt();
        }
        this.pageMode = pageModeFromBuf;

        TypeMoonWorldModVariables.PlayerVariables vars = this.entity.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        
        // Mystic Eyes Slot (Index 0)
        this.addSlot(new SlotItemHandler(vars.mysticEyesInventory, 0, 200, 40) {
            @Override
            public boolean isActive() {
                return pageMode == 0;
            }

            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return stack.getItem() instanceof MysticEyesItem;
            }

            @Override
            public void onTake(Player player, ItemStack stack) {
                if (player instanceof LivingEntity livingEntity) {
                    livingEntity.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 1200, 0));
                }
                super.onTake(player, stack);
            }
        });

        // Player Inventory (Indices 1-27)
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(inv, j + i * 9 + 9, 150 + j * 18, 85 + i * 18) {
                    @Override
                    public boolean isActive() {
                        return pageMode == 0;
                    }
                });
            }
        }

        // Hotbar (Indices 28-36)
        for (int k = 0; k < 9; ++k) {
            this.addSlot(new Slot(inv, k, 150 + k * 18, 143) {
                @Override
                public boolean isActive() {
                    return pageMode == 0;
                }
            });
        }
    }

    public void setPage(int page) {
        this.pageMode = page;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return true;
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            
            // Index 0 is Mystic Eyes
            // Index 1-36 is Player Inventory
            if (index == 0) {
                // From Mystic Eyes to Player Inventory
                if (!this.moveItemStackTo(itemstack1, 1, 37, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (index > 0) {
                // From Player Inventory to Mystic Eyes
                if (!this.moveItemStackTo(itemstack1, 0, 1, false)) {
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
        }
        return itemstack;
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
