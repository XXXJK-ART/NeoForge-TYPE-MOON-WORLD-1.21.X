package net.xxxjk.TYPE_MOON_WORLD.world.terrain;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

/** Remembers the original ground so repeated impacts cannot progressively dig a pit. */
@EventBusSubscriber(modid = "typemoonworld")
public final class CombatTerrainProtection {
   private static final Map<Level, Map<Long, Integer>> FLOORS = new WeakHashMap<>();
   private CombatTerrainProtection() { }
   public static boolean canRemove(Level level, BlockPos pos) {
      if (level == null || !level.hasChunkAt(pos)) return false;
      Map<Long, Integer> floors = FLOORS.computeIfAbsent(level, unused -> new HashMap<>());
      long column = BlockPos.asLong(pos.getX(), 0, pos.getZ());
      int floor = floors.computeIfAbsent(column, unused -> findFloor(level, pos));
      return pos.getY() > floor;
   }
   private static int findFloor(Level level, BlockPos pos) {
      int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ()) - 1;
      while (y > level.getMinBuildHeight()) {
         BlockPos at = new BlockPos(pos.getX(), y, pos.getZ());
         var state = level.getBlockState(at);
         if (!state.isAir() && !state.is(BlockTags.LEAVES) && !state.is(BlockTags.LOGS)
            && state.isFaceSturdy(level, at, net.minecraft.core.Direction.UP)) break;
         y--;
      }
      return y;
   }
   public static boolean removeBlock(Level level, BlockPos pos, boolean moving) {
      return canRemove(level, pos) && level.removeBlock(pos, moving);
   }
   public static boolean destroyBlock(Level level, BlockPos pos, boolean drops, net.minecraft.world.entity.Entity source) {
      return canRemove(level, pos) && level.destroyBlock(pos, drops, source);
   }
   @SubscribeEvent public static void explosion(ExplosionEvent.Detonate event) {
      var source = event.getExplosion().getDirectSourceEntity();
      if (source == null) return;
      String name = source.getClass().getSimpleName();
      // NP controllers retain their established terrain permissions.
      boolean np = name.contains("Excalibur") || name.contains("EaBeam") || name.contains("GaeBulgArmy")
         || name.contains("Stella") || name.contains("BrokenPhantasm") || name.contains("RoyalCannon")
         || source.getPersistentData().getBoolean("TypeMoonNoblePhantasmTerrain");
      if (np) return;
      var id = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(source.getType());
      if (id != null && id.getNamespace().equals("typemoonworld")) event.getAffectedBlocks().removeIf(p -> !canRemove(event.getLevel(), p));
   }
   @SubscribeEvent public static void unload(LevelEvent.Unload event) { FLOORS.remove(event.getLevel()); }
}
