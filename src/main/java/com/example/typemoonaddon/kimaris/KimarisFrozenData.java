package com.example.typemoonaddon.kimaris;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.INBTSerializable;
import org.jetbrains.annotations.NotNull;

/** Persistent, server-owned restoration data for one frozen entity. */
public final class KimarisFrozenData implements INBTSerializable<CompoundTag> {
    public static final int MAX_DURATION_TICKS = 200;

    // Keep the historical deserialization ceiling so older radius-10 snapshots can be restored.
    public static final int MAX_TEMPORARY_ICE_BLOCKS = 4200;

    private static final int DATA_VERSION = 2;

    private boolean active;
    private UUID casterId;
    private int remainingTicks;
    private long startedAtTick;
    private long scheduledEndTick;
    private boolean freezeDamageApplied;
    private boolean thawDamageApplied;
    private Vec3 savedVelocity = Vec3.ZERO;
    private boolean savedNoGravity;
    private boolean hasSavedMobState;
    private boolean savedNoAi;
    private Vec3 lockedPosition = Vec3.ZERO;
    private float lockedYaw;
    private float lockedPitch;
    private String dimensionId = "";
    private boolean entityStateRestored;
    private final List<TemporaryIceBlock> temporaryIceBlocks = new ArrayList<>();

    private transient long lastProcessedTick = Long.MIN_VALUE;
    private transient long lastPlayerCorrectionTick = Long.MIN_VALUE;

    public boolean isActive() {
        return active && remainingTicks > 0 && casterId != null && !dimensionId.isBlank();
    }

    public UUID casterId() {
        return casterId;
    }

    public int remainingTicks() {
        return remainingTicks;
    }

    public Vec3 savedVelocity() {
        return savedVelocity;
    }

    public boolean savedNoGravity() {
        return savedNoGravity;
    }

    public boolean hasSavedMobState() {
        return hasSavedMobState;
    }

    public boolean savedNoAi() {
        return savedNoAi;
    }

    public Vec3 lockedPosition() {
        return lockedPosition;
    }

    public float lockedYaw() {
        return lockedYaw;
    }

    public float lockedPitch() {
        return lockedPitch;
    }

    public String dimensionId() {
        return dimensionId;
    }

    public boolean freezeDamageApplied() {
        return freezeDamageApplied;
    }

    public boolean thawDamageApplied() {
        return thawDamageApplied;
    }

    public boolean entityStateRestored() {
        return entityStateRestored;
    }

    public void markEntityStateRestored() {
        entityStateRestored = true;
    }

    public List<TemporaryIceBlock> temporaryIceBlocks() {
        return List.copyOf(temporaryIceBlocks);
    }

    public boolean hasTemporaryIceBlocks() {
        return !temporaryIceBlocks.isEmpty();
    }

    public boolean hasTemporaryIceBlock(BlockPos pos) {
        return pos != null && temporaryIceBlocks.stream().anyMatch(block -> block.pos().equals(pos));
    }

    public boolean addTemporaryIceBlock(BlockPos pos, BlockState originalState, BlockState placedState) {
        if (pos == null || originalState == null || placedState == null
                || temporaryIceBlocks.size() >= MAX_TEMPORARY_ICE_BLOCKS
                || hasTemporaryIceBlock(pos)) {
            return false;
        }
        temporaryIceBlocks.add(new TemporaryIceBlock(pos.immutable(), originalState, placedState));
        return true;
    }

    public void removeTemporaryIceBlock(BlockPos pos) {
        if (pos != null) {
            temporaryIceBlocks.removeIf(block -> block.pos().equals(pos));
        }
    }

    public void begin(
            UUID casterId,
            long currentTick,
            int durationTicks,
            Vec3 savedVelocity,
            boolean savedNoGravity,
            boolean hasSavedMobState,
            boolean savedNoAi,
            Vec3 lockedPosition,
            float lockedYaw,
            float lockedPitch,
            String dimensionId
    ) {
        int duration = clampDuration(durationTicks);
        this.active = casterId != null && duration > 0 && dimensionId != null && !dimensionId.isBlank();
        this.casterId = casterId;
        this.remainingTicks = duration;
        this.startedAtTick = Math.max(0L, currentTick);
        this.scheduledEndTick = safeAdd(this.startedAtTick, duration);
        this.freezeDamageApplied = false;
        this.thawDamageApplied = false;
        this.savedVelocity = finiteVector(savedVelocity);
        this.savedNoGravity = savedNoGravity;
        this.hasSavedMobState = hasSavedMobState;
        this.savedNoAi = savedNoAi;
        this.lockedPosition = finiteVector(lockedPosition);
        this.lockedYaw = finiteFloat(lockedYaw);
        this.lockedPitch = finiteFloat(lockedPitch);
        this.dimensionId = dimensionId == null ? "" : dimensionId;
        this.entityStateRestored = false;
        this.temporaryIceBlocks.clear();
        this.lastProcessedTick = currentTick;
        this.lastPlayerCorrectionTick = Long.MIN_VALUE;
    }

