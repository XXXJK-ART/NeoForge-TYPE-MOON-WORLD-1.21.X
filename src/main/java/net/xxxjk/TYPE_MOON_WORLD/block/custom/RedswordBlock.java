package net.xxxjk.TYPE_MOON_WORLD.block.custom;

import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.xxxjk.TYPE_MOON_WORLD.block.entity.RedswordBlockEntity;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.NotNull;
import com.mojang.serialization.MapCodec;

public class RedswordBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final IntegerProperty ANIMATION = IntegerProperty.create("animation", 0, 1);
    public static final MapCodec<RedswordBlock> CODEC = simpleCodec(RedswordBlock::new);

    public RedswordBlock(Properties properties) {
        super(properties.strength(10f).sound(SoundType.WOOD).noCollission());
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(ANIMATION, 0));
    }
    
    @Override
    @NotNull
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ANIMATION);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(@NotNull BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    @NotNull
    public RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new RedswordBlockEntity(pos, state);
    }

    @Override
    @NotNull
    public InteractionResult useWithoutItem(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull BlockHitResult hit) {
        if (!level.isClientSide) {
             ItemStack stackToDrop;
             if (level.random.nextFloat() < 0.1f) {
                 stackToDrop = new ItemStack(ModItems.TSUMUKARI_MURAMASA.get());
             } else {
                 stackToDrop = new ItemStack(ModItems.REDSWORD.get());
             }
             ItemEntity entityToSpawn = new ItemEntity(level, pos.getX(), pos.getY(), pos.getZ(), stackToDrop);
             entityToSpawn.setPickUpDelay(15);
             level.addFreshEntity(entityToSpawn);
             level.destroyBlock(pos, false);
         }
        return InteractionResult.SUCCESS;
    }
}
