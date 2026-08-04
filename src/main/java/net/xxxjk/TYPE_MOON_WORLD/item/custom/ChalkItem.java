package net.xxxjk.TYPE_MOON_WORLD.item.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.xxxjk.TYPE_MOON_WORLD.block.ModBlocks;

public final class ChalkItem extends Item {
   public ChalkItem(Properties properties) {
      super(properties);
   }

   @Override
   public InteractionResult useOn(UseOnContext context) {
      if (context.getClickedFace() != net.minecraft.core.Direction.UP) return InteractionResult.PASS;
      Level level = context.getLevel();
      BlockPos pos = context.getClickedPos().above();
      BlockState state = level.getBlockState(pos);
      if (!state.canBeReplaced() || !level.getBlockState(context.getClickedPos()).isSolidRender(level, context.getClickedPos())) {
         return InteractionResult.FAIL;
      }
      if (!level.isClientSide) {
         level.setBlockAndUpdate(pos, ModBlocks.SUMMONING_CIRCLE.get().defaultBlockState());
         if (context.getPlayer() != null && !context.getPlayer().getAbilities().instabuild) {
            context.getItemInHand().shrink(1);
         }
      }
      return InteractionResult.sidedSuccess(level.isClientSide);
   }
}
