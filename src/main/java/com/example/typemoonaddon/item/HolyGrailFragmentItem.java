package com.example.typemoonaddon.item;

import com.example.typemoonaddon.magic.SakuraTypeMoonIntegration;
import com.example.typemoonaddon.registry.AddonAttachments;
import com.example.typemoonaddon.registry.AddonItems;
import net.minecraft.core.particles.ParticleTypes;
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

public final class HolyGrailFragmentItem extends Item {
    public HolyGrailFragmentItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return useForRitual(level, player, hand);
    }

    public static InteractionResultHolder<ItemStack> useForRitual(Level level, Player player, InteractionHand usedHand) {
        ItemStack used = player.getItemInHand(usedHand);
        InteractionHand otherHand = usedHand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack other = player.getItemInHand(otherHand);
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.sidedSuccess(used, level.isClientSide());
        }
        if (!AwakeningCatalystItems.requireAwakened(serverPlayer)) {
            return InteractionResultHolder.fail(used);
        }
        if (!isRitualPair(used, other)) {
            serverPlayer.displayClientMessage(Component.translatable("message.typemoonworld.holy_grail.requires_pair"), true);
            return InteractionResultHolder.fail(used);
        }
        var data = serverPlayer.getData(AddonAttachments.IMAGINARY_SPACE.get());
        if (!data.learned()) {
            serverPlayer.displayClientMessage(Component.translatable("message.typemoonworld.holy_grail.requires_imaginary"), true);
            return InteractionResultHolder.fail(used);
        }
        if (!SakuraTypeMoonIntegration.performHolyGrailRitual(serverPlayer)) {
            serverPlayer.displayClientMessage(Component.translatable(
                    data.grailWormAscended()
                            ? "message.typemoonworld.holy_grail.already_used"
                            : "message.typemoonworld.holy_grail.failed"), true);
            return InteractionResultHolder.fail(used);
        }
        AddonAttachments.sync(serverPlayer, AddonAttachments.IMAGINARY_SPACE);
        if (!serverPlayer.getAbilities().instabuild) {
            used.shrink(1);
            other.shrink(1);
        }
        serverPlayer.serverLevel().sendParticles(ParticleTypes.SQUID_INK, serverPlayer.getX(), serverPlayer.getY() + 1.0D, serverPlayer.getZ(), 48, 0.6D, 1.0D, 0.6D, 0.08D);
        level.playSound(null, serverPlayer.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 0.65F);
        serverPlayer.displayClientMessage(Component.translatable("message.typemoonworld.holy_grail.awakened"), false);
        return InteractionResultHolder.success(used);
    }

    private static boolean isRitualPair(ItemStack first, ItemStack second) {
        return first.is(AddonItems.HOLY_GRAIL_FRAGMENT.get()) && second.is(AddonItems.CREST_WORM.get())
                || first.is(AddonItems.CREST_WORM.get()) && second.is(AddonItems.HOLY_GRAIL_FRAGMENT.get());
    }
}
