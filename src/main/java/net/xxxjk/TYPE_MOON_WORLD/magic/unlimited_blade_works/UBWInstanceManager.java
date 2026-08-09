package net.xxxjk.TYPE_MOON_WORLD.magic.unlimited_blade_works;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.xxxjk.TYPE_MOON_WORLD.world.dimension.ModDimensions;

@EventBusSubscriber(
   modid = "typemoonworld"
)
public final class UBWInstanceManager {
   public static final double ENTRY_RANGE = 50000.0;
   private static final Map<UUID, ResourceKey<Level>> OWNER_DIMENSIONS = new ConcurrentHashMap<>();
   private static final Map<ResourceKey<Level>, UUID> DIMENSION_OWNERS = new ConcurrentHashMap<>();
   private static final Map<UUID, ChunkPos> OWNER_TICKETS = new ConcurrentHashMap<>();

   private UBWInstanceManager() {
   }

   public static ServerLevel getOrCreateFreshPlayerInstance(ServerPlayer player) {
      return player == null ? null : acquireStaticDimension(player.getServer(), player.getUUID(), ModDimensions.UBW_KEY);
   }

   public static ServerLevel getOrCreateFreshInstance(MinecraftServer server, LivingEntity owner) {
      if (owner == null) {
         return null;
      }

      ResourceKey<Level> key = owner instanceof ServerPlayer ? ModDimensions.UBW_KEY : ModDimensions.EMIYA_UBW_KEY;
      return acquireStaticDimension(server, owner.getUUID(), key);
   }

   public static ServerLevel ensureRegisteredPlayerInstance(ServerPlayer player) {
      if (player == null) {
         return null;
      }

      ServerLevel level = getRegisteredPlayerInstance(player);
      return level != null ? level : getOrCreateFreshPlayerInstance(player);
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

      return ModDimensions.UBW_KEY.equals(OWNER_DIMENSIONS.get(player.getUUID())) ? player.getServer().getLevel(ModDimensions.UBW_KEY) : null;
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
      return isUbwDimension(level);
   }

   public static boolean isDimensionOccupied(MinecraftServer server, ResourceKey<Level> dimension, UUID requesterId) {
      UUID ownerId = DIMENSION_OWNERS.get(dimension);
      if (ownerId == null || ownerId.equals(requesterId)) {
         return false;
      }

      return server == null || server.getLevel(dimension) != null;
   }

   public static Vec3 randomEntryPosition(RandomSource random) {
      double x = (random.nextDouble() * 2.0 - 1.0) * ENTRY_RANGE + 0.5;
      double z = (random.nextDouble() * 2.0 - 1.0) * ENTRY_RANGE + 0.5;
      return new Vec3(x, 0.0, z);
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
      releaseStaticDimension(server, ownerId);
   }

   public static void deleteInstanceNow(MinecraftServer server, UUID ownerId) {
      releaseStaticDimension(server, ownerId);
   }

   public static void processPendingDeletions(MinecraftServer server) {
   }

   @SubscribeEvent
   public static void onServerStopping(ServerStoppingEvent event) {
      clearAllInstances(event.getServer());
   }

   public static void clearAllInstances(MinecraftServer server) {
      if (server != null) {
         for (Map.Entry<UUID, ChunkPos> entry : OWNER_TICKETS.entrySet()) {
            ResourceKey<Level> key = OWNER_DIMENSIONS.get(entry.getKey());
            if (key == null) {
               continue;
            }
            ServerLevel level = server.getLevel(key);
            if (level != null) {
               ChunkPos chunkPos = entry.getValue();
               level.getChunkSource().removeRegionTicket(TicketType.PLAYER, chunkPos, 3, chunkPos);
            }
         }
      }
      OWNER_TICKETS.clear();
      OWNER_DIMENSIONS.clear();
      DIMENSION_OWNERS.clear();
   }

   private static ServerLevel acquireStaticDimension(MinecraftServer server, UUID ownerId, ResourceKey<Level> key) {
      if (server == null || ownerId == null || key == null) {
         return null;
      }

      ResourceKey<Level> oldKey = OWNER_DIMENSIONS.get(ownerId);
      if (oldKey != null && !oldKey.equals(key)) {
         releaseStaticDimension(server, ownerId);
      }

      UUID currentOwner = DIMENSION_OWNERS.get(key);
      if (currentOwner != null && !currentOwner.equals(ownerId)) {
         return null;
      }

      ServerLevel level = server.getLevel(key);
      if (level == null) {
         return null;
      }

      OWNER_DIMENSIONS.put(ownerId, key);
      DIMENSION_OWNERS.put(key, ownerId);
      return level;
   }

   private static void releaseStaticDimension(MinecraftServer server, UUID ownerId) {
      if (ownerId == null) {
         return;
      }

      releaseInstanceTicket(server, ownerId);
      ResourceKey<Level> key = OWNER_DIMENSIONS.remove(ownerId);
      if (key != null && ownerId.equals(DIMENSION_OWNERS.get(key))) {
         DIMENSION_OWNERS.remove(key);
      }
   }
}
