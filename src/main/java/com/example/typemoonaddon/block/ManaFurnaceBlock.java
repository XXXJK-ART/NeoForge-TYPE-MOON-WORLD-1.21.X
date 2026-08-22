package com.example.typemoonaddon.block;

import com.example.typemoonaddon.TypeMoonAddon;
import com.example.typemoonaddon.block.entity.ManaFurnaceBlockEntity;
import com.example.typemoonaddon.block.entity.AddonBlockEntities;
import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public final class ManaFurnaceBlock extends BaseEntityBlock {
    public static final MapCodec<ManaFurnaceBlock> CODEC = simpleCodec(ManaFurnaceBlock::new);

    public ManaFurnaceBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected @NotNull InteractionResult useWithoutItem(
            @NotNull BlockState state,
            @NotNull Level level,
            @NotNull BlockPos pos,
            @NotNull Player player,
            @NotNull BlockHitResult hitResult
    ) {
        if (!(level.getBlockEntity(pos) instanceof ManaFurnaceBlockEntity furnace)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }
        if (serverPlayer.isShiftKeyDown()) {
            if (furnace.isOwner(serverPlayer)) {
                if (furnace.hasPendingAuthorization()) {
                    furnace.confirmPending(serverPlayer);
                } else {
                    furnace.returnToOwner(serverPlayer);
                }
            } else if (!furnace.isBound()) {
                furnace.beginBinding(serverPlayer);
            } else {
                furnace.requestAuthorization(serverPlayer);
            }
        } else {
            serverPlayer.displayClientMessage(Component.translatable(
                    furnace.isBound()
                            ? "message.typemoonworld.mana_furnace.bound_status"
                            : "message.typemoonworld.mana_furnace.unbound_status"), true);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new ManaFurnaceBlockEntity(pos, state);
    }

    @Override
    protected @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            @NotNull Level level,
            @NotNull BlockState state,
            @NotNull BlockEntityType<T> type
    ) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(type, AddonBlockEntities.MANA_FURNACE.get(),
                (serverLevel, pos, blockState, blockEntity) -> ManaFurnaceBlockEntity.tick((ServerLevel) serverLevel, blockEntity));
    }

    @Override
    public void onRemove(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos,
                         @NotNull BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel serverLevel
                && level.getBlockEntity(pos) instanceof ManaFurnaceBlockEntity furnace) {
            furnace.clearAuthorization(serverLevel);
        }
        super.onRemove(state, level, pos, newState, moving);
    }
}
