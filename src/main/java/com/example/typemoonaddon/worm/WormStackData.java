package com.example.typemoonaddon.worm;

import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import com.example.typemoonaddon.item.WormItem;
import com.example.typemoonaddon.registry.AddonItems;

public final class WormStackData {
    public static final String TYPE = "WormType";
    public static final String GU = "GuPower";
    public static final String OWNER = "Owner";
    public static final String CAPTURE_ELIGIBLE = "CaptureEligible";

    private WormStackData() {
    }

    public static WormType type(ItemStack stack) {
        if (stack != null && stack.getItem() instanceof WormItem wormItem && wormItem.fixedType() != null) {
            return wormItem.fixedType();
        }
        return WormType.byId(stack == null ? null : stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.EMPTY).copyTag().getString(TYPE));
    }

    public static int gu(ItemStack stack) {
        if (stack == null) return 0;
        CompoundTag custom = stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        return WormType.clampGu(custom.contains(GU) ? custom.getInt(GU) : type(stack).defaultGu());
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
        tag.putInt(GU, WormType.clampGu(gu));
        if (owner == null) {
            tag.remove(OWNER);
        } else {
            tag.putUUID(OWNER, owner);
        }
        write(stack, tag);
    }

    /** Copies the complete custom NBT payload while keeping the destination item id. */
    public static void copyCustomData(ItemStack source, ItemStack destination) {
        if (source == null || destination == null || source.isEmpty() || destination.isEmpty()) {
            return;
        }
        CompoundTag sourceTag = source.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        if (!sourceTag.isEmpty()) {
            destination.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                    net.minecraft.world.item.component.CustomData.of(sourceTag));
        }
    }

    public static void setCaptureEligible(ItemStack stack, boolean value) {
        CompoundTag tag = tag(stack);
        tag.putBoolean(CAPTURE_ELIGIBLE, value);
        write(stack, tag);
    }

    public static boolean canMerge(ItemStack first, ItemStack second) {
        return first != null && second != null && isWormItem(first) && isWormItem(second)
                && ItemStack.isSameItemSameComponents(first, second);
    }

    public static boolean isWormItem(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getItem() instanceof WormItem;
    }

    /** Returns the species-specific item used when a captured entity becomes an item. */
    public static net.minecraft.world.item.Item itemFor(WormType type) {
        if (type == null) type = WormType.SILVERFISH;
        return switch (type) {
            case SILVERFISH -> AddonItems.SILVERFISH_WORM.get();
            case ENDERMITE -> AddonItems.ENDERMITE_WORM.get();
            case WINGED -> AddonItems.WINGED_WORM.get();
            case FIREPROOF -> AddonItems.FIREPROOF_WORM.get();
            case DETECTION -> AddonItems.DETECTION_WORM.get();
        };
    }

    private static CompoundTag tag(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return new CompoundTag();
        }
        return stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
    }

    private static void write(ItemStack stack, CompoundTag tag) {
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.of(tag));
    }
}
