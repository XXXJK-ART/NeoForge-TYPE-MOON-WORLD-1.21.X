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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.entity.player.Player;
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
    private static final int MIN_REQUIRED_CONCENTRATION_EXCLUSIVE = 50;
    private static final int VERY_HIGH_CONCENTRATION_THRESHOLD = 80;
    private static final int APPROX_COORDINATE_STEP_BLOCKS = 32;
    private static final double SURVEY_MANA_COST = 10.0;

    public ManaSurveyCompassItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }

        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        int currentConcentration = LeylineService.getConcentration(serverPlayer.serverLevel(), serverPlayer.blockPosition());
        if (currentConcentration > MIN_REQUIRED_CONCENTRATION_EXCLUSIVE) {
            clearTarget(tag);
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            serverPlayer.displayClientMessage(
                    Component.translatable(
                            "message.typemoonworld.mana_survey_compass.current_area_high",
                            currentConcentration
                    ),
                    false
            );
            return InteractionResultHolder.fail(stack);
        }

        if (!ManaHelper.consumeManaStrict(serverPlayer, SURVEY_MANA_COST, false)) {
            serverPlayer.displayClientMessage(Component.translatable("message.typemoonworld.not_enough_mana"), false);
            return InteractionResultHolder.fail(stack);
        }

        ScanResult result = scanBestTarget(serverPlayer);
        if (result == null || result.concentration <= MIN_REQUIRED_CONCENTRATION_EXCLUSIVE) {
            clearTarget(tag);
            serverPlayer.displayClientMessage(
                    Component.translatable("message.typemoonworld.mana_survey_compass.no_high_mana", MIN_REQUIRED_CONCENTRATION_EXCLUSIVE),
                    false
            );
        } else {
            storeTarget(tag, result.targetPos, result.concentration, serverPlayer.serverLevel());
            int approxX = approximateCoordinate(result.targetPos.getX());
            int approxZ = approximateCoordinate(result.targetPos.getZ());
            serverPlayer.displayClientMessage(
                    Component.translatable(
                            "message.typemoonworld.mana_survey_compass.found_target",
                            approxX,
                            approxZ,
                            result.concentration
                    ),
                    false
            );
        }
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (level.isClientSide() || !(entity instanceof ServerPlayer serverPlayer)) {
            return;
        }

        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return;
        }

        CompoundTag tag = data.copyTag();
        if (!tag.getBoolean(TAG_HAS_TARGET) || tag.getBoolean(TAG_TARGET_ARRIVED)) {
            return;
        }

        if (!isTagInCurrentDimension(tag, serverPlayer.serverLevel().dimension().location().toString())) {
            return;
        }

        BlockPos targetPos = readTargetPos(tag);
        if (!isPlayerInTargetChunk(serverPlayer, targetPos)) {
            return;
        }

        tag.putBoolean(TAG_TARGET_ARRIVED, true);
        if (!tag.getBoolean(TAG_TARGET_ARRIVAL_NOTIFIED)) {
            int concentration = tag.getInt(TAG_TARGET_CONCENTRATION);
            boolean veryHigh = concentration >= VERY_HIGH_CONCENTRATION_THRESHOLD;
            int notifyCount = veryHigh ? 3 : 1;
            String key = veryHigh
                    ? "message.typemoonworld.mana_survey_compass.arrived_very_high"
                    : "message.typemoonworld.mana_survey_compass.arrived";

            for (int i = 0; i < notifyCount; i++) {
                serverPlayer.displayClientMessage(Component.translatable(key, concentration), false);
            }
            tag.putBoolean(TAG_TARGET_ARRIVAL_NOTIFIED, true);
        }

        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    @Nullable
    private static ScanResult scanBestTarget(ServerPlayer player) {
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
                if (!level.isLoaded(samplePos)) {
                    continue;
                }

                int concentration = LeylineService.getConcentration(level, samplePos);
                double distanceSq = (dx * (double) dx) + (dz * (double) dz);
                if (!found
                        || concentration > bestConcentration
                        || (concentration == bestConcentration && distanceSq < bestDistanceSq)) {
                    found = true;
                    bestChunkX = chunkX;
                    bestChunkZ = chunkZ;
                    bestConcentration = concentration;
                    bestDistanceSq = distanceSq;
                }
            }
        }

        if (!found) {
            return null;
        }

        BlockPos targetPos = new BlockPos((bestChunkX << 4) + 8, sampleY, (bestChunkZ << 4) + 8);
        return new ScanResult(targetPos, bestConcentration);
    }

    private static void storeTarget(CompoundTag tag, BlockPos targetPos, int concentration, ServerLevel level) {
        tag.putBoolean(TAG_HAS_TARGET, true);
        tag.putInt(TAG_TARGET_X, targetPos.getX());
        tag.putInt(TAG_TARGET_Y, targetPos.getY());
        tag.putInt(TAG_TARGET_Z, targetPos.getZ());
        tag.putString(TAG_TARGET_DIM, level.dimension().location().toString());
        tag.putInt(TAG_TARGET_CONCENTRATION, concentration);
        tag.putBoolean(TAG_TARGET_ARRIVED, false);
        tag.putBoolean(TAG_TARGET_ARRIVAL_NOTIFIED, false);
    }

    private static void clearTarget(CompoundTag tag) {
        tag.putBoolean(TAG_HAS_TARGET, false);
        tag.remove(TAG_TARGET_X);
        tag.remove(TAG_TARGET_Y);
        tag.remove(TAG_TARGET_Z);
        tag.remove(TAG_TARGET_DIM);
        tag.remove(TAG_TARGET_CONCENTRATION);
        tag.remove(TAG_TARGET_ARRIVED);
        tag.remove(TAG_TARGET_ARRIVAL_NOTIFIED);
    }

    private static int getScanRadiusChunks(ServerPlayer player) {
        if (player.getServer() != null) {
            int viewDistance = player.getServer().getPlayerList().getViewDistance();
            if (viewDistance > 0) {
                return Math.max(1, Math.min(viewDistance, MAX_SCAN_RADIUS_CHUNKS));
            }
        }
        return FALLBACK_SCAN_RADIUS_CHUNKS;
    }

    @Nullable
    public static GlobalPos getStoredTarget(@Nullable ClientLevel level, ItemStack stack) {
        if (level == null) {
            return null;
        }

        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return null;
        }

        CompoundTag tag = data.copyTag();
        if (!tag.getBoolean(TAG_HAS_TARGET)) {
            return null;
        }

        if (tag.getBoolean(TAG_TARGET_ARRIVED)) {
            return null;
        }

        if (!isTagInCurrentDimension(tag, level.dimension().location().toString())) {
            return null;
        }

        return GlobalPos.of(level.dimension(), readTargetPos(tag));
    }

    private static boolean isTagInCurrentDimension(CompoundTag tag, String dimensionId) {
        String storedDimensionId = tag.getString(TAG_TARGET_DIM);
        return !storedDimensionId.isEmpty() && storedDimensionId.equals(dimensionId);
    }

    private static BlockPos readTargetPos(CompoundTag tag) {
        return new BlockPos(tag.getInt(TAG_TARGET_X), tag.getInt(TAG_TARGET_Y), tag.getInt(TAG_TARGET_Z));
    }

    private static boolean isPlayerInTargetChunk(ServerPlayer player, BlockPos targetPos) {
        ChunkPos playerChunk = player.chunkPosition();
        return playerChunk.x == (targetPos.getX() >> 4) && playerChunk.z == (targetPos.getZ() >> 4);
    }

    private static int approximateCoordinate(int value) {
        return Math.round(value / (float) APPROX_COORDINATE_STEP_BLOCKS) * APPROX_COORDINATE_STEP_BLOCKS;
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
