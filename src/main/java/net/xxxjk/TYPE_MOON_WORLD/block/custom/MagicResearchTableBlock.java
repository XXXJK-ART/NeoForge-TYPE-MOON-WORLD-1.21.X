package net.xxxjk.TYPE_MOON_WORLD.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.xxxjk.TYPE_MOON_WORLD.block.entity.MagicResearchTableBlockEntity;
import org.jetbrains.annotations.NotNull;

public class MagicResearchTableBlock extends BaseEntityBlock {
   public static final MapCodec<MagicResearchTableBlock> CODEC = simpleCodec(MagicResearchTableBlock::new);
   public MagicResearchTableBlock(Properties p) { super(p); }
   @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
   @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
   @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new MagicResearchTableBlockEntity(pos, state); }
   @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return level.isClientSide?null:(l,p,s,be)->{if(be instanceof MagicResearchTableBlockEntity research)MagicResearchTableBlockEntity.tick(l,p,s,research);};}
   @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
      if (!level.isClientSide && player instanceof ServerPlayer sp && level.getBlockEntity(pos) instanceof MagicResearchTableBlockEntity be) sp.openMenu(be, pos);
      return InteractionResult.sidedSuccess(level.isClientSide);
   }
   @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
      if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof MagicResearchTableBlockEntity be) for (int i=0;i<be.getItems().getSlots();i++) { var s=be.getItems().getStackInSlot(i); if (!s.isEmpty()) Containers.dropItemStack(level,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,s); }
      super.onRemove(state, level, pos, newState, moving);
   }
}
