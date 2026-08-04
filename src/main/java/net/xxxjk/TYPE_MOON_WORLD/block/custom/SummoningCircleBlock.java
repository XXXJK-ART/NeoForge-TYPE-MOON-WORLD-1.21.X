package net.xxxjk.TYPE_MOON_WORLD.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class SummoningCircleBlock extends Block {
   public static final BooleanProperty LIT = BlockStateProperties.LIT;
   private static final VoxelShape SHAPE = Block.box(-16.0, 0.0, -16.0, 32.0, 1.0, 32.0);

   public SummoningCircleBlock(Properties properties) {
      super(properties);
      registerDefaultState(stateDefinition.any().setValue(LIT, false));
   }

   public static int activeLight(BlockState state) {
      return state.getValue(LIT) ? 13 : 0;
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
      builder.add(LIT);
   }

   @Override
   protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return SHAPE;
   }

   @Override
   protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return Shapes.empty();
   }

   @Override
   public boolean isPathfindable(BlockState state, net.minecraft.world.level.pathfinder.PathComputationType type) {
      return true;
   }
}
