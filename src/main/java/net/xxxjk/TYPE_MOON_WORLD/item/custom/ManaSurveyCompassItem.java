package net.xxxjk.TYPE_MOON_WORLD.item.custom;

import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.xxxjk.TYPE_MOON_WORLD.utils.ManaHelper;
import net.xxxjk.TYPE_MOON_WORLD.world.leyline.LeylineService;

public class ManaSurveyCompassItem extends Item {
   private static final String TAG_HAS_TARGET = "ManaSurveyHasTarget";
   private static final String TAG_TARGET_X = "ManaSurveyTargetX";
   private static final String TAG_TARGET_Y = "ManaSurveyTargetY";
   private static final String TAG_TARGET_Z = "ManaSurveyTargetZ";
   private static final String TAG_TARGET_DIM = "ManaSurveyTargetDim";
   private static final String TAG_TARGET_CONCENTRATION = "ManaSurveyTargetConcentration";
   private static final String TAG_TARGET_ARRIVED = "ManaSurveyTargetArrived";
   private static final String TAG_TARGET_ARRIVAL_NOTIFIED = "ManaSurveyTargetArrivalNotified";
   private static final int FALLBACK_SCAN_RADIUS_CHUNKS = 8;
   private static final int MAX_SCAN_RADIUS_CHUNKS = 8;
   private static final double SURVEY_MANA_COST = 10.0;
   private final int minRequiredConcentrationExclusive;
   private final int veryHighConcentrationThreshold;

