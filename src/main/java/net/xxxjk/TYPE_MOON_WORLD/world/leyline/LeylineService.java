package net.xxxjk.TYPE_MOON_WORLD.world.leyline;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.xxxjk.TYPE_MOON_WORLD.util.ModTags;

@EventBusSubscriber(
   modid = "typemoonworld"
)
public final class LeylineService {
   public static final int MANA_PER_CONCENTRATION = 1000;
   private static final int GEM_TERRAIN_BONUS = 50;
   private static final int CACHE_SIZE = 4096;
   private static final Map<String, LeylineService.ChunkProfileCache> PROFILE_CACHE = new ConcurrentHashMap<>();

   private LeylineService() {
   }

   public static LeylineChunkProfile getProfile(ServerLevel level, BlockPos pos) {
      return getProfile(level, new ChunkPos(pos));
   }

   public static LeylineChunkProfile getProfile(ServerLevel level, int chunkX, int chunkZ) {
      return getProfile(level, new ChunkPos(chunkX, chunkZ));
   }

   public static LeylineChunkProfile getProfile(ServerLevel level, ChunkPos chunkPos) {
      String dimensionId = level.dimension().location().toString();
      LeylineService.ChunkProfileCache cache = PROFILE_CACHE.computeIfAbsent(dimensionId, ignored -> new LeylineService.ChunkProfileCache(4096));
      long key = chunkPos.toLong();
      synchronized (cache) {
         LeylineChunkProfile cached = cache.get(key);
         if (cached != null) {
            return cached;
         }
      }

      LeylineChunkProfile computed = createProfile(level, chunkPos);
      synchronized (cache) {
         cache.put(key, computed);
         return computed;
      }
   }

   public static int getConcentration(ServerLevel level, BlockPos pos) {
      return getProfile(level, pos).concentration();
   }

   public static double getRegenMultiplier(ServerLevel level, BlockPos pos) {
      return getProfile(level, pos).regenMultiplier();
   }

   public static long getManaCapacity(ServerLevel level, BlockPos pos) {
      return getProfile(level, pos).manaCapacity();
   }

   public static int computeBaseConcentration(ServerLevel level, int chunkX, int chunkZ) {
      return LeylineNoise.computeBaseConcentration(level.getSeed(), level.dimension().location().toString(), chunkX, chunkZ);
   }

   private static LeylineChunkProfile createProfile(ServerLevel level, ChunkPos chunkPos) {
      String dimensionId = level.dimension().location().toString();
      boolean bonusApplied = false;
      int concentration;
      if (LeylineNoise.isUbwDimension(dimensionId)) {
         concentration = 50;
      } else {
         concentration = LeylineNoise.computeBaseConcentration(level.getSeed(), dimensionId, chunkPos.x, chunkPos.z);
         bonusApplied = isGemTerrainChunk(level, chunkPos);
         if (bonusApplied) {
            concentration = Math.min(100, concentration + 50);
         }
      }

      double regenMultiplier = LeylineNoise.regenMultiplier(concentration);
      long manaCapacity = concentration * 1000L;
      return new LeylineChunkProfile(concentration, manaCapacity, regenMultiplier, bonusApplied);
   }

   private static boolean isGemTerrainChunk(ServerLevel level, ChunkPos chunkPos) {
      if (level.dimension() != Level.END && !LeylineNoise.isUbwDimension(level.dimension().location().toString())) {
         int centerX = (chunkPos.x << 4) + 8;
         int centerZ = (chunkPos.z << 4) + 8;
         int sampleY = Math.max(level.getMinBuildHeight(), Math.min(level.getSeaLevel(), level.getMaxBuildHeight() - 1));
         int qx = QuartPos.fromBlock(centerX);
         int qy = QuartPos.fromBlock(sampleY);
         int qz = QuartPos.fromBlock(centerZ);
         Holder<Biome> holder = level.getChunkSource()
            .getGenerator()
            .getBiomeSource()
            .getNoiseBiome(qx, qy, qz, level.getChunkSource().randomState().sampler());
         return holder.is(ModTags.Biomes.IS_GEM_TERRAIN);
      } else {
         return false;
      }
   }

   @SubscribeEvent
   public static void onServerStopping(ServerStoppingEvent event) {
      clearCaches();
   }

   public static void clearCaches() {
      PROFILE_CACHE.clear();
   }

   private static final class ChunkProfileCache extends LinkedHashMap<Long, LeylineChunkProfile> {
      private final int maxEntries;

      private ChunkProfileCache(int maxEntries) {
         super(512, 0.75F, true);
         this.maxEntries = maxEntries;
      }

      @Override
      protected boolean removeEldestEntry(Entry<Long, LeylineChunkProfile> eldest) {
         return this.size() > this.maxEntries;
      }
   }
}
