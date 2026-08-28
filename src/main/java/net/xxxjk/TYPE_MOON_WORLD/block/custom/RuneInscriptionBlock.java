package net.xxxjk.TYPE_MOON_WORLD.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.BlockGetter;
import net.xxxjk.TYPE_MOON_WORLD.block.entity.ModBlockEntities;
import net.xxxjk.TYPE_MOON_WORLD.block.entity.RuneBlockEntity;

public final class RuneInscriptionBlock extends BaseEntityBlock {
   public static final MapCodec<RuneInscriptionBlock> CODEC = simpleCodec(RuneInscriptionBlock::new);
   private static final VoxelShape SHAPE = box(0, 0, 0, 16, 1, 16);
   public RuneInscriptionBlock(Properties properties) { super(properties); }
   @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
   @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
   @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPE; }
   @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new RuneBlockEntity(pos, state); }
   @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
      return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.RUNE_BLOCK_ENTITY.get(), RuneBlockEntity::tick);
   }
}
