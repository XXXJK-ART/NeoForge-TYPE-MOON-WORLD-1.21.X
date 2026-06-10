package net.xxxjk.TYPE_MOON_WORLD.magic.unlimited_blade_works;

import dev.galacticraft.dynamicdimensions.api.DynamicDimensionRegistry;
import dev.galacticraft.dynamicdimensions.api.PlayerRemover;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.world.dimension.ModDimensions;

public final class UBWInstanceManager {
   private static final int DELETE_DELAY_TICKS = 2;
   private static final Map<UUID, ResourceKey<Level>> OWNER_DIMENSIONS = new ConcurrentHashMap<>();
   private static final Map<ResourceKey<Level>, UUID> DIMENSION_OWNERS = new ConcurrentHashMap<>();
   private static final Map<UUID, Integer> OWNER_GENERATIONS = new ConcurrentHashMap<>();
   private static final Map<ResourceKey<Level>, PendingDeletion> PENDING_DELETIONS = new ConcurrentHashMap<>();
   private static final Map<UUID, ChunkPos> OWNER_TICKETS = new ConcurrentHashMap<>();

   private UBWInstanceManager() {
   }

   public static ServerLevel getOrCreateFreshPlayerInstance(ServerPlayer player) {
      return getOrCreateFreshInstance(player.getServer(), player);
   }

   public static ServerLevel getOrCreateFreshInstance(MinecraftServer server, LivingEntity owner) {
      if (server == null || owner == null) {
         return null;
      }

      UUID ownerId = owner.getUUID();
      ResourceKey<Level> oldKey = OWNER_DIMENSIONS.get(ownerId);
      if (oldKey != null) {
         PENDING_DELETIONS.remove(oldKey);
         releaseInstanceTicket(server, ownerId);
         deleteDimension(server, oldKey);
         DIMENSION_OWNERS.remove(oldKey);
         OWNER_DIMENSIONS.remove(ownerId);
      }

      int generation = OWNER_GENERATIONS.merge(ownerId, 1, (oldValue, ignored) -> oldValue + 1);
      ResourceLocation id = ModDimensions.ubwInstanceId(ownerId, generation);
      ResourceKey<Level> key = ModDimensions.ubwInstanceKey(ownerId, generation);
      DynamicDimensionRegistry registry = DynamicDimensionRegistry.from(server);
      while (registry.anyDimensionExists(id)) {
         generation = OWNER_GENERATIONS.merge(ownerId, 1, (oldValue, ignored) -> oldValue + 1);
         id = ModDimensions.ubwInstanceId(ownerId, generation);
         key = ModDimensions.ubwInstanceKey(ownerId, generation);
      }

      ServerLevel level = createOrLoad(server, registry, id, true);
      if (level != null) {
         registerLevelImmediately(server, key, level);
         OWNER_DIMENSIONS.put(ownerId, key);
         DIMENSION_OWNERS.put(key, ownerId);
      }

      return level;
   }

   public static ServerLevel ensureRegisteredPlayerInstance(ServerPlayer player) {
      ServerLevel level = getRegisteredPlayerInstance(player);
      if (level != null) {
         return level;
      }

      ResourceKey<Level> key = OWNER_DIMENSIONS.get(player.getUUID());
      if (key != null) {
         DIMENSION_OWNERS.remove(key);
         OWNER_DIMENSIONS.remove(player.getUUID());
      }

      return getOrCreateFreshPlayerInstance(player);
   }

   public static ServerLevel getPlayerInstance(ServerPlayer player) {
      if (player == null || player.getServer() == null) {
         return null;
      }

      ResourceKey<Level> key = OWNER_DIMENSIONS.get(player.getUUID());
      return key == null ? null : player.getServer().getLevel(key);
   }

   public static ServerLevel getRegisteredPlayerInstance(ServerPlayer player) {
      if (player == null || player.getServer() == null) {
         return null;
      }

      ServerLevel level = getPlayerInstance(player);
      if (level != null) {
         return level;
      }

      Integer generation = OWNER_GENERATIONS.get(player.getUUID());
      if (generation == null) {
         return null;
      }

      ResourceKey<Level> key = ModDimensions.ubwInstanceKey(player.getUUID(), generation);
      level = player.getServer().getLevel(key);
      if (level != null) {
         OWNER_DIMENSIONS.put(player.getUUID(), key);
         DIMENSION_OWNERS.put(key, player.getUUID());
      }

      return level;
   }

   private static void registerLevelImmediately(MinecraftServer server, ResourceKey<Level> key, ServerLevel level) {
      if (server.getLevel(key) == null) {
         server.forgeGetWorldMap().put(key, level);
      }

      server.markWorldsDirty();
   }

   public static ResourceKey<Level> getOwnerDimension(UUID ownerId) {
      return OWNER_DIMENSIONS.get(ownerId);
   }

   public static UUID getOwnerId(ResourceKey<Level> dimension) {
      return DIMENSION_OWNERS.get(dimension);
   }

   public static boolean isUbwDimension(Level level) {
      return level != null && isUbwDimension(level.dimension().location());
   }

   public static boolean isUbwDimension(ResourceLocation location) {
      return ModDimensions.isUbwDimension(location);
   }

   public static boolean isUbwInstance(Level level) {
      return level != null && ModDimensions.isUbwInstance(level.dimension().location());
   }

   public static void keepInstanceTicking(UUID ownerId, ServerLevel level, BlockPos centerPos) {
      if (ownerId == null || level == null || centerPos == null) {
         return;
      }

      releaseInstanceTicket(level.getServer(), ownerId);
      ChunkPos chunkPos = new ChunkPos(centerPos);
      level.getChunkSource().addRegionTicket(TicketType.PLAYER, chunkPos, 3, chunkPos);
      OWNER_TICKETS.put(ownerId, chunkPos);
   }

