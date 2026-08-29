package io.github.typemoonaddon.item;

import io.github.typemoonaddon.magic.ImaginaryShadowService;
import io.github.typemoonaddon.magic.ImaginaryStorageService;
import io.github.typemoonaddon.magic.TypeMoonIntegration;
import io.github.typemoonaddon.registry.ModAttachments;
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

public final class CrestWormItem extends Item {
    public CrestWormItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        InteractionHand otherHand = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        if (player.getItemInHand(otherHand).is(io.github.typemoonaddon.registry.ModItems.HOLY_GRAIL_FRAGMENT.get())) {
            return HolyGrailFragmentItem.useForRitual(level, player, hand);
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.sidedSuccess(held, level.isClientSide());
        }
        if (!AwakeningCatalystItems.requireAwakened(serverPlayer)) {
            return InteractionResultHolder.fail(held);
        }

        var data = serverPlayer.getData(ModAttachments.IMAGINARY_SPACE.get());
        if (!data.learned()) {
            serverPlayer.displayClientMessage(Component.translatable("message.typemoonaddon.crest_worm.requires_storage"), true);
            return InteractionResultHolder.fail(held);
        }
        if (data.crestWormAssimilated()) {
            serverPlayer.displayClientMessage(Component.translatable("message.typemoonaddon.crest_worm.already_used"), true);
            return InteractionResultHolder.fail(held);
        }
        if (!TypeMoonIntegration.assimilateCrestWorm(serverPlayer)) {
            serverPlayer.displayClientMessage(Component.translatable("message.typemoonaddon.crest_worm.failed"), false);
            return InteractionResultHolder.fail(held);
        }

        ImaginaryStorageService.cancelChannelsForUpgrade(serverPlayer);
        ImaginaryShadowService.stopForUpgrade(serverPlayer);
        serverPlayer.syncData(ModAttachments.IMAGINARY_SPACE.get());
        if (!serverPlayer.getAbilities().instabuild) {
            held.shrink(1);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.PLAYERS, 0.7F, 0.65F);
        serverPlayer.displayClientMessage(Component.translatable("message.typemoonaddon.crest_worm.assimilated"), false);
        return InteractionResultHolder.success(held);
    }
}
