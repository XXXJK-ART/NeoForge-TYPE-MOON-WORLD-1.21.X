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
import net.minecraft.world.entity.MobSpawnType;
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
import net.xxxjk.TYPE_MOON_WORLD.entity.KendoApprenticeEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.KendoMasterEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.ShinsengumiEntity;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;

@EventBusSubscriber(modid = "typemoonworld")
public final class KendoDojoWorldEvents {
   private static final ResourceKey<Structure> HOKUSHIN = key("hokushin_dojo");
   private static final ResourceKey<Structure> TENNEN = key("tennen_dojo");
   private static final Map<ServerLevel, Set<Long>> PENDING = new ConcurrentHashMap<>();
   private KendoDojoWorldEvents() {}
   private static ResourceKey<Structure> key(String id) { return ResourceKey.create(Registries.STRUCTURE, ResourceLocation.fromNamespaceAndPath("typemoonworld", id)); }

   @SubscribeEvent public static void onChunkLoad(ChunkEvent.Load event) {
      if (!(event.getLevel() instanceof ServerLevel level) || event.getChunk().getAllStarts().isEmpty()) return;
      PENDING.computeIfAbsent(level, ignored -> ConcurrentHashMap.newKeySet()).add(event.getChunk().getPos().toLong());
   }
   @SubscribeEvent public static void onLevelTick(LevelTickEvent.Post event) {
      if (!(event.getLevel() instanceof ServerLevel level)) return;
      Set<Long> pending = PENDING.get(level); if (pending == null) return;
      int checked = 0;
      for (long packed : pending.toArray(Long[]::new)) {
         boolean done = initialize(level, new ChunkPos(packed), HOKUSHIN) & initialize(level, new ChunkPos(packed), TENNEN);
         if (done) pending.remove(packed); if (++checked >= 8) break;
      }
      if (pending.isEmpty()) PENDING.remove(level);
   }
   @SubscribeEvent public static void onUnload(LevelEvent.Unload event) { if (event.getLevel() instanceof ServerLevel level) PENDING.remove(level); }

   private static boolean initialize(ServerLevel level, ChunkPos pos, ResourceKey<Structure> key) {
      Structure structure = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getOrThrow(key).value();
      LevelChunk chunk = level.getChunkSource().getChunkNow(pos.x, pos.z); if (chunk == null) return false;
      StructureStart start = chunk.getStartForStructure(structure); if (start == null || !start.isValid()) return true;
      BoundingBox box = start.getBoundingBox(); if (box.getXSpan() > 160 || box.getZSpan() > 160) return true;
      String id = key.location() + ":" + level.dimension().location() + ":" + pos.x + ":" + pos.z;
      KendoSavedData saved = level.getDataStorage().computeIfAbsent(KendoSavedData.FACTORY, "typemoonworld_kendo_dojos");
      if (!saved.initialized.add(id)) return true;
      KendoSchool school = key == TENNEN ? KendoSchool.TENNEN : KendoSchool.HOKUSHIN;
      BlockPos center = new BlockPos((box.minX() + box.maxX()) / 2, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (box.minX() + box.maxX()) / 2, (box.minZ() + box.maxZ()) / 2), (box.minZ() + box.maxZ()) / 2);
      KendoMasterEntity master = ModEntities.KENDO_MASTER.get().create(level);
      if (master != null) { master.setSchool(school); master.setDojoHome(center); master.moveTo(center.getX() + .5, center.getY(), center.getZ() + .5, 0, 0); level.addFreshEntity(master); }
      int count = 1 + level.random.nextInt(4);
      for (int i = 0; i < count; i++) {
         KendoApprenticeEntity apprentice = ModEntities.KENDO_APPRENTICE.get().create(level);
         if (apprentice == null) continue;
         apprentice.setFemale(level.random.nextBoolean()); apprentice.setProficiency(20 + level.random.nextInt(61));
         BlockPos spawn = new BlockPos(MthClamp(center.getX() + level.random.nextInt(9) - 4, box.minX(), box.maxX()), center.getY(), MthClamp(center.getZ() + level.random.nextInt(9) - 4, box.minZ(), box.maxZ()));
         apprentice.moveTo(spawn.getX() + .5, spawn.getY(), spawn.getZ() + .5, level.random.nextFloat() * 360, 0); apprentice.finalizeSpawn(level, level.getCurrentDifficultyAt(spawn), MobSpawnType.STRUCTURE, null); apprentice.setSchool(school); apprentice.setProficiency(20 + level.random.nextInt(61)); level.addFreshEntity(apprentice);
      }
      if (school == KendoSchool.TENNEN) {
         ShinsengumiEntity shinsengumi = ModEntities.SHINSENGUMI.get().create(level);
         if (shinsengumi != null) { shinsengumi.setProficiency(70 + level.random.nextInt(31)); shinsengumi.moveTo(center.getX() + 1.5, center.getY(), center.getZ() + .5, 0, 0); level.addFreshEntity(shinsengumi); }
      }
      saved.setDirty(); return true;
   }
   private static int MthClamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }
   private static final class KendoSavedData extends SavedData {
      private static final Factory<KendoSavedData> FACTORY = new Factory<>(KendoSavedData::new, KendoSavedData::load);
      private final Set<String> initialized = new HashSet<>();
      private static KendoSavedData load(CompoundTag tag, HolderLookup.Provider provider) { KendoSavedData data = new KendoSavedData(); ListTag list = tag.getList("Initialized", 8); for (int i = 0; i < list.size(); i++) data.initialized.add(list.getString(i)); return data; }
      @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) { ListTag list = new ListTag(); initialized.forEach(value -> list.add(StringTag.valueOf(value))); tag.put("Initialized", list); return tag; }
   }
}
