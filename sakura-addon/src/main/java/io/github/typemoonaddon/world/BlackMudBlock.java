package io.github.typemoonaddon.world;

import io.github.typemoonaddon.magic.BlackMudService;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class BlackMudBlock extends LiquidBlock {
    private static final VoxelShape WALKABLE_SURFACE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 14.0D, 16.0D);

    public BlackMudBlock(FlowingFluid fluid, BlockBehaviour.Properties properties) {
        super(fluid, properties);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (entity instanceof LivingEntity living) {
            BlackMudService.touch(living, pos);
        }
    }

    @Override
    protected VoxelShape getCollisionShape(
        BlockState state,
        BlockGetter level,
        BlockPos pos,
        CollisionContext context
    ) {
        return context.isAbove(WALKABLE_SURFACE, pos, true)
            && context.canStandOnFluid(level.getFluidState(pos.above()), state.getFluidState())
            ? WALKABLE_SURFACE
            : Shapes.empty();
    }
}
