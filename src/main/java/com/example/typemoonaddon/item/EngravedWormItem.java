package com.example.typemoonaddon.item;

import com.example.typemoonaddon.engravedworm.EngravedWormData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class EngravedWormItem extends Item {
    public EngravedWormItem(Properties properties) {
        super(properties.stacksTo(16));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
        if (EngravedWormData.owner(stack) == null) {
            EngravedWormData.set(stack, EngravedWormData.gu(stack), serverPlayer.getUUID());
            serverPlayer.displayClientMessage(Component.translatable("message.typemoonworld.engraved_worm.bound"), true);
        } else if (!EngravedWormData.isBoundTo(stack, serverPlayer.getUUID())) {
            serverPlayer.displayClientMessage(Component.translatable("message.typemoonworld.engraved_worm.owner_only"), true);
            return InteractionResultHolder.fail(stack);
        }
        return InteractionResultHolder.success(stack);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.typemoonworld.engraved_worm")
                .append(" (")
                .append(Component.literal(Integer.toString(EngravedWormData.gu(stack))))
                .append(")");
    }
}
