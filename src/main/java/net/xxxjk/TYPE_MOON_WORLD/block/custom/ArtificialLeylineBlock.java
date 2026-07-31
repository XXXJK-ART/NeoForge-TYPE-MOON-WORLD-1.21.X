package net.xxxjk.TYPE_MOON_WORLD.block.custom;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.xxxjk.TYPE_MOON_WORLD.block.entity.ArtificialLeylineBlockEntity;
import net.xxxjk.TYPE_MOON_WORLD.block.entity.ModBlockEntities;
import org.jetbrains.annotations.NotNull;

public class ArtificialLeylineBlock extends BaseEntityBlock {
   public static final MapCodec<ArtificialLeylineBlock> CODEC = simpleCodec(ArtificialLeylineBlock::new);

   public ArtificialLeylineBlock(Properties properties) {
      super(properties);
   }

   @Override
   protected MapCodec<? extends BaseEntityBlock> codec() {
      return CODEC;
   }

   @Override
   protected @NotNull InteractionResult useWithoutItem(
      @NotNull BlockState state,
      @NotNull Level level,
      @NotNull BlockPos pos,
      @NotNull Player player,
      @NotNull BlockHitResult hitResult
   ) {
      if (!(level.getBlockEntity(pos) instanceof ArtificialLeylineBlockEntity blockEntity)) {
         return InteractionResult.PASS;
      }
      if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
         blockEntity.bindTick(serverPlayer);
         if (level instanceof ServerLevel serverLevel && serverPlayer.tickCount % 10 == 0) {
            serverLevel.sendParticles(ParticleTypes.ENCHANT, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 24, 0.45, 0.6, 0.45, 0.04);
         }
      }
      return InteractionResult.sidedSuccess(level.isClientSide());
   }

   @Override
   protected void onRemove(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState newState, boolean isMoving) {
      if (!state.is(newState.getBlock())) {
         if (!level.isClientSide()) {
            if (level instanceof ServerLevel serverLevel
               && level.getBlockEntity(pos) instanceof ArtificialLeylineBlockEntity blockEntity) {
               blockEntity.clearBindingOnDestroy(serverLevel);
            }
            level.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 7.0F, Level.ExplosionInteraction.BLOCK);
         }
         super.onRemove(state, level, pos, newState, isMoving);
      } else {
         super.onRemove(state, level, pos, newState, isMoving);
      }
   }

   @Override
   public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
      return new ArtificialLeylineBlockEntity(pos, state);
   }

   @Override
   protected @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
      return RenderShape.MODEL;
   }

   @Override
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
      if (level.isClientSide()) {
         return null;
      }
      return createTickerHelper(type, ModBlockEntities.ARTIFICIAL_LEYLINE_BLOCK_ENTITY.get(), (serverLevel, pos, blockState, blockEntity) -> {
         if (serverLevel instanceof ServerLevel realServerLevel) {
            ArtificialLeylineBlockEntity.tick(realServerLevel, pos, blockState, blockEntity);
         }
      });
   }
}
