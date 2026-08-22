package com.example.typemoonaddon.item;

import com.example.typemoonaddon.registry.AddonItems;
import com.example.typemoonaddon.engravedworm.EngravedWormService;
import com.example.typemoonaddon.worm.WormCaptureService;
import com.example.typemoonaddon.worm.WormStackData;
import com.example.typemoonaddon.worm.WormWarehouseService;
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

public final class WormItem extends Item {
    public WormItem(Properties properties) {
        super(properties.stacksTo(16));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack stack = context.getItemInHand();
        if (context.getLevel().getBlockState(context.getClickedPos()).is(net.minecraft.world.level.block.Blocks.DEEPSLATE_BRICKS)) {
            if (!context.getLevel().isClientSide()
                    && context.getPlayer() instanceof ServerPlayer serverPlayer
                    && WormWarehouseService.activateFromFrame(context.getLevel(), context.getClickedPos(), serverPlayer, stack)) {
                if (!serverPlayer.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                return InteractionResult.CONSUME;
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }
        if (player.isShiftKeyDown()) {
            ItemStack engraved = EngravedWormService.createFromWorm((ServerPlayer) player, stack);
            if (engraved.isEmpty()) {
                return InteractionResultHolder.fail(stack);
            }
            if (!player.addItem(engraved)) {
                player.drop(engraved, false);
            }
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            return InteractionResultHolder.success(stack);
        }
        if (!WormCaptureService.release(serverLevel, (ServerPlayer) player, stack,
                player.getX(), player.getEyeY() - 0.15D, player.getZ())) {
            return InteractionResultHolder.fail(stack);
        }
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResultHolder.success(stack);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.typemoonworld.worm." + WormStackData.type(stack).id())
                .append(" ")
                .append(Component.literal("(" + WormStackData.gu(stack) + ")"));
    }
}