    public void refresh(UUID newCasterId, long currentTick, int durationTicks) {
        if (!isActive() || newCasterId == null) {
            return;
        }
        remainingTicks = clampDuration(durationTicks);
        casterId = newCasterId;
        scheduledEndTick = safeAdd(Math.max(0L, currentTick), remainingTicks);
        freezeDamageApplied = true;
        lastProcessedTick = currentTick;
    }

    public void markFreezeDamageApplied() {
        freezeDamageApplied = true;
    }

    public void markThawDamageApplied() {
        thawDamageApplied = true;
    }

    /** Returns true once the persisted remaining duration reaches zero. */
    public boolean advance(long currentTick) {
        if (!isActive() || currentTick == lastProcessedTick) {
            return false;
        }
        lastProcessedTick = currentTick;
        remainingTicks = Math.max(0, remainingTicks - 1);
        scheduledEndTick = safeAdd(Math.max(0L, currentTick), remainingTicks);
        if (remainingTicks == 0) {
            active = false;
            return true;
        }
        return false;
    }

    public boolean shouldCorrectPlayer(long currentTick, int intervalTicks) {
        if (lastPlayerCorrectionTick != Long.MIN_VALUE
                && currentTick >= lastPlayerCorrectionTick
                && currentTick - lastPlayerCorrectionTick < Math.max(1, intervalTicks)) {
            return false;
        }
        lastPlayerCorrectionTick = currentTick;
        return true;
    }

    public void deactivate() {
        active = false;
        remainingTicks = 0;
    }

    @Override
    public @NotNull CompoundTag serializeNBT(@NotNull HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Version", DATA_VERSION);
        tag.putBoolean("Active", isActive());
        if (casterId != null) {
            tag.putUUID("Caster", casterId);
        }
        tag.putInt("RemainingTicks", clampDuration(remainingTicks));
        tag.putLong("StartedAtTick", Math.max(0L, startedAtTick));
        tag.putLong("ScheduledEndTick", Math.max(0L, scheduledEndTick));
        tag.putBoolean("FreezeDamageApplied", freezeDamageApplied);
        tag.putBoolean("ThawDamageApplied", thawDamageApplied);
        putVector(tag, "SavedVelocity", savedVelocity);
        tag.putBoolean("SavedNoGravity", savedNoGravity);
        tag.putBoolean("HasSavedMobState", hasSavedMobState);
        tag.putBoolean("SavedNoAi", savedNoAi);
        putVector(tag, "LockedPosition", lockedPosition);
        tag.putFloat("LockedYaw", finiteFloat(lockedYaw));
        tag.putFloat("LockedPitch", finiteFloat(lockedPitch));
        tag.putString("Dimension", dimensionId == null ? "" : dimensionId);
        tag.putBoolean("EntityStateRestored", entityStateRestored);
        ListTag iceBlocks = new ListTag();
        for (TemporaryIceBlock block : temporaryIceBlocks) {
            CompoundTag blockTag = new CompoundTag();
            blockTag.putLong("Pos", block.pos().asLong());
            blockTag.put("OriginalState", NbtUtils.writeBlockState(block.originalState()));
            blockTag.put("PlacedState", NbtUtils.writeBlockState(block.placedState()));
            iceBlocks.add(blockTag);
        }
        tag.put("TemporaryIceBlocks", iceBlocks);
        return tag;
    }

