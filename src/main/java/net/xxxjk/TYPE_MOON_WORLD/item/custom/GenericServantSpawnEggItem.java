package net.xxxjk.TYPE_MOON_WORLD.item.custom;

import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
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
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.common.NeoForge;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.BuiltinServantEntityFactory;
import net.xxxjk.typemoonworld.api.event.ServantSummonEvent;

public class GenericServantSpawnEggItem extends DeferredSpawnEggItem {
   private final ResourceLocation servantId;

   public GenericServantSpawnEggItem(
      Supplier<? extends EntityType<? extends Mob>> type,
      ResourceLocation servantId,
      int primary,
      int secondary
   ) {
      super(type, primary, secondary, new Item.Properties());
      this.servantId = servantId;
   }

   @Override
   public InteractionResult useOn(UseOnContext context) {
      Level level = context.getLevel();
      if (!(level instanceof ServerLevel serverLevel)) {
         return InteractionResult.SUCCESS;
      }

      ItemStack stack = context.getItemInHand();
      BlockPos clickedPos = context.getClickedPos();
      Direction direction = context.getClickedFace();
      BlockState clickedState = level.getBlockState(clickedPos);
      BlockPos spawnPos = clickedState.getCollisionShape(level, clickedPos).isEmpty() ? clickedPos : clickedPos.relative(direction);
      Entity spawned = this.spawnServant(serverLevel, stack, context.getPlayer(), spawnPos, !Objects.equals(clickedPos, spawnPos) && direction == Direction.UP);
      if (spawned != null) {
         if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild) {
            stack.shrink(1);
         }
         level.gameEvent(context.getPlayer(), GameEvent.ENTITY_PLACE, clickedPos);
      }
      return InteractionResult.CONSUME;
   }

   @Override
   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      BlockHitResult hitResult = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
      if (hitResult.getType() != HitResult.Type.BLOCK) {
         return InteractionResultHolder.pass(stack);
      }
      if (!(level instanceof ServerLevel serverLevel)) {
         return InteractionResultHolder.success(stack);
      }

      BlockPos blockPos = hitResult.getBlockPos();
      if (!(level.getBlockState(blockPos).getBlock() instanceof LiquidBlock)) {
         return InteractionResultHolder.pass(stack);
      }
      if (!level.mayInteract(player, blockPos) || !player.mayUseItemAt(blockPos, hitResult.getDirection(), stack)) {
         return InteractionResultHolder.fail(stack);
      }

      Entity spawned = this.spawnServant(serverLevel, stack, player, blockPos, false);
      if (spawned == null) {
         return InteractionResultHolder.pass(stack);
      }
      if (!player.getAbilities().instabuild) {
         stack.consume(1, player);
      }
      player.awardStat(Stats.ITEM_USED.get(this));
      level.gameEvent(player, GameEvent.ENTITY_PLACE, spawned.position());
      return InteractionResultHolder.consume(stack);
   }

   private Entity spawnServant(ServerLevel level, ItemStack stack, Player player, BlockPos pos, boolean offsetY) {
      ServantSummonEvent.Pre pre = NeoForge.EVENT_BUS.post(new ServantSummonEvent.Pre(level, this.servantId, pos));
      if (pre.isCanceled()) {
         return null;
      }
      var entity = BuiltinServantEntityFactory.create(level, this.servantId);
      if (entity == null) {
         return null;
      }
      double y = offsetY ? pos.getY() + 0.05 : pos.getY();
      entity.moveTo(pos.getX() + 0.5, y, pos.getZ() + 0.5, player == null ? 0.0F : player.getYRot(), 0.0F);
      if (entity instanceof Mob mob) {
         mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.SPAWN_EGG, null);
      }
      if (!level.addFreshEntity(entity)) {
         return null;
      }
      NeoForge.EVENT_BUS.post(new ServantSummonEvent.Post(level, this.servantId, pos, entity));
      return entity;
   }
}
