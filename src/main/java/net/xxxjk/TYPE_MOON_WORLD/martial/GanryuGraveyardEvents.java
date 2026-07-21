package net.xxxjk.TYPE_MOON_WORLD.martial;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.entity.MysteriousSwordsmanEntity;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;

@EventBusSubscriber(modid = "typemoonworld")
public final class GanryuGraveyardEvents {
   private static final int MAX_STRUCTURE_SPAN = 160;
   private static final ResourceKey<Structure> GRAVEYARD = ResourceKey.create(Registries.STRUCTURE,
      ResourceLocation.fromNamespaceAndPath("typemoonworld", "ganryu_graveyard"));
   private static final Map<ServerLevel, Set<Long>> PENDING = new ConcurrentHashMap<>();

   private GanryuGraveyardEvents() {}

   @SubscribeEvent
   public static void onChunkLoad(ChunkEvent.Load event) {
      if (!(event.getLevel() instanceof ServerLevel level) || event.getChunk().getAllStarts().isEmpty()) return;
      Structure structure = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getOrThrow(GRAVEYARD).value();
      StructureStart start = event.getChunk().getStartForStructure(structure);
      if (start != null && start.isValid()) PENDING.computeIfAbsent(level, ignored -> ConcurrentHashMap.newKeySet()).add(start.getChunkPos().toLong());
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
            TYPE_MOON_WORLD.LOGGER.error("Failed to initialize Ganryu graveyard at {} in {}", new ChunkPos(packed), level.dimension().location(), exception);
            complete = true;
         }
         if (complete) pending.remove(packed);
         if (++checked >= 8) break;
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
      Structure structure = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getOrThrow(GRAVEYARD).value();
      StructureStart start = chunk.getStartForStructure(structure);
      if (start == null || !start.isValid()) return true;
      String id = level.dimension().location() + ":" + start.getChunkPos().x + ":" + start.getChunkPos().z;
      GraveyardSavedData saved = level.getDataStorage().computeIfAbsent(GraveyardSavedData.FACTORY, "typemoonworld_ganryu_graveyards");
      if (saved.initialized.contains(id)) return true;
      BoundingBox box = start.getBoundingBox();
      if (!isReasonable(box)) {
         saved.markInitialized(id);
         return true;
      }
      if (!areChunksLoaded(level, box)) return false;
      if (!level.getEntitiesOfClass(MysteriousSwordsmanEntity.class,
         new net.minecraft.world.phys.AABB(box.minX() - 8, box.minY() - 8, box.minZ() - 8, box.maxX() + 8, box.maxY() + 16, box.maxZ() + 8),
         MysteriousSwordsmanEntity::isGraveyardBound).isEmpty()) {
         saved.markInitialized(id);
         return true;
      }
      int centerX = net.minecraft.util.Mth.floor((box.minX() + box.maxX()) * 0.5 + 0.5);
      int centerZ = net.minecraft.util.Mth.floor((box.minZ() + box.maxZ()) * 0.5 + 0.5);
      BlockPos home = findSafeHome(level, box, centerX, centerZ);
      MysteriousSwordsmanEntity swordsman = ModEntities.MYSTERIOUS_SWORDSMAN.get().create(level);
      if (swordsman == null) return false;
      swordsman.moveTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5, 0.0F, 0.0F);
      swordsman.setGraveyardHome(home);
      if (!level.addFreshEntity(swordsman)) return false;
      saved.markInitialized(id);
      return true;
   }

   private static boolean isReasonable(BoundingBox box) {
      return box.getXSpan() > 0 && box.getYSpan() > 0 && box.getZSpan() > 0
         && box.getXSpan() <= MAX_STRUCTURE_SPAN && box.getYSpan() <= MAX_STRUCTURE_SPAN && box.getZSpan() <= MAX_STRUCTURE_SPAN;
   }

   private static boolean areChunksLoaded(ServerLevel level, BoundingBox box) {
      for (int x = box.minX() >> 4; x <= box.maxX() >> 4; x++) {
         for (int z = box.minZ() >> 4; z <= box.maxZ() >> 4; z++) {
            if (level.getChunkSource().getChunkNow(x, z) == null) return false;
         }
      }
      return true;
   }

   private static BlockPos findSafeHome(ServerLevel level, BoundingBox box, int centerX, int centerZ) {
      int maxRadius = Math.min(12, Math.max(box.getXSpan(), box.getZSpan()) / 2);
      for (int radius = 0; radius <= maxRadius; radius++) {
         for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int z = centerZ - radius; z <= centerZ + radius; z++) {
               if (Math.max(Math.abs(x - centerX), Math.abs(z - centerZ)) != radius
                  || x < box.minX() || x > box.maxX() || z < box.minZ() || z > box.maxZ()) continue;
               for (int y = box.minY() + 1; y <= box.maxY() - 2; y++) {
                  BlockPos feet = new BlockPos(x, y, z);
                  if (!level.getBlockState(feet.below()).getCollisionShape(level, feet.below()).isEmpty()
                     && level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                     && level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty()) return feet;
               }
            }
         }
      }
      return new BlockPos(centerX, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, centerX, centerZ), centerZ);
   }

   private static final class GraveyardSavedData extends SavedData {
      private static final Factory<GraveyardSavedData> FACTORY = new Factory<>(GraveyardSavedData::new, GraveyardSavedData::load);
      private final Set<String> initialized = new HashSet<>();

      private static GraveyardSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
         GraveyardSavedData data = new GraveyardSavedData();
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
