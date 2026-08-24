package com.example.typemoonaddon.engravedworm;

import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class EngravedWormData {
    public static final String GU = "EngravedWormGu";
    public static final String OWNER = "EngravedWormOwner";
    public static final String OWNER_NAME = "EngravedWormOwnerName";

    private EngravedWormData() {
    }

    public static ItemStack create(Item item, int guPower, @Nullable UUID owner) {
        ItemStack stack = new ItemStack(item);
        set(stack, guPower, owner);
        return stack;
    }

    public static ItemStack create(Item item, int guPower, @Nullable UUID owner, @Nullable String ownerName) {
        ItemStack stack = new ItemStack(item);
        set(stack, guPower, owner, ownerName);
        return stack;
    }

    public static int gu(ItemStack stack) {
        return Math.max(1, tag(stack).getInt(GU));
    }

    @Nullable
    public static UUID owner(ItemStack stack) {
        CompoundTag tag = tag(stack);
        return tag.hasUUID(OWNER) ? tag.getUUID(OWNER) : null;
    }

    public static boolean isBoundTo(ItemStack stack, UUID owner) {
        return owner != null && owner.equals(owner(stack));
    }

    public static void set(ItemStack stack, int guPower, @Nullable UUID owner) {
        set(stack, guPower, owner, "");
    }

    public static void set(ItemStack stack, int guPower, @Nullable UUID owner, @Nullable String ownerName) {
        CompoundTag tag = tag(stack);
        tag.putInt(GU, Math.max(1, guPower));
        if (owner == null) {
            tag.remove(OWNER);
        } else {
            tag.putUUID(OWNER, owner);
        }
        if (ownerName == null || ownerName.isBlank()) {
            tag.remove(OWNER_NAME);
        } else {
            tag.putString(OWNER_NAME, ownerName);
        }
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.of(tag));
    }

    public static String ownerName(ItemStack stack) {
        return tag(stack).getString(OWNER_NAME);
    }

    private static CompoundTag tag(ItemStack stack) {
        return stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
    }
}
