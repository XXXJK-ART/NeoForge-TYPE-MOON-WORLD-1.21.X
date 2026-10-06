package com.example.typemoonaddon.item;

import com.example.typemoonaddon.registry.AddonItems;
import com.example.typemoonaddon.worm.WormCaptureService;
import com.example.typemoonaddon.worm.WormStackData;
import com.example.typemoonaddon.worm.WormWarehouseService;
import com.example.typemoonaddon.magic.WormMagicIntegration;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import com.example.typemoonaddon.worm.WormType;

public final class WormItem extends Item {
    private final WormType fixedType;

    public WormItem(Properties properties) {
        this(properties, null);
    }

    public WormItem(Properties properties, WormType fixedType) {
        super(properties.stacksTo(16));
        this.fixedType = fixedType;
    }

    public WormType fixedType() {
        return fixedType;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack stack = context.getItemInHand();
        if (context.getLevel().getBlockState(context.getClickedPos()).is(net.minecraft.world.level.block.Blocks.DEEPSLATE_BRICKS)) {
            if (!context.getLevel().isClientSide()
                    && context.getPlayer() instanceof ServerPlayer serverPlayer
                    && WormWarehouseService.activateFromFrame(context.getLevel(), context.getClickedPos(), serverPlayer, stack)) {
                return InteractionResult.CONSUME;
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer serverPlayer) || !(level instanceof ServerLevel serverLevel)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
        if (!WormMagicIntegration.isKnown(serverPlayer, WormMagicIntegration.WORM_MAGIC)) {
            serverPlayer.displayClientMessage(Component.translatable("message.typemoonworld.worm.requires_magic"), true);
            return InteractionResultHolder.fail(stack);
        }
        try {
            if (!WormCaptureService.release(serverLevel, serverPlayer, stack,
                    player.getX(), player.getEyeY() - 0.15D, player.getZ())) {
                return InteractionResultHolder.fail(stack);
            }
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            return InteractionResultHolder.success(stack);
        } catch (RuntimeException exception) {
            com.example.typemoonaddon.TypeMoonAddon.LOGGER.error("Worm item activation failed", exception);
            return InteractionResultHolder.fail(stack);
        }
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.typemoonworld.worm." + WormStackData.type(stack).id())
                .append(" ")
                .append(Component.literal("(" + WormStackData.gu(stack) + ")"));
    }
}
