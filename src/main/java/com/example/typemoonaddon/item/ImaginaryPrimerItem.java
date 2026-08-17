package com.example.typemoonaddon.item;

import com.example.typemoonaddon.magic.SakuraTypeMoonIntegration;
import com.example.typemoonaddon.registry.AddonAttachments;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class ImaginaryPrimerItem extends Item {
    public ImaginaryPrimerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.sidedSuccess(held, level.isClientSide());
        }
        if (!AwakeningCatalystItems.requireAwakened(serverPlayer)) {
            return InteractionResultHolder.fail(held);
        }

        var data = serverPlayer.getData(AddonAttachments.IMAGINARY_SPACE.get());
        if (data.learned()) {
            SakuraTypeMoonIntegration.ensureImaginaryAttribute(serverPlayer);
            serverPlayer.displayClientMessage(Component.translatable("message.typemoonworld.already_learned"), true);
            return InteractionResultHolder.fail(held);
        }
        if (!SakuraTypeMoonIntegration.learnAndGrantAttribute(serverPlayer)) {
            serverPlayer.displayClientMessage(Component.translatable("message.typemoonworld.attribute_failed"), false);
            return InteractionResultHolder.fail(held);
        }
        data.unlock();
        serverPlayer.syncData(AddonAttachments.IMAGINARY_SPACE.get());
        if (!serverPlayer.getAbilities().instabuild) {
            held.shrink(1);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0F, 0.65F);
        serverPlayer.displayClientMessage(Component.translatable("message.typemoonworld.learned"), false);
        return InteractionResultHolder.success(held);
    }
}
