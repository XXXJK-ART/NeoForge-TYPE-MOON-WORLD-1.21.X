package net.xxxjk.TYPE_MOON_WORLD.item.custom;

import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Spawner;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;

public class ServantEntitySpawnEggItem extends DeferredSpawnEggItem {
   public ServantEntitySpawnEggItem(Supplier<? extends EntityType<? extends Mob>> type, int primary, int secondary) {
      super(type, primary, secondary, new Item.Properties());
   }

   @Override
   public InteractionResult useOn(UseOnContext context) {
      Level level = context.getLevel();
      if (!(level instanceof ServerLevel serverLevel)) {
         return InteractionResult.SUCCESS;
      }

      ItemStack itemStack = context.getItemInHand();
      BlockPos clickedPos = context.getClickedPos();
      Direction direction = context.getClickedFace();
      BlockState clickedState = level.getBlockState(clickedPos);
      if (level.getBlockEntity(clickedPos) instanceof Spawner spawner) {
         EntityType<?> type = this.getType(itemStack);
         spawner.setEntityId(type, level.getRandom());
         level.sendBlockUpdated(clickedPos, clickedState, clickedState, 3);
         level.gameEvent(context.getPlayer(), GameEvent.BLOCK_CHANGE, clickedPos);
         itemStack.shrink(1);
         return InteractionResult.CONSUME;
      }

      BlockPos spawnPos = clickedState.getCollisionShape(level, clickedPos).isEmpty() ? clickedPos : clickedPos.relative(direction);
      EntityType<?> type = this.getType(itemStack);
      Entity spawned = type.spawn(
         serverLevel,
         itemStack,
         context.getPlayer(),
         spawnPos,
         MobSpawnType.SPAWN_EGG,
         true,
         !Objects.equals(clickedPos, spawnPos) && direction == Direction.UP
      );
      if (spawned != null) {
         itemStack.shrink(1);
         level.gameEvent(context.getPlayer(), GameEvent.ENTITY_PLACE, clickedPos);
      }

      return InteractionResult.CONSUME;
   }

   @Override
   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack itemStack = player.getItemInHand(hand);
      BlockHitResult hitResult = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
      if (hitResult.getType() != HitResult.Type.BLOCK) {
         return InteractionResultHolder.pass(itemStack);
      }
      if (!(level instanceof ServerLevel serverLevel)) {
         return InteractionResultHolder.success(itemStack);
      }

      BlockPos blockPos = hitResult.getBlockPos();
      if (!(level.getBlockState(blockPos).getBlock() instanceof LiquidBlock)) {
         return InteractionResultHolder.pass(itemStack);
      }
      if (!level.mayInteract(player, blockPos) || !player.mayUseItemAt(blockPos, hitResult.getDirection(), itemStack)) {
         return InteractionResultHolder.fail(itemStack);
      }

      EntityType<?> type = this.getType(itemStack);
      Entity spawned = type.spawn(serverLevel, itemStack, player, blockPos, MobSpawnType.SPAWN_EGG, false, false);
      if (spawned == null) {
         return InteractionResultHolder.pass(itemStack);
      }

      itemStack.consume(1, player);
      player.awardStat(Stats.ITEM_USED.get(this));
      level.gameEvent(player, GameEvent.ENTITY_PLACE, spawned.position());
      return InteractionResultHolder.consume(itemStack);
   }
}
