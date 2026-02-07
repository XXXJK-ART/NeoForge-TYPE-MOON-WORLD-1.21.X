package net.xxxjk.TYPE_MOON_WORLD.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.state.BlockState;

public class GreenTransparentBlock extends HalfTransparentBlock {
    public GreenTransparentBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!level.isClientSide) {
            // Schedule tick for 8-10 seconds (160-200 ticks)
            level.scheduleTick(pos, this, 160 + level.getRandom().nextInt(41));
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        super.tick(state, level, pos, random);
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
    }
}
