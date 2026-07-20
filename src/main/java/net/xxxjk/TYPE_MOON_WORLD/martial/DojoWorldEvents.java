package net.xxxjk.TYPE_MOON_WORLD.martial;

import net.minecraft.core.BlockPos;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.xxxjk.TYPE_MOON_WORLD.entity.BajiquanMasterEntity;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;

@EventBusSubscriber(modid = "typemoonworld")
public final class DojoWorldEvents {
   private static final ResourceKey<Structure> DOJO = ResourceKey.create(Registries.STRUCTURE,
      ResourceLocation.fromNamespaceAndPath("typemoonworld", "bajiquan_dojo"));
   private DojoWorldEvents() {}

   @SubscribeEvent
   public static void onChunkLoad(ChunkEvent.Load event) {
      if (!(event.getLevel() instanceof ServerLevel level)) return;
      Structure structure = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getOrThrow(DOJO).value();
      StructureStart start = level.structureManager().startsForStructure(event.getChunk().getPos(), candidate -> candidate == structure)
         .stream().filter(StructureStart::isValid).findFirst().orElse(null);
      if (start == null) return;
      String dojoId = level.dimension().location() + ":" + start.getChunkPos().x + ":" + start.getChunkPos().z;
      DojoSavedData saved = level.getDataStorage().computeIfAbsent(DojoSavedData.FACTORY, "typemoonworld_bajiquan_dojos");
      if (saved.initialized.contains(dojoId)) return;
      BoundingBox box = start.getBoundingBox();
      double cx = (box.minX() + box.maxX()) * 0.5 + 0.5;
      double cz = (box.minZ() + box.maxZ()) * 0.5 + 0.5;
      if (!level.getEntitiesOfClass(BajiquanMasterEntity.class,
         new net.minecraft.world.phys.AABB(box.minX() - 8, box.minY() - 8, box.minZ() - 8, box.maxX() + 8, box.maxY() + 16, box.maxZ() + 8)).isEmpty()) {
         saved.markInitialized(dojoId);
         return;
      }
      BlockPos home = findSafeHome(level, box, MthFloor(cx), MthFloor(cz));
      BajiquanMasterEntity master = ModEntities.BAJIQUAN_MASTER.get().create(level);
      if (master != null) {
         master.moveTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5, 0.0F, 0.0F);
         master.setDojoHome(home);
         master.setCustomName(net.minecraft.network.chat.Component.translatable("entity.typemoonworld.bajiquan_master"));
         master.setCustomNameVisible(true);
         level.addFreshEntity(master);
         saved.markInitialized(dojoId);
      }
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
