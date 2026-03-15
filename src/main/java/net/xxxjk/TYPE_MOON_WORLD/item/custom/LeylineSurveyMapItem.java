package net.xxxjk.TYPE_MOON_WORLD.item.custom;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import net.xxxjk.TYPE_MOON_WORLD.network.OpenLeylineSurveyMapMessage;
import net.xxxjk.TYPE_MOON_WORLD.world.leyline.LeylineService;

public class LeylineSurveyMapItem extends Item {
   private static final String TAG_HAS_SNAPSHOT = "LeylineMapHasSnapshot";
   private static final String TAG_GRID_SIZE = "LeylineMapGridSize";
   private static final String TAG_RADIUS = "LeylineMapRadius";
   private static final String TAG_CENTER_CHUNK_X = "LeylineMapCenterChunkX";
   private static final String TAG_CENTER_CHUNK_Z = "LeylineMapCenterChunkZ";
   private static final String TAG_DIMENSION = "LeylineMapDimension";
   private static final String TAG_SNAPSHOT_DATA = "LeylineMapData";
   private static final String TAG_SNAPSHOT_TIME = "LeylineMapGameTime";
   private static final int GRID_SIZE_CHUNKS = 100;
   private static final int PRELOAD_RADIUS_CHUNKS = 5;
   private static final int PRELOAD_MAX_CHUNKS = 80;

   public LeylineSurveyMapItem(Properties properties) {
      super(properties);
   }

   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
      ItemStack stack = player.getItemInHand(usedHand);
      if (player instanceof ServerPlayer serverPlayer) {
         LeylineSurveyMapItem.ScanResult result = scanChunks(serverPlayer);
         if (result == null) {
            serverPlayer.displayClientMessage(Component.translatable("message.typemoonworld.leyline_map.no_loaded_chunks"), false);
            return InteractionResultHolder.fail(stack);
         } else {
            CompoundTag tag = ((CustomData)stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)).copyTag();
            writeSnapshot(tag, result, serverPlayer.serverLevel());
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            PacketDistributor.sendToPlayer(
               serverPlayer,
               new OpenLeylineSurveyMapMessage(
                  result.gridSize(),
                  result.centerChunkX(),
                  result.centerChunkZ(),
                  serverPlayer.serverLevel().dimension().location().toString(),
                  result.concentrations()
               ),
               new CustomPacketPayload[0]
            );
            serverPlayer.displayClientMessage(
               Component.translatable("message.typemoonworld.leyline_map.scanned_opened", result.loadedSamples()), false
            );
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
         }
      } else {
         return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
      }
   }

   public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
      super.appendHoverText(stack, context, tooltip, flag);
      tooltip.add(Component.translatable("tooltip.typemoonworld.leyline_map.usage").withStyle(ChatFormatting.GRAY));
      LeylineSurveyMapItem.Snapshot snapshot = readSnapshot(stack);
      if (snapshot != null) {
         tooltip.add(
            Component.translatable(
                  "tooltip.typemoonworld.leyline_map.snapshot",
                  snapshot.gridSize(), snapshot.gridSize(), snapshot.centerChunkX(), snapshot.centerChunkZ(), snapshot.dimensionId()
               )
               .withStyle(ChatFormatting.AQUA)
         );
      }
   }

   private static void writeSnapshot(CompoundTag tag, LeylineSurveyMapItem.ScanResult result, ServerLevel level) {
      tag.putBoolean(TAG_HAS_SNAPSHOT, true);
      tag.putInt(TAG_GRID_SIZE, result.gridSize());
      tag.remove(TAG_RADIUS);
      tag.putInt(TAG_CENTER_CHUNK_X, result.centerChunkX());
      tag.putInt(TAG_CENTER_CHUNK_Z, result.centerChunkZ());
      tag.putString(TAG_DIMENSION, level.dimension().location().toString());
      tag.putByteArray(TAG_SNAPSHOT_DATA, result.concentrations());
      tag.putLong(TAG_SNAPSHOT_TIME, level.getGameTime());
   }

   @Nullable
   public static LeylineSurveyMapItem.Snapshot readSnapshot(ItemStack stack) {
      CustomData data = (CustomData)stack.get(DataComponents.CUSTOM_DATA);
      if (data == null) {
         return null;
      } else {
         CompoundTag tag = data.copyTag();
         if (!tag.getBoolean(TAG_HAS_SNAPSHOT)) {
            return null;
         } else {
            int gridSize = tag.contains(TAG_GRID_SIZE) ? tag.getInt(TAG_GRID_SIZE) : 0;
            if (gridSize <= 0 && tag.contains(TAG_RADIUS)) {
               int legacyRadius = tag.getInt(TAG_RADIUS);
               gridSize = legacyRadius * 2 + 1;
            }

            if (gridSize <= 0) {
               return null;
            } else {
               byte[] concentrations = tag.getByteArray(TAG_SNAPSHOT_DATA);
               return concentrations.length != gridSize * gridSize
                  ? null
                  : new LeylineSurveyMapItem.Snapshot(
                     gridSize, tag.getInt(TAG_CENTER_CHUNK_X), tag.getInt(TAG_CENTER_CHUNK_Z), tag.getString(TAG_DIMENSION), concentrations
                  );
            }
         }
      }
   }

   @Nullable
   private static LeylineSurveyMapItem.ScanResult scanChunks(ServerPlayer player) {
      ServerLevel level = player.serverLevel();
      ChunkPos origin = player.chunkPosition();
      preloadNearbyChunks(level, origin);
      int size = GRID_SIZE_CHUNKS;
      int half = size / 2;
      int startChunkX = origin.x - half;
      int startChunkZ = origin.z - half;
      byte[] concentrations = new byte[size * size];
      int sampledChunks = 0;
      int sampleY = player.getBlockY();

      for (int z = 0; z < size; z++) {
         int chunkZ = startChunkZ + z;

         for (int x = 0; x < size; x++) {
            int chunkX = startChunkX + x;
            int index = z * size + x;
            BlockPos samplePos = new BlockPos((chunkX << 4) + 8, sampleY, (chunkZ << 4) + 8);
            int concentration = LeylineService.getConcentration(level, samplePos);
            concentrations[index] = (byte)concentration;
            sampledChunks++;
         }
      }

      return sampledChunks <= 0 ? null : new LeylineSurveyMapItem.ScanResult(size, origin.x, origin.z, concentrations, sampledChunks);
   }

   private static void preloadNearbyChunks(ServerLevel level, ChunkPos origin) {
      int preloaded = 0;

      for (int radius = 0; radius <= PRELOAD_RADIUS_CHUNKS && preloaded < PRELOAD_MAX_CHUNKS; radius++) {
         for (int dz = -radius; dz <= radius && preloaded < PRELOAD_MAX_CHUNKS; dz++) {
            for (int dx = -radius; dx <= radius && preloaded < PRELOAD_MAX_CHUNKS; dx++) {
               if (Math.max(Math.abs(dx), Math.abs(dz)) == radius) {
                  int chunkX = origin.x + dx;
                  int chunkZ = origin.z + dz;
                  if (level.getChunkSource().getChunkNow(chunkX, chunkZ) == null) {
                     level.getChunk(chunkX, chunkZ);
                     preloaded++;
                  }
               }
            }
         }
      }
   }

   private record ScanResult(int gridSize, int centerChunkX, int centerChunkZ, byte[] concentrations, int loadedSamples) {
   }

   public record Snapshot(int gridSize, int centerChunkX, int centerChunkZ, String dimensionId, byte[] concentrations) {
   }
}
