package net.xxxjk.TYPE_MOON_WORLD.martial;

import net.minecraft.core.BlockPos;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.entity.BajiquanMasterEntity;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;

@EventBusSubscriber(modid = "typemoonworld")
public final class DojoWorldEvents {
   private static final int MAX_STRUCTURE_SPAN = 160;
   private static final int MAX_PENDING_CHECKS_PER_TICK = 8;
   private static final ResourceKey<Structure> DOJO = ResourceKey.create(Registries.STRUCTURE,
      ResourceLocation.fromNamespaceAndPath("typemoonworld", "bajiquan_dojo"));
   private static final Map<ServerLevel, Set<Long>> PENDING = new ConcurrentHashMap<>();
   private DojoWorldEvents() {}

   @SubscribeEvent
   public static void onChunkLoad(ChunkEvent.Load event) {
      if (!(event.getLevel() instanceof ServerLevel level)) return;
      if (event.getChunk().getAllStarts().isEmpty()) return;
      Structure structure = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getOrThrow(DOJO).value();
      StructureStart start = event.getChunk().getStartForStructure(structure);
      if (start == null || !start.isValid()) return;
      PENDING.computeIfAbsent(level, ignored -> ConcurrentHashMap.newKeySet()).add(start.getChunkPos().toLong());
   }

   @SubscribeEvent
   public static void onLevelTick(LevelTickEvent.Post event) {
      if (!(event.getLevel() instanceof ServerLevel level)) return;
      Set<Long> pending = PENDING.get(level);
      if (pending == null || pending.isEmpty()) return;
      int checked = 0;
      for (long packed : pending) {
         boolean complete;
         try {
            complete = tryInitialize(level, new ChunkPos(packed));
         } catch (Exception exception) {
            TYPE_MOON_WORLD.LOGGER.error("Failed to initialize Bajiquan dojo at chunk {} in {}",
               new ChunkPos(packed), level.dimension().location(), exception);
            complete = true;
         }
         if (complete) pending.remove(packed);
         if (++checked >= MAX_PENDING_CHECKS_PER_TICK) break;
      }
      if (pending.isEmpty()) PENDING.remove(level, pending);
   }

   @SubscribeEvent
   public static void onLevelUnload(LevelEvent.Unload event) {
      if (event.getLevel() instanceof ServerLevel level) PENDING.remove(level);
   }

   private static boolean tryInitialize(ServerLevel level, ChunkPos startPos) {
      LevelChunk chunk = level.getChunkSource().getChunkNow(startPos.x, startPos.z);
      if (chunk == null) return false;
      Structure structure = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getOrThrow(DOJO).value();
      StructureStart start = chunk.getStartForStructure(structure);
      if (start == null || !start.isValid()) return true;
      String dojoId = level.dimension().location() + ":" + start.getChunkPos().x + ":" + start.getChunkPos().z;
      DojoSavedData saved = level.getDataStorage().computeIfAbsent(DojoSavedData.FACTORY, "typemoonworld_bajiquan_dojos");
      if (saved.initialized.contains(dojoId)) return true;
      BoundingBox box = start.getBoundingBox();
      if (!isReasonable(box)) {
         TYPE_MOON_WORLD.LOGGER.error("Skipping Bajiquan dojo with invalid bounds {} at {} in {}", box, startPos, level.dimension().location());
         saved.markInitialized(dojoId);
         return true;
      }
      if (!areStructureChunksLoaded(level, box)) return false;
      double cx = (box.minX() + box.maxX()) * 0.5 + 0.5;
      double cz = (box.minZ() + box.maxZ()) * 0.5 + 0.5;
      if (!level.getEntitiesOfClass(BajiquanMasterEntity.class,
         new net.minecraft.world.phys.AABB(box.minX() - 8, box.minY() - 8, box.minZ() - 8, box.maxX() + 8, box.maxY() + 16, box.maxZ() + 8)).isEmpty()) {
         saved.markInitialized(dojoId);
         return true;
      }
      BlockPos home = findSafeHome(level, box, MthFloor(cx), MthFloor(cz));
      BajiquanMasterEntity master = ModEntities.BAJIQUAN_MASTER.get().create(level);
      if (master != null) {
         master.moveTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5, 0.0F, 0.0F);
         master.setDojoHome(home);
         master.ensureRandomName();
         level.addFreshEntity(master);
         saved.markInitialized(dojoId);
      }
      return master != null;
   }

   private static boolean isReasonable(BoundingBox box) {
      return box.getXSpan() > 0 && box.getYSpan() > 0 && box.getZSpan() > 0
         && box.getXSpan() <= MAX_STRUCTURE_SPAN
         && box.getYSpan() <= MAX_STRUCTURE_SPAN
         && box.getZSpan() <= MAX_STRUCTURE_SPAN;
   }

   private static boolean areStructureChunksLoaded(ServerLevel level, BoundingBox box) {
      int minChunkX = box.minX() >> 4;
      int maxChunkX = box.maxX() >> 4;
      int minChunkZ = box.minZ() >> 4;
      int maxChunkZ = box.maxZ() >> 4;
      for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
         for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
            if (level.getChunkSource().getChunkNow(chunkX, chunkZ) == null) return false;
         }
      }
      return true;
   }

   private static int MthFloor(double value) { return net.minecraft.util.Mth.floor(value); }

   private static BlockPos findSafeHome(ServerLevel level, BoundingBox box, int centerX, int centerZ) {
      int maxRadius = Math.min(10, Math.max(box.getXSpan(), box.getZSpan()) / 2);
      for (int radius = 0; radius <= maxRadius; radius++) {
         for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int z = centerZ - radius; z <= centerZ + radius; z++) {
               if (Math.max(Math.abs(x - centerX), Math.abs(z - centerZ)) != radius) continue;
               if (x < box.minX() || x > box.maxX() || z < box.minZ() || z > box.maxZ()) continue;
               for (int y = box.minY() + 1; y <= box.maxY() - 2; y++) {
                  BlockPos feet = new BlockPos(x, y, z);
                  if (!level.getBlockState(feet.below()).getCollisionShape(level, feet.below()).isEmpty()
                     && level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                     && level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty()) return feet;
               }
            }
         }
      }
      int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, centerX, centerZ);
      return new BlockPos(centerX, surface, centerZ);
   }

   private static final class DojoSavedData extends SavedData {
      private static final Factory<DojoSavedData> FACTORY = new Factory<>(DojoSavedData::new, DojoSavedData::load);
      private final Set<String> initialized = new HashSet<>();

      private static DojoSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
         DojoSavedData data = new DojoSavedData();
         ListTag list = tag.getList("Initialized", 8);
         for (int i = 0; i < list.size(); i++) data.initialized.add(list.getString(i));
         return data;
      }

      private void markInitialized(String id) {
         if (this.initialized.add(id)) this.setDirty();
      }

      @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
         ListTag list = new ListTag();
         for (String id : this.initialized) list.add(StringTag.valueOf(id));
         tag.put("Initialized", list);
         return tag;
      }
   }
}
