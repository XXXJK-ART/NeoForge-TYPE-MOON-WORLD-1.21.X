package net.xxxjk.TYPE_MOON_WORLD.item.custom;

import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.xxxjk.TYPE_MOON_WORLD.network.GrailOpenPayload;
import net.xxxjk.TYPE_MOON_WORLD.grail.GrailWishService;
import net.neoforged.neoforge.network.PacketDistributor;

/** A server-authoritative grail. SoulCount is deliberately kept in CUSTOM_DATA for save compatibility. */
public final class HolyGrailItem extends Item {
    public static final String SOUL_COUNT = "SoulCount";
    public static final int MAX_SOUL_COUNT = 7;

    public HolyGrailItem(Properties properties) {
        super(properties);
    }

    public static int soulCount(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!(tag.get(SOUL_COUNT) instanceof IntTag)) return 0;
        return Math.clamp(tag.getInt(SOUL_COUNT), 0, MAX_SOUL_COUNT);
    }

    /** Normalizes missing, malformed and out-of-range values and returns the clamped value. */
    public static int normalizeSoulCount(ItemStack stack) {
        int value = soulCount(stack);
        if (stack != null && !stack.isEmpty()) {
            CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            if (!(tag.get(SOUL_COUNT) instanceof IntTag) || tag.getInt(SOUL_COUNT) != value) {
                tag.putInt(SOUL_COUNT, value);
                stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            }
        }
        return value;
    }

    public static void setSoulCount(ItemStack stack, int value) {
        if (stack == null || stack.isEmpty()) return;
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putInt(SOUL_COUNT, Math.clamp(value, 0, MAX_SOUL_COUNT));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean selected) {
        if (!level.isClientSide) normalizeSoulCount(stack);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown()) return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            if (normalizeSoulCount(stack) < MAX_SOUL_COUNT) {
                player.displayClientMessage(Component.translatable("message.typemoonworld.holy_grail.not_full"), true);
            } else {
                GrailWishService.rememberTriggerHand(serverPlayer, hand);
                PacketDistributor.sendToPlayer(serverPlayer, new GrailOpenPayload(hand));
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.typemoonworld.holy_grail.soul_count", soulCount(stack)));
    }
}