   public static void releaseInstanceTicket(MinecraftServer server, UUID ownerId) {
      if (server == null || ownerId == null) {
         return;
      }

      ChunkPos chunkPos = OWNER_TICKETS.remove(ownerId);
      ResourceKey<Level> key = OWNER_DIMENSIONS.get(ownerId);
      if (chunkPos != null && key != null) {
         ServerLevel level = server.getLevel(key);
         if (level != null) {
            level.getChunkSource().removeRegionTicket(TicketType.PLAYER, chunkPos, 3, chunkPos);
         }
      }
   }

   public static void scheduleDeleteInstance(MinecraftServer server, UUID ownerId) {
      if (server != null && ownerId != null) {
         ResourceKey<Level> key = OWNER_DIMENSIONS.get(ownerId);
         if (key != null) {
            PENDING_DELETIONS.put(key, new PendingDeletion(ownerId, DELETE_DELAY_TICKS));
         }
      }
   }

   public static void deleteInstanceNow(MinecraftServer server, UUID ownerId) {
      if (server == null || ownerId == null) {
         return;
      }

      ResourceKey<Level> key = ModDimensions.ubwInstanceKey(ownerId);
      releaseInstanceTicket(server, ownerId);
      ResourceKey<Level> mappedKey = OWNER_DIMENSIONS.get(ownerId);
      if (mappedKey != null) {
         deleteDimension(server, mappedKey);
      } else {
         deleteDimension(server, key);
      }
      server.markWorldsDirty();

      OWNER_DIMENSIONS.remove(ownerId);
      DIMENSION_OWNERS.remove(key);
      if (mappedKey != null) {
         DIMENSION_OWNERS.remove(mappedKey);
         PENDING_DELETIONS.remove(mappedKey);
      }
      OWNER_TICKETS.remove(ownerId);
   }

   public static void processPendingDeletions(MinecraftServer server) {
      if (server == null || PENDING_DELETIONS.isEmpty()) {
         return;
      }

      Iterator<Map.Entry<ResourceKey<Level>, PendingDeletion>> it = PENDING_DELETIONS.entrySet().iterator();
      while (it.hasNext()) {
         Map.Entry<ResourceKey<Level>, PendingDeletion> entry = it.next();
         PendingDeletion pending = entry.getValue();
         int ticks = pending.ticks() - 1;
         if (ticks <= 0) {
            ResourceKey<Level> key = entry.getKey();
            UUID ownerId = pending.ownerId();
            it.remove();
            if (key.equals(OWNER_DIMENSIONS.get(ownerId))) {
               deleteInstanceNow(server, ownerId);
            } else {
               deleteDimension(server, key);
               DIMENSION_OWNERS.remove(key);
            }
         } else {
            entry.setValue(new PendingDeletion(pending.ownerId(), ticks));
         }
      }
   }

   private static ServerLevel createOrLoad(MinecraftServer server, DynamicDimensionRegistry registry, ResourceLocation id, boolean fresh) {
      LevelStem template = getTemplateStem(server);
      if (template == null) {
         TYPE_MOON_WORLD.LOGGER.error("Unable to create UBW instance '{}': missing UBW template dimension.", id);
         return null;
      }

      ChunkGenerator generator = template.generator();
      DimensionType type = copyDimensionType(template.type().value());
      ServerLevel level = fresh
         ? registry.createDynamicDimension(id, generator, type)
         : registry.loadDynamicDimension(id, generator, type);
      if (level == null && !fresh) {
         level = registry.createDynamicDimension(id, generator, type);
      } else if (level == null && fresh) {
         level = registry.loadDynamicDimension(id, generator, type);
      }

      return level;
   }

   private static void deleteDimension(MinecraftServer server, ResourceKey<Level> key) {
      DynamicDimensionRegistry registry = DynamicDimensionRegistry.from(server);
      ResourceLocation id = key.location();
      if (registry.canDeleteDimension(key)) {
         registry.deleteDynamicDimension(id, returnPlayersToOverworld());
      } else if (registry.dynamicDimensionExists(key)) {
         registry.unloadDynamicDimension(id, returnPlayersToOverworld());
      }
      server.markWorldsDirty();
   }

   private static LevelStem getTemplateStem(MinecraftServer server) {
      Registry<LevelStem> stems = server.registryAccess().registryOrThrow(Registries.LEVEL_STEM);
      ResourceKey<LevelStem> templateKey = ResourceKey.create(Registries.LEVEL_STEM, ModDimensions.UBW_KEY.location());
      return stems.get(templateKey);
   }

   private static DimensionType copyDimensionType(DimensionType source) {
      return new DimensionType(
         source.fixedTime(),
         source.hasSkyLight(),
         source.hasCeiling(),
         source.ultraWarm(),
         source.natural(),
         source.coordinateScale(),
         source.bedWorks(),
         source.respawnAnchorWorks(),
         source.minY(),
         source.height(),
         source.logicalHeight(),
         source.infiniburn(),
         source.effectsLocation(),
         source.ambientLight(),
         source.monsterSettings()
      );
   }

   private static PlayerRemover returnPlayersToOverworld() {
      return (server, player) -> {
         ServerLevel overworld = server.overworld();
         if (overworld != null && player != null) {
            Vec3 spawn = Vec3.atBottomCenterOf(overworld.getSharedSpawnPos());
            player.teleportTo(overworld, spawn.x, spawn.y, spawn.z, player.getYRot(), player.getXRot());
         }
      };
   }

   private record PendingDeletion(UUID ownerId, int ticks) {
   }
}
