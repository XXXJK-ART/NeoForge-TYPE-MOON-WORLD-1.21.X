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
import net.xxxjk.TYPE_MOON_WORLD.entity.KendoMasterEntity;
import net.xxxjk.TYPE_MOON_WORLD.martial.KendoSchool;

/** Spawn egg that tags a shared master entity with its dojo sword school. */
public final class KendoMasterSpawnEggItem extends DeferredSpawnEggItem {
   private final KendoSchool school;

   public KendoMasterSpawnEggItem(Supplier<? extends EntityType<? extends Mob>> type, int background, int highlight,
      Item.Properties properties, KendoSchool school) {
      super(type, background, highlight, properties);
      this.school = school;
   }

   @Override public InteractionResult useOn(UseOnContext context) {
      Level level = context.getLevel();
      if (!(level instanceof ServerLevel serverLevel)) return InteractionResult.SUCCESS;
      ItemStack stack = context.getItemInHand();
      BlockPos clicked = context.getClickedPos();
      Direction direction = context.getClickedFace();
      BlockState state = level.getBlockState(clicked);
      if (level.getBlockEntity(clicked) instanceof Spawner spawner) {
         spawner.setEntityId(this.getType(stack), level.getRandom());
         level.sendBlockUpdated(clicked, state, state, 3);
         level.gameEvent(context.getPlayer(), GameEvent.BLOCK_CHANGE, clicked);
         stack.shrink(1);
         return InteractionResult.CONSUME;
      }
      BlockPos spawnPos = state.getCollisionShape(level, clicked).isEmpty() ? clicked : clicked.relative(direction);
      Entity spawned = this.getType(stack).spawn(serverLevel, stack, context.getPlayer(), spawnPos, MobSpawnType.SPAWN_EGG,
         true, !Objects.equals(clicked, spawnPos) && direction == Direction.UP);
      if (spawned != null) {
         configure(spawned);
         stack.shrink(1);
         level.gameEvent(context.getPlayer(), GameEvent.ENTITY_PLACE, clicked);
      }
      return InteractionResult.CONSUME;
   }

   @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
      if (hit.getType() != HitResult.Type.BLOCK) return InteractionResultHolder.pass(stack);
      if (!(level instanceof ServerLevel serverLevel)) return InteractionResultHolder.success(stack);
      BlockPos pos = hit.getBlockPos();
      if (!(level.getBlockState(pos).getBlock() instanceof LiquidBlock)) return InteractionResultHolder.pass(stack);
      if (!level.mayInteract(player, pos) || !player.mayUseItemAt(pos, hit.getDirection(), stack)) return InteractionResultHolder.fail(stack);
      Entity spawned = this.getType(stack).spawn(serverLevel, stack, player, pos, MobSpawnType.SPAWN_EGG, false, false);
      if (spawned == null) return InteractionResultHolder.pass(stack);
      configure(spawned);
      stack.consume(1, player);
      player.awardStat(Stats.ITEM_USED.get(this));
      level.gameEvent(player, GameEvent.ENTITY_PLACE, spawned.position());
      return InteractionResultHolder.consume(stack);
   }

   private void configure(Entity entity) {
      if (entity instanceof KendoMasterEntity master) master.setSchool(school);
   }
}
