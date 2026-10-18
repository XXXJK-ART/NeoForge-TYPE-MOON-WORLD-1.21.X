package com.captain.servant.item;

import com.captain.servant.voice.VoiceCommunicationService;
import java.util.List;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

public final class MessageStoneItem extends Item {
    private static final String STONE_ID_KEY = "fatesupport_stone_id";
    private static final String PAIR_ID_KEY = "fatesupport_pair_id";
    private static final String PARTNER_ID_KEY = "fatesupport_partner_id";
    private static final String PARTNER_HOLDER_KEY = "fatesupport_partner_holder";

    public MessageStoneItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack used = player.getItemInHand(hand);
        if (!VoiceCommunicationService.isAvailable()) {
            if (!level.isClientSide()) {
                player.displayClientMessage(Component.translatable("message.fatesupport.voicechat_missing"), true);
            }
            return InteractionResultHolder.fail(used);
        }
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();
        Binding mainBinding = binding(mainHand);
        Binding offBinding = binding(offHand);

        if (areMutuallyBound(mainBinding, offBinding)) {
            if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
                clearBinding(mainHand);
                clearBinding(offHand);
                serverPlayer.displayClientMessage(Component.translatable("message.fatesupport.message_stone.unbound"), true);
            }
            return InteractionResultHolder.sidedSuccess(used, level.isClientSide());
        }

        if (isUnboundMessageStone(mainHand) && isUnboundMessageStone(offHand)) {
            if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
                bind(mainHand, offHand);
                serverPlayer.displayClientMessage(Component.translatable("message.fatesupport.message_stone.bound"), true);
            }
            return InteractionResultHolder.sidedSuccess(used, level.isClientSide());
        }

        if (binding(used) != null) {
            player.startUsingItem(hand);
            if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
                serverPlayer.displayClientMessage(
                        Component.translatable("message.fatesupport.message_stone.transmitting"),
                        true
                );
            }
            return InteractionResultHolder.consume(used);
        }

        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.displayClientMessage(Component.translatable("message.fatesupport.message_stone.requires_pair"), true);
        }
        return InteractionResultHolder.fail(used);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72_000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeCharged) {
        if (!level.isClientSide() && entity instanceof ServerPlayer serverPlayer && binding(stack) != null) {
            serverPlayer.displayClientMessage(
                    Component.translatable("message.fatesupport.message_stone.stopped"),
                    true
            );
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return binding(stack) != null;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        String key = binding(stack) == null
                ? "item.fatesupport.message_stone.unbound"
                : "item.fatesupport.message_stone.bound";
        tooltip.add(Component.translatable(key).withStyle(ChatFormatting.GRAY));
        String partnerHolderName = partnerHolderName(stack);
        if (partnerHolderName != null) {
            tooltip.add(Component.translatable(
                    "item.fatesupport.message_stone.partner_holder",
                    Component.literal(partnerHolderName).withStyle(ChatFormatting.AQUA)
            ).withStyle(ChatFormatting.GRAY));
        }
    }

    public static Binding binding(ItemStack stack) {
        if (stack == null || !(stack.getItem() instanceof MessageStoneItem)) {
            return null;
        }
        CompoundTag tag = ((CustomData) stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)).copyTag();
        if (!tag.hasUUID(STONE_ID_KEY) || !tag.hasUUID(PAIR_ID_KEY) || !tag.hasUUID(PARTNER_ID_KEY)) {
            return null;
        }
        return new Binding(tag.getUUID(STONE_ID_KEY), tag.getUUID(PAIR_ID_KEY), tag.getUUID(PARTNER_ID_KEY));
    }

    public static void clearBinding(ItemStack stack) {
        if (stack == null || !(stack.getItem() instanceof MessageStoneItem)) {
            return;
        }
        CompoundTag tag = ((CustomData) stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)).copyTag();
        tag.remove(STONE_ID_KEY);
        tag.remove(PAIR_ID_KEY);
        tag.remove(PARTNER_ID_KEY);
        tag.remove(PARTNER_HOLDER_KEY);
        if (tag.isEmpty()) {
            stack.remove(DataComponents.CUSTOM_DATA);
        } else {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
    }

    public static boolean setPartnerHolderName(ItemStack stack, String holderName) {
        if (binding(stack) == null) {
            return false;
        }
        CompoundTag tag = ((CustomData) stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)).copyTag();
        String currentName = tag.contains(PARTNER_HOLDER_KEY) ? tag.getString(PARTNER_HOLDER_KEY) : null;
        if (holderName == null || holderName.isBlank()) {
            if (currentName == null) {
                return false;
            }
            tag.remove(PARTNER_HOLDER_KEY);
        } else {
            if (holderName.equals(currentName)) {
                return false;
            }
            tag.putString(PARTNER_HOLDER_KEY, holderName);
        }
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return true;
    }

    private static String partnerHolderName(ItemStack stack) {
        if (binding(stack) == null) {
            return null;
        }
        CompoundTag tag = ((CustomData) stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)).copyTag();
        if (!tag.contains(PARTNER_HOLDER_KEY)) {
            return null;
        }
        String holderName = tag.getString(PARTNER_HOLDER_KEY);
        return holderName.isBlank() ? null : holderName;
    }

    private static boolean isUnboundMessageStone(ItemStack stack) {
        return stack.getItem() instanceof MessageStoneItem && binding(stack) == null;
    }

    private static boolean areMutuallyBound(Binding first, Binding second) {
        return first != null
                && second != null
                && first.pairId().equals(second.pairId())
                && first.partnerId().equals(second.stoneId())
                && second.partnerId().equals(first.stoneId());
    }

    private static void bind(ItemStack first, ItemStack second) {
        UUID firstId = UUID.randomUUID();
        UUID secondId = UUID.randomUUID();
        UUID pairId = UUID.randomUUID();
        writeBinding(first, new Binding(firstId, pairId, secondId));
        writeBinding(second, new Binding(secondId, pairId, firstId));
    }

    private static void writeBinding(ItemStack stack, Binding binding) {
        CompoundTag tag = ((CustomData) stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)).copyTag();
        tag.putUUID(STONE_ID_KEY, binding.stoneId());
        tag.putUUID(PAIR_ID_KEY, binding.pairId());
        tag.putUUID(PARTNER_ID_KEY, binding.partnerId());
        tag.remove(PARTNER_HOLDER_KEY);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public record Binding(UUID stoneId, UUID pairId, UUID partnerId) {
    }
}
