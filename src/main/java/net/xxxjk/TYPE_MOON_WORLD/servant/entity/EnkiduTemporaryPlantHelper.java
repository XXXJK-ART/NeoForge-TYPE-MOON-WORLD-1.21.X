package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.BlockEvent.BreakEvent;
import net.neoforged.neoforge.event.level.LevelEvent.Unload;

@EventBusSubscriber(modid = "typemoonworld")
public final class EnkiduTemporaryPlantHelper {
   private static final Map<GlobalPos, Long> TEMPORARY_PLANTS = new ConcurrentHashMap<>();

   private EnkiduTemporaryPlantHelper() {
   }

   public static void register(ServerLevel level, BlockPos pos, long expireGameTime) {
      TEMPORARY_PLANTS.put(GlobalPos.of(level.dimension(), pos.immutable()), expireGameTime);
   }

   public static boolean unregister(ServerLevel level, BlockPos pos) {
      return TEMPORARY_PLANTS.remove(GlobalPos.of(level.dimension(), pos.immutable())) != null;
   }

   public static boolean isTemporary(ServerLevel level, BlockPos pos) {
      return TEMPORARY_PLANTS.containsKey(GlobalPos.of(level.dimension(), pos.immutable()));
   }

   public static void cleanupExpired(ServerLevel level, long now) {
      TEMPORARY_PLANTS.entrySet().removeIf(entry -> {
         if (!entry.getKey().dimension().equals(level.dimension()) || entry.getValue() > now) {
            return false;
         }
         BlockPos pos = entry.getKey().pos();
         if (isTemporaryPlantState(level.getBlockState(pos))) {
            level.removeBlock(pos, false);
         }
         return true;
      });
   }

   @SubscribeEvent
   public static void onBlockBreak(BreakEvent event) {
      LevelAccessor accessor = event.getLevel();
      if (accessor.isClientSide() || !(accessor instanceof ServerLevel level)) {
         return;
      }
      if (unregister(level, event.getPos())) {
         event.setCanceled(true);
         level.setBlock(event.getPos(), Blocks.AIR.defaultBlockState(), 2);
      }
   }

   @SubscribeEvent
   public static void onBlockDrops(BlockDropsEvent event) {
      ServerLevel level = event.getLevel();
      if (isTemporary(level, event.getPos())) {
         event.getDrops().clear();
         event.setDroppedExperience(0);
         unregister(level, event.getPos());
      }
   }

   @SubscribeEvent
   public static void onLevelUnload(Unload event) {
      if (!event.getLevel().isClientSide() && event.getLevel() instanceof ServerLevel level) {
         TEMPORARY_PLANTS.keySet().removeIf(pos -> pos.dimension().equals(level.dimension()));
      }
   }

   public static boolean isTemporaryPlantState(BlockState state) {
      return state.is(Blocks.OAK_LOG)
         || state.is(Blocks.BIRCH_LOG)
         || state.is(Blocks.SPRUCE_LOG)
         || state.is(Blocks.ACACIA_LOG)
         || state.is(Blocks.MANGROVE_LOG)
         || state.is(Blocks.CHERRY_LOG)
         || state.is(Blocks.JUNGLE_LOG)
         || state.is(Blocks.OAK_LEAVES)
         || state.is(Blocks.SPRUCE_LEAVES)
         || state.is(Blocks.CHERRY_LEAVES)
         || state.is(Blocks.JUNGLE_LEAVES)
         || state.is(Blocks.MOSS_BLOCK)
         || state.is(Blocks.MOSS_CARPET)
         || state.is(Blocks.FERN)
         || state.is(Blocks.SHORT_GRASS)
         || state.is(Blocks.MANGROVE_ROOTS);
   }
}
