package com.example.typemoonaddon.block;

import com.example.typemoonaddon.block.entity.WormWarehouseBlockEntity;
import com.example.typemoonaddon.worm.WormWarehouseService;
import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.xxxjk.TYPE_MOON_WORLD.world.inventory.WormWarehouseMenu;
import org.jetbrains.annotations.NotNull;

public final class WormWarehouseBlock extends BaseEntityBlock {
    public static final MapCodec<WormWarehouseBlock> CODEC = simpleCodec(WormWarehouseBlock::new);
    public static final BooleanProperty CONTROLLER = BooleanProperty.create("controller");

    public WormWarehouseBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(CONTROLLER, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
        builder.add(CONTROLLER);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return state.getValue(CONTROLLER) ? new WormWarehouseBlockEntity(pos, state) : null;
    }

    @Override
    protected @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected @NotNull InteractionResult useWithoutItem(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull BlockHitResult hitResult) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            WormWarehouseService.controllerEntity(level, pos).ifPresent(controller -> serverPlayer.openMenu(controller, controller.getBlockPos()));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected @NotNull ItemInteractionResult useItemOn(
            @NotNull net.minecraft.world.item.ItemStack stack,
            @NotNull BlockState state,
            @NotNull Level level,
            @NotNull BlockPos pos,
            @NotNull Player player,
            @NotNull InteractionHand hand,
            @NotNull BlockHitResult hitResult
    ) {
        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }

    @Override
    protected void onRemove(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock())) {
            if (!level.isClientSide && level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                if (state.getValue(CONTROLLER)
                        && level.getBlockEntity(pos) instanceof WormWarehouseBlockEntity controller
                        && controller.getBounds() != null) {
                    WormWarehouseService.destroyWarehouse(serverLevel, controller.getBounds());
                } else {
                    WormWarehouseService.destroyWarehouse(serverLevel, pos);
                }
            }
        }
        super.onRemove(state, level, pos, newState, moving);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return createTickerHelper(type, com.example.typemoonaddon.block.entity.AddonBlockEntities.WORM_WAREHOUSE.get(), (serverLevel, pos, blockState, blockEntity) -> {
            WormWarehouseService.tick((ServerLevel) serverLevel, pos, blockEntity);
        });
    }

    @Override
    public @Nullable BlockState getStateForPlacement(@NotNull BlockPlaceContext context) {
        return defaultBlockState().setValue(CONTROLLER, false);
    }

    @Override
    public @NotNull BlockState rotate(@NotNull BlockState state, @NotNull Rotation rotation) {
        return state;
    }

    @Override
    public @NotNull BlockState mirror(@NotNull BlockState state, @NotNull Mirror mirror) {
        return state;
    }
}
