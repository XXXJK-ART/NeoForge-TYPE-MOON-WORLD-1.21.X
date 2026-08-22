package com.example.typemoonaddon.worm;

import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public final class WormStackData {
    public static final String TYPE = "WormType";
    public static final String GU = "GuPower";
    public static final String OWNER = "Owner";
    public static final String CAPTURE_ELIGIBLE = "CaptureEligible";

    private WormStackData() {
    }

    public static WormType type(ItemStack stack) {
        return WormType.byId(stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.EMPTY).copyTag().getString(TYPE));
    }

    public static int gu(ItemStack stack) {
        return Math.max(1, stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.EMPTY).copyTag().getInt(GU));
    }

    @Nullable
    public static UUID owner(ItemStack stack) {
        CompoundTag tag = tag(stack);
        return tag.hasUUID(OWNER) ? tag.getUUID(OWNER) : null;
    }

    public static boolean captureEligible(ItemStack stack) {
        return tag(stack).getBoolean(CAPTURE_ELIGIBLE);
    }

    public static ItemStack create(net.minecraft.world.item.Item item, WormType type, int gu, @Nullable UUID owner) {
        ItemStack stack = new ItemStack(item);
        set(stack, type, gu, owner);
        return stack;
    }

    public static void set(ItemStack stack, WormType type, int gu, @Nullable UUID owner) {
        CompoundTag tag = tag(stack);
        tag.putString(TYPE, type.id());
        tag.putInt(GU, Math.max(1, gu));
        tag.putBoolean(CAPTURE_ELIGIBLE, false);
        if (owner == null) {
            tag.remove(OWNER);
        } else {
            tag.putUUID(OWNER, owner);
        }
        write(stack, tag);
    }

    public static void setCaptureEligible(ItemStack stack, boolean value) {
        CompoundTag tag = tag(stack);
        tag.putBoolean(CAPTURE_ELIGIBLE, value);
        write(stack, tag);
    }

    public static boolean canMerge(ItemStack first, ItemStack second) {
        return first.getItem() == second.getItem()
                && type(first) == type(second)
                && gu(first) == gu(second)
                && java.util.Objects.equals(owner(first), owner(second));
    }

    private static CompoundTag tag(ItemStack stack) {
        return stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
    }

    private static void write(ItemStack stack, CompoundTag tag) {
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.of(tag));
    }
}