   public ManaSurveyCompassItem(Properties properties, int minRequiredConcentrationExclusive, int veryHighConcentrationThreshold) {
      super(properties);
      this.minRequiredConcentrationExclusive = minRequiredConcentrationExclusive;
      this.veryHighConcentrationThreshold = veryHighConcentrationThreshold;
   }

   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
      ItemStack stack = player.getItemInHand(usedHand);
      if (!(player instanceof ServerPlayer serverPlayer)) {
         return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
      } else {
         CompoundTag tag = ((CustomData)stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)).copyTag();
         int currentConcentration = LeylineService.getConcentration(serverPlayer.serverLevel(), serverPlayer.blockPosition());
         if (currentConcentration > this.minRequiredConcentrationExclusive) {
            clearTarget(tag);
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            serverPlayer.displayClientMessage(
               Component.translatable("message.typemoonworld.mana_survey_compass.current_area_high", new Object[]{currentConcentration}), false
            );
            return InteractionResultHolder.fail(stack);
         } else if (!ManaHelper.consumeManaStrict(serverPlayer, 10.0, false)) {
            serverPlayer.displayClientMessage(Component.translatable("message.typemoonworld.not_enough_mana"), false);
            return InteractionResultHolder.fail(stack);
         } else {
            ManaSurveyCompassItem.ScanResult result = scanBestTarget(serverPlayer);
            if (result != null && result.concentration > this.minRequiredConcentrationExclusive) {
               storeTarget(tag, result.targetPos, result.concentration, serverPlayer.serverLevel());
               serverPlayer.displayClientMessage(
                  Component.translatable("message.typemoonworld.mana_survey_compass.found_target", new Object[]{result.concentration}), false
               );
            } else {
               clearTarget(tag);
               serverPlayer.displayClientMessage(
                  Component.translatable("message.typemoonworld.mana_survey_compass.no_high_mana", new Object[]{this.minRequiredConcentrationExclusive}), false
               );
            }

            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
         }
      }
   }

   public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
      super.inventoryTick(stack, level, entity, slotId, isSelected);
      if (!level.isClientSide() && entity instanceof ServerPlayer serverPlayer) {
         CustomData data = (CustomData)stack.get(DataComponents.CUSTOM_DATA);
         if (data != null) {
            CompoundTag tag = data.copyTag();
            if (tag.getBoolean("ManaSurveyHasTarget") && !tag.getBoolean("ManaSurveyTargetArrived")) {
               if (isTagInCurrentDimension(tag, serverPlayer.serverLevel().dimension().location().toString())) {
                  BlockPos targetPos = readTargetPos(tag);
                  if (isPlayerInTargetChunk(serverPlayer, targetPos)) {
                     tag.putBoolean("ManaSurveyTargetArrived", true);
                     if (!tag.getBoolean("ManaSurveyTargetArrivalNotified")) {
                        int concentration = tag.getInt("ManaSurveyTargetConcentration");
                        boolean veryHigh = concentration >= this.veryHighConcentrationThreshold;
                        int notifyCount = veryHigh ? 3 : 1;
                        String key = veryHigh
                           ? "message.typemoonworld.mana_survey_compass.arrived_very_high"
                           : "message.typemoonworld.mana_survey_compass.arrived";

                        for (int i = 0; i < notifyCount; i++) {
                           serverPlayer.displayClientMessage(Component.translatable(key, new Object[]{concentration}), false);
                        }

                        tag.putBoolean("ManaSurveyTargetArrivalNotified", true);
                     }

                     stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                  }
               }
            }
         }
      }
   }

   @Nullable
   private static ManaSurveyCompassItem.ScanResult scanBestTarget(ServerPlayer player) {
      ServerLevel level = player.serverLevel();
      ChunkPos origin = player.chunkPosition();
      int radius = getScanRadiusChunks(player);
      boolean found = false;
      int bestChunkX = origin.x;
      int bestChunkZ = origin.z;
      int bestConcentration = Integer.MIN_VALUE;
      double bestDistanceSq = Double.MAX_VALUE;
      int sampleY = player.getBlockY();

      for (int dx = -radius; dx <= radius; dx++) {
         int chunkX = origin.x + dx;

         for (int dz = -radius; dz <= radius; dz++) {
            int chunkZ = origin.z + dz;
            BlockPos samplePos = new BlockPos((chunkX << 4) + 8, sampleY, (chunkZ << 4) + 8);
            if (level.isLoaded(samplePos)) {
               int concentration = LeylineService.getConcentration(level, samplePos);
               double distanceSq = (double)dx * dx + (double)dz * dz;
               if (!found || concentration > bestConcentration || concentration == bestConcentration && distanceSq < bestDistanceSq) {
                  found = true;
                  bestChunkX = chunkX;
                  bestChunkZ = chunkZ;
                  bestConcentration = concentration;
                  bestDistanceSq = distanceSq;
               }
            }
         }
      }

      if (!found) {
         return null;
      } else {
         BlockPos targetPos = new BlockPos((bestChunkX << 4) + 8, sampleY, (bestChunkZ << 4) + 8);
         return new ManaSurveyCompassItem.ScanResult(targetPos, bestConcentration);
      }
   }

   private static void storeTarget(CompoundTag tag, BlockPos targetPos, int concentration, ServerLevel level) {
      tag.putBoolean("ManaSurveyHasTarget", true);
      tag.putInt("ManaSurveyTargetX", targetPos.getX());
      tag.putInt("ManaSurveyTargetY", targetPos.getY());
      tag.putInt("ManaSurveyTargetZ", targetPos.getZ());
      tag.putString("ManaSurveyTargetDim", level.dimension().location().toString());
      tag.putInt("ManaSurveyTargetConcentration", concentration);
      tag.putBoolean("ManaSurveyTargetArrived", false);
      tag.putBoolean("ManaSurveyTargetArrivalNotified", false);
   }

   private static void clearTarget(CompoundTag tag) {
      tag.putBoolean("ManaSurveyHasTarget", false);
      tag.remove("ManaSurveyTargetX");
      tag.remove("ManaSurveyTargetY");
      tag.remove("ManaSurveyTargetZ");
      tag.remove("ManaSurveyTargetDim");
      tag.remove("ManaSurveyTargetConcentration");
      tag.remove("ManaSurveyTargetArrived");
      tag.remove("ManaSurveyTargetArrivalNotified");
   }

   private static int getScanRadiusChunks(ServerPlayer player) {
      if (player.getServer() != null) {
         int viewDistance = player.getServer().getPlayerList().getViewDistance();
         if (viewDistance > 0) {
            return Math.max(1, Math.min(viewDistance, 8));
         }
      }

      return 8;
   }

   @Nullable
   public static GlobalPos getStoredTarget(@Nullable ClientLevel level, ItemStack stack) {
      if (level == null) {
         return null;
      } else {
         CustomData data = (CustomData)stack.get(DataComponents.CUSTOM_DATA);
         if (data == null) {
            return null;
         } else {
            CompoundTag tag = data.copyTag();
            if (!tag.getBoolean("ManaSurveyHasTarget")) {
               return null;
            } else if (tag.getBoolean("ManaSurveyTargetArrived")) {
               return null;
            } else {
               return !isTagInCurrentDimension(tag, level.dimension().location().toString()) ? null : GlobalPos.of(level.dimension(), readTargetPos(tag));
            }
         }
      }
   }

   private static boolean isTagInCurrentDimension(CompoundTag tag, String dimensionId) {
      String storedDimensionId = tag.getString("ManaSurveyTargetDim");
      return !storedDimensionId.isEmpty() && storedDimensionId.equals(dimensionId);
   }

   private static BlockPos readTargetPos(CompoundTag tag) {
      return new BlockPos(tag.getInt("ManaSurveyTargetX"), tag.getInt("ManaSurveyTargetY"), tag.getInt("ManaSurveyTargetZ"));
   }

   private static boolean isPlayerInTargetChunk(ServerPlayer player, BlockPos targetPos) {
      ChunkPos playerChunk = player.chunkPosition();
      return playerChunk.x == targetPos.getX() >> 4 && playerChunk.z == targetPos.getZ() >> 4;
   }

   private static final class ScanResult {
      private final BlockPos targetPos;
      private final int concentration;

      private ScanResult(BlockPos targetPos, int concentration) {
         this.targetPos = targetPos;
         this.concentration = concentration;
      }
   }
}