    @Override
    public void deserializeNBT(@NotNull HolderLookup.Provider provider, @NotNull CompoundTag tag) {
        casterId = tag.hasUUID("Caster") ? tag.getUUID("Caster") : null;
        remainingTicks = tag.contains("RemainingTicks", Tag.TAG_INT)
                ? clampDuration(tag.getInt("RemainingTicks")) : 0;
        startedAtTick = readNonNegativeLong(tag, "StartedAtTick");
        scheduledEndTick = readNonNegativeLong(tag, "ScheduledEndTick");
        freezeDamageApplied = tag.getBoolean("FreezeDamageApplied");
        thawDamageApplied = tag.getBoolean("ThawDamageApplied");
        savedVelocity = readVector(tag, "SavedVelocity");
        savedNoGravity = tag.getBoolean("SavedNoGravity");
        hasSavedMobState = tag.getBoolean("HasSavedMobState");
        savedNoAi = tag.getBoolean("SavedNoAi");
        lockedPosition = readVector(tag, "LockedPosition");
        lockedYaw = finiteFloat(tag.getFloat("LockedYaw"));
        lockedPitch = finiteFloat(tag.getFloat("LockedPitch"));
        dimensionId = tag.contains("Dimension", Tag.TAG_STRING) ? tag.getString("Dimension") : "";
        entityStateRestored = tag.getBoolean("EntityStateRestored");
        temporaryIceBlocks.clear();
        Set<BlockPos> seenPositions = new HashSet<>();
        ListTag iceBlocks = tag.getList("TemporaryIceBlocks", Tag.TAG_COMPOUND);
        var blockLookup = provider.lookupOrThrow(Registries.BLOCK);
        for (int i = 0; i < Math.min(iceBlocks.size(), MAX_TEMPORARY_ICE_BLOCKS); i++) {
            CompoundTag blockTag = iceBlocks.getCompound(i);
            if (!blockTag.contains("Pos", Tag.TAG_LONG)
                    || !blockTag.contains("OriginalState", Tag.TAG_COMPOUND)
                    || !blockTag.contains("PlacedState", Tag.TAG_COMPOUND)) {
                continue;
            }
            BlockPos pos = BlockPos.of(blockTag.getLong("Pos"));
            BlockState originalState = NbtUtils.readBlockState(blockLookup, blockTag.getCompound("OriginalState"));
            BlockState placedState = NbtUtils.readBlockState(blockLookup, blockTag.getCompound("PlacedState"));
            if (seenPositions.add(pos) && isAllowedTemporaryIce(placedState)) {
                temporaryIceBlocks.add(new TemporaryIceBlock(pos, originalState, placedState));
            }
        }
        active = tag.getBoolean("Active") && casterId != null && remainingTicks > 0 && !dimensionId.isBlank();
        if (!active) {
            remainingTicks = 0;
        }
        lastProcessedTick = Long.MIN_VALUE;
        lastPlayerCorrectionTick = Long.MIN_VALUE;
    }

    private static int clampDuration(int value) {
        return Math.max(0, Math.min(MAX_DURATION_TICKS, value));
    }

    private static long safeAdd(long value, long addition) {
        return value > Long.MAX_VALUE - addition ? Long.MAX_VALUE : value + addition;
    }

    private static long readNonNegativeLong(CompoundTag tag, String key) {
        return tag.contains(key, Tag.TAG_LONG) ? Math.max(0L, tag.getLong(key)) : 0L;
    }

    private static void putVector(CompoundTag parent, String key, Vec3 vector) {
        Vec3 safe = finiteVector(vector);
        CompoundTag tag = new CompoundTag();
        tag.putDouble("X", safe.x);
        tag.putDouble("Y", safe.y);
        tag.putDouble("Z", safe.z);
        parent.put(key, tag);
    }

    private static Vec3 readVector(CompoundTag parent, String key) {
        if (!parent.contains(key, Tag.TAG_COMPOUND)) {
            return Vec3.ZERO;
        }
        CompoundTag tag = parent.getCompound(key);
        return finiteVector(new Vec3(tag.getDouble("X"), tag.getDouble("Y"), tag.getDouble("Z")));
    }

    private static Vec3 finiteVector(Vec3 vector) {
        return vector != null
                && Double.isFinite(vector.x)
                && Double.isFinite(vector.y)
                && Double.isFinite(vector.z) ? vector : Vec3.ZERO;
    }

    private static float finiteFloat(float value) {
        return Float.isFinite(value) ? value : 0.0F;
    }

    private static boolean isAllowedTemporaryIce(BlockState state) {
        return state.is(Blocks.PACKED_ICE) || state.is(Blocks.BLUE_ICE);
    }

    public record TemporaryIceBlock(BlockPos pos, BlockState originalState, BlockState placedState) {
    }
}
