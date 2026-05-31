package net.xxxjk.TYPE_MOON_WORLD.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.xxxjk.TYPE_MOON_WORLD.block.entity.GemCarvingTableBlockEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GemCarvingTableBlock extends BaseEntityBlock {
   public static final MapCodec<GemCarvingTableBlock> CODEC = simpleCodec(GemCarvingTableBlock::new);
   public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

   public GemCarvingTableBlock(Properties properties) {
      super(properties);
      this.registerDefaultState((BlockState)((BlockState)this.stateDefinition.any()).setValue(FACING, Direction.NORTH));
   }

   @NotNull
   protected MapCodec<? extends BaseEntityBlock> codec() {
      return CODEC;
   }

   @NotNull
   public RenderShape getRenderShape(@NotNull BlockState state) {
      return RenderShape.MODEL;
   }

   @Nullable
   public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
      return new GemCarvingTableBlockEntity(pos, state);
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{FACING});
   }

   @Nullable
   public BlockState getStateForPlacement(@NotNull BlockPlaceContext context) {
      return (BlockState)this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
   }

   @NotNull
   public BlockState rotate(@NotNull BlockState state, Rotation rotation) {
      return (BlockState)state.setValue(FACING, rotation.rotate((Direction)state.getValue(FACING)));
   }

   @NotNull
   public BlockState mirror(@NotNull BlockState state, Mirror mirror) {
      return this.rotate(state, mirror.getRotation((Direction)state.getValue(FACING)));
   }

   @NotNull
   protected InteractionResult useWithoutItem(
      @NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull BlockHitResult hitResult
   ) {
      openMenu(level, pos, player);
      return InteractionResult.sidedSuccess(level.isClientSide);
   }

   @NotNull
   protected ItemInteractionResult useItemOn(
      @NotNull ItemStack stack,
      @NotNull BlockState state,
      @NotNull Level level,
      @NotNull BlockPos pos,
      @NotNull Player player,
      @NotNull InteractionHand hand,
      @NotNull BlockHitResult hitResult
   ) {
      openMenu(level, pos, player);
      return ItemInteractionResult.SUCCESS;
   }

   private static void openMenu(Level level, BlockPos pos, Player player) {
      if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
         if (level.getBlockEntity(pos) instanceof GemCarvingTableBlockEntity carvingTableBlockEntity) {
            serverPlayer.openMenu(carvingTableBlockEntity, pos);
         }
      }
   }

   protected void onRemove(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState newState, boolean isMoving) {
      if (state.is(newState.getBlock())) {
         super.onRemove(state, level, pos, newState, isMoving);
      } else {
         if (!level.isClientSide && level.getBlockEntity(pos) instanceof GemCarvingTableBlockEntity carvingTableBlockEntity) {
            ItemStackHandler handler = carvingTableBlockEntity.getItems();

            for (int slot = 0; slot < handler.getSlots(); slot++) {
               ItemStack stack = handler.getStackInSlot(slot);
               if (!stack.isEmpty()) {
                  Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack.copy());
                  handler.setStackInSlot(slot, ItemStack.EMPTY);
               }
            }
         }

         super.onRemove(state, level, pos, newState, isMoving);
      }
   }
}
