package net.xxxjk.TYPE_MOON_WORLD.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.xxxjk.TYPE_MOON_WORLD.block.entity.SwordInStoneBlockEntity;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.survival.MerlinGiftService;

/** Stone block with the supplied animated sword-in-stone model. */
public final class SwordInStoneBlock extends BaseEntityBlock {
   public static final MapCodec<SwordInStoneBlock> CODEC = simpleCodec(SwordInStoneBlock::new);

   public SwordInStoneBlock(Properties properties) {
      super(properties);
   }

   @Override
   protected MapCodec<? extends BaseEntityBlock> codec() {
      return CODEC;
   }

   @Override
   public RenderShape getRenderShape(BlockState state) {
      return RenderShape.ENTITYBLOCK_ANIMATED;
   }

   @Override
   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new SwordInStoneBlockEntity(pos, state);
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
      return extract(level, pos, player);
   }

   @Override
   protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
         InteractionHand hand, BlockHitResult hit) {
      InteractionResult result = extract(level, pos, player);
      return result == InteractionResult.CONSUME
            ? ItemInteractionResult.CONSUME
            : ItemInteractionResult.sidedSuccess(level.isClientSide);
   }

   private InteractionResult extract(Level level, BlockPos pos, Player player) {
      if (level.isClientSide) return InteractionResult.SUCCESS;
      if (!MerlinGiftService.isKing(player)) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.king.denied"), true);
      } else if (level.getBlockState(pos).is(this)) {
         level.setBlock(pos, Blocks.STONE.defaultBlockState(), 3);
         ItemStack sword = new ItemStack(ModItems.EXCALIBUR2.get());
         if (!player.addItem(sword)) player.drop(sword, false);
      }
      return InteractionResult.CONSUME;
   }
}
