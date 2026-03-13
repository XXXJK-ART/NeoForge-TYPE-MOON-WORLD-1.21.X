package net.xxxjk.TYPE_MOON_WORLD.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.xxxjk.TYPE_MOON_WORLD.block.entity.MuramasaBlockEntity;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MuramasaBlock extends BaseEntityBlock {
   public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
   public static final IntegerProperty ANIMATION = IntegerProperty.create("animation", 0, 1);
   public static final MapCodec<MuramasaBlock> CODEC = simpleCodec(MuramasaBlock::new);

   public MuramasaBlock(Properties properties) {
      super(properties.strength(10.0F).sound(SoundType.WOOD).noCollission());
      this.registerDefaultState((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(FACING, Direction.NORTH)).setValue(ANIMATION, 0));
   }

   @NotNull
   protected MapCodec<? extends BaseEntityBlock> codec() {
      return CODEC;
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{FACING, ANIMATION});
   }

   @Nullable
   public BlockState getStateForPlacement(@NotNull BlockPlaceContext context) {
      return (BlockState)this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
   }

   @NotNull
   public RenderShape getRenderShape(@NotNull BlockState state) {
      return RenderShape.ENTITYBLOCK_ANIMATED;
   }

   @Nullable
   public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
      return new MuramasaBlockEntity(pos, state);
   }

   @NotNull
   public InteractionResult useWithoutItem(
      @NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull BlockHitResult hit
   ) {
      if (!level.isClientSide) {
         ItemStack stackToDrop;
         if (level.random.nextFloat() < 0.1F) {
            stackToDrop = new ItemStack((ItemLike)ModItems.TSUMUKARI_MURAMASA.get());
         } else {
            stackToDrop = new ItemStack((ItemLike)ModItems.MURAMASA.get());
         }

         ItemEntity entityToSpawn = new ItemEntity(level, pos.getX(), pos.getY(), pos.getZ(), stackToDrop);
         entityToSpawn.setPickUpDelay(15);
         level.addFreshEntity(entityToSpawn);
         level.destroyBlock(pos, false);
      }

      return InteractionResult.SUCCESS;
   }
}
