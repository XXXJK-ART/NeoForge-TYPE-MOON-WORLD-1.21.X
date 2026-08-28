package io.github.typemoonaddon.item;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.xxxjk.typemoonworld.api.TypeMoonWorldApi;

/** Dedicated card-face item that delegates activation to the public master-card API. */
public final class MasterProfileCardItem extends Item {
    private final ResourceLocation profileId;

    public MasterProfileCardItem(Properties properties, ResourceLocation profileId) {
        super(properties);
        this.profileId = profileId;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }
        if (!(player instanceof ServerPlayer)) {
            return InteractionResultHolder.fail(stack);
        }

        ItemStack delegate = TypeMoonWorldApi.addon(profileId.getNamespace()).masters().createCard(profileId);
        if (delegate.isEmpty()) {
            return InteractionResultHolder.fail(stack);
        }
        player.setItemInHand(hand, delegate);
        return delegate.use(level, player, hand);
    }
}
