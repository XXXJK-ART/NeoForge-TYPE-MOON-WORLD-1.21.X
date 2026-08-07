package com.example.typemoonaddon.imaginary_space;

import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.neoforged.neoforge.common.util.INBTSerializable;
import org.jetbrains.annotations.NotNull;

/** Persistent, server-owned player state for one imaginary-space session. */
public final class ImaginarySpaceData implements INBTSerializable<CompoundTag> {
    public static final int SELF_MODE = 0;
    public static final int CREATURE_MODE = 1;
    public static final int ENTRY_INVULNERABILITY_TICKS = 600;
    public static final double INITIAL_DEPTH = 10.0D;
    public static final double MAX_DEPTH = 100.0D;
    public static final double MAX_TIME_OFFSET = 1.0E12D;

    private int mode;
    private boolean active;
    private UUID instanceId;
    private String returnDimension = "";
    private double returnX;
    private double returnY;
    private double returnZ;
    private float returnYaw;
    private float returnPitch;
    private double safeX;
    private double safeY;
    private double safeZ;
    private double centerX;
    private double centerY;
    private double centerZ;
    private int entryChunkX;
    private int entryChunkZ;
    private double existence = 100.0D;
    private double depth;
    private double timeOffset;
    private int invulnerabilityTicks;
    private boolean previousNoGravity;
    private int sessionTicks;
    private int existenceTicker;
    private int timeTicker;
    private int generation;
    private int outerGodCountdown = -1;
    private boolean contentsCleared;
    private boolean highDepthOverride;
    private int previousSelectedSlot = -1;

    public int mode() {
        return mode;
    }

    public void setMode(int mode) {
        this.mode = mode == CREATURE_MODE ? CREATURE_MODE : SELF_MODE;
    }

    public int cycleMode() {
        setMode(mode == SELF_MODE ? CREATURE_MODE : SELF_MODE);
        return mode;
    }

    public boolean active() {
        return active && instanceId != null;
    }

    public UUID instanceId() {
        return instanceId;
    }

    public String returnDimension() {
        return returnDimension;
    }

    public double returnX() {
        return returnX;
    }

    public double returnY() {
        return returnY;
    }

    public double returnZ() {
        return returnZ;
    }

    public float returnYaw() {
        return returnYaw;
    }

    public float returnPitch() {
        return returnPitch;
    }

    public double safeX() {
        return safeX;
    }

    public double safeY() {
        return safeY;
    }

    public double safeZ() {
        return safeZ;
    }

    public double centerX() {
        return centerX;
    }

    public double centerY() {
        return centerY;
    }

    public double centerZ() {
        return centerZ;
    }

    public int entryChunkX() {
        return entryChunkX;
    }

    public int entryChunkZ() {
        return entryChunkZ;
    }

    public double existence() {
        existence = clampExistence(existence);
        return existence;
    }

    public void setExistence(double existence) {
        this.existence = clampExistence(existence);
    }

    public double depth() {
        depth = sanitizeDepth(depth);
        return depth;
    }

    public boolean setDepth(double depth) {
        double sanitized = sanitizeDepth(depth);
        if (Double.compare(this.depth(), sanitized) == 0) {
            return false;
        }
        this.depth = sanitized;
        return true;
    }

    public double timeOffset() {
        timeOffset = sanitizeTimeOffset(timeOffset);
        return timeOffset;
    }

    public void setTimeOffset(double timeOffset) {
        this.timeOffset = sanitizeTimeOffset(timeOffset);
    }

    public void addTimeOffset(double delta) {
        if (!Double.isFinite(delta)) {
            return;
        }
        setTimeOffset(timeOffset() + delta);
    }

    public int invulnerabilityTicks() {
        return Math.max(0, invulnerabilityTicks);
    }

    public void tickInvulnerability() {
        if (invulnerabilityTicks > 0) {
            invulnerabilityTicks--;
        }
    }

    public boolean previousNoGravity() {
        return previousNoGravity;
    }

    public int sessionTicks() {
        return Math.max(0, sessionTicks);
    }

    public void tickSession() {
        if (sessionTicks < Integer.MAX_VALUE) {
            sessionTicks++;
        }
    }

    public boolean advanceExistenceTicker(int interval) {
        existenceTicker++;
        if (existenceTicker < Math.max(1, interval)) {
            return false;
        }
        existenceTicker = 0;
        return true;
    }

    public boolean advanceTimeTicker(int interval) {
        timeTicker++;
        if (timeTicker < Math.max(1, interval)) {
            return false;
        }
        timeTicker = 0;
        return true;
    }

    public int generation() {
        return Math.max(0, generation);
    }

    public int nextGeneration() {
        generation = generation == Integer.MAX_VALUE ? 1 : generation + 1;
        return generation;
    }

    public int outerGodCountdown() {
        return outerGodCountdown;
    }

    public void startOuterGodCountdown(int ticks) {
        if (outerGodCountdown < 0) {
            outerGodCountdown = Math.max(1, ticks);
        }
    }

    public boolean tickOuterGodCountdown() {
        if (outerGodCountdown < 0) {
            return false;
        }
        if (outerGodCountdown > 0) {
            outerGodCountdown--;
        }
        return outerGodCountdown == 0;
    }

    public void clearOuterGodCountdown() {
        outerGodCountdown = -1;
    }

    public boolean contentsCleared() {
        return contentsCleared;
    }

    public void markContentsCleared() {
        contentsCleared = true;
    }

    public void markContentsGenerated() {
        contentsCleared = false;
    }

    public boolean highDepthOverride() {
        return highDepthOverride;
    }

    public int previousSelectedSlot() {
        return previousSelectedSlot;
    }

    public void markHighDepthOverride(int selectedSlot) {
        highDepthOverride = true;
        previousSelectedSlot = Math.max(0, Math.min(8, selectedSlot));
    }

    public void clearHighDepthOverride() {
        highDepthOverride = false;
        previousSelectedSlot = -1;
    }

    public void beginSession(
            UUID instanceId,
            String returnDimension,
            double returnX,
            double returnY,
            double returnZ,
            float returnYaw,
            float returnPitch,
            double safeX,
            double safeY,
            double safeZ,
            double centerX,
            double centerY,
            double centerZ,
            int entryChunkX,
            int entryChunkZ,
            boolean previousNoGravity
    ) {
        this.active = instanceId != null;
        this.instanceId = instanceId;
        this.returnDimension = returnDimension == null ? "" : returnDimension;
        this.returnX = finiteOrZero(returnX);
        this.returnY = finiteOrZero(returnY);
        this.returnZ = finiteOrZero(returnZ);
        this.returnYaw = Float.isFinite(returnYaw) ? returnYaw : 0.0F;
        this.returnPitch = Float.isFinite(returnPitch) ? returnPitch : 0.0F;
        this.safeX = finiteOrZero(safeX);
        this.safeY = finiteOrZero(safeY);
        this.safeZ = finiteOrZero(safeZ);
        this.centerX = finiteOrZero(centerX);
        this.centerY = finiteOrZero(centerY);
        this.centerZ = finiteOrZero(centerZ);
        this.entryChunkX = entryChunkX;
        this.entryChunkZ = entryChunkZ;
        this.existence = 100.0D;
        this.depth = INITIAL_DEPTH;
        this.timeOffset = 0.0D;
        this.invulnerabilityTicks = ENTRY_INVULNERABILITY_TICKS;
        this.previousNoGravity = previousNoGravity;
        this.sessionTicks = 0;
        this.existenceTicker = 0;
        this.timeTicker = 0;
        this.generation = 0;
        this.outerGodCountdown = -1;
        this.contentsCleared = false;
        this.highDepthOverride = false;
        this.previousSelectedSlot = -1;
    }

    public void clearSession() {
        active = false;
        instanceId = null;
        returnDimension = "";
        returnX = 0.0D;
        returnY = 0.0D;
        returnZ = 0.0D;
        returnYaw = 0.0F;
        returnPitch = 0.0F;
        safeX = 0.0D;
        safeY = 0.0D;
        safeZ = 0.0D;
        centerX = 0.0D;
        centerY = 0.0D;
        centerZ = 0.0D;
        entryChunkX = 0;
        entryChunkZ = 0;
        existence = 100.0D;
        depth = 0.0D;
        timeOffset = 0.0D;
        invulnerabilityTicks = 0;
        previousNoGravity = false;
        sessionTicks = 0;
        existenceTicker = 0;
        timeTicker = 0;
        generation = 0;
        outerGodCountdown = -1;
        contentsCleared = false;
        highDepthOverride = false;
        previousSelectedSlot = -1;
    }

    public void applyClientState(
            int mode,
            boolean active,
            double existence,
            double depth,
            double timeOffset,
            int invulnerabilityTicks
    ) {
        setMode(mode);
        this.active = active;
        this.existence = clampExistence(existence);
        this.depth = sanitizeDepth(depth);
        this.timeOffset = sanitizeTimeOffset(timeOffset);
        this.invulnerabilityTicks = Math.max(0, Math.min(ENTRY_INVULNERABILITY_TICKS, invulnerabilityTicks));
        if (!active) {
            this.instanceId = null;
        } else if (this.instanceId == null) {
            this.instanceId = new UUID(0L, 1L);
        }
    }

    @Override
    public @NotNull CompoundTag serializeNBT(@NotNull HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Mode", mode());
        tag.putBoolean("Active", active());
        if (instanceId != null) {
            tag.putUUID("InstanceId", instanceId);
        }
        tag.putString("ReturnDimension", returnDimension);
        tag.putDouble("ReturnX", finiteOrZero(returnX));
        tag.putDouble("ReturnY", finiteOrZero(returnY));
        tag.putDouble("ReturnZ", finiteOrZero(returnZ));
        tag.putFloat("ReturnYaw", Float.isFinite(returnYaw) ? returnYaw : 0.0F);
        tag.putFloat("ReturnPitch", Float.isFinite(returnPitch) ? returnPitch : 0.0F);
        tag.putDouble("SafeX", finiteOrZero(safeX));
        tag.putDouble("SafeY", finiteOrZero(safeY));
        tag.putDouble("SafeZ", finiteOrZero(safeZ));
        tag.putDouble("CenterX", finiteOrZero(centerX));
        tag.putDouble("CenterY", finiteOrZero(centerY));
        tag.putDouble("CenterZ", finiteOrZero(centerZ));
        tag.putInt("EntryChunkX", entryChunkX);
        tag.putInt("EntryChunkZ", entryChunkZ);
        tag.putDouble("Existence", existence());
        tag.putDouble("Depth", depth());
        tag.putDouble("TimeOffset", timeOffset());
        tag.putInt("InvulnerabilityTicks", invulnerabilityTicks());
        tag.putBoolean("PreviousNoGravity", previousNoGravity);
        tag.putInt("SessionTicks", sessionTicks());
        tag.putInt("ExistenceTicker", Math.max(0, existenceTicker));
        tag.putInt("TimeTicker", Math.max(0, timeTicker));
        tag.putInt("Generation", generation());
        tag.putInt("OuterGodCountdown", outerGodCountdown);
        tag.putBoolean("ContentsCleared", contentsCleared);
        tag.putBoolean("HighDepthOverride", highDepthOverride);
        tag.putInt("PreviousSelectedSlot", previousSelectedSlot);
        return tag;
    }

    @Override
    public void deserializeNBT(@NotNull HolderLookup.Provider provider, @NotNull CompoundTag tag) {
        setMode(tag.contains("Mode", Tag.TAG_INT) ? tag.getInt("Mode") : SELF_MODE);
        instanceId = tag.hasUUID("InstanceId") ? tag.getUUID("InstanceId") : null;
        active = tag.getBoolean("Active") && instanceId != null;
        returnDimension = tag.getString("ReturnDimension");
        returnX = finiteOrZero(tag.getDouble("ReturnX"));
        returnY = finiteOrZero(tag.getDouble("ReturnY"));
        returnZ = finiteOrZero(tag.getDouble("ReturnZ"));
        returnYaw = Float.isFinite(tag.getFloat("ReturnYaw")) ? tag.getFloat("ReturnYaw") : 0.0F;
        returnPitch = Float.isFinite(tag.getFloat("ReturnPitch")) ? tag.getFloat("ReturnPitch") : 0.0F;
        safeX = finiteOrZero(tag.getDouble("SafeX"));
        safeY = finiteOrZero(tag.getDouble("SafeY"));
        safeZ = finiteOrZero(tag.getDouble("SafeZ"));
        centerX = finiteOrZero(tag.getDouble("CenterX"));
        centerY = finiteOrZero(tag.getDouble("CenterY"));
        centerZ = finiteOrZero(tag.getDouble("CenterZ"));
        entryChunkX = tag.getInt("EntryChunkX");
        entryChunkZ = tag.getInt("EntryChunkZ");
        existence = tag.contains("Existence", Tag.TAG_DOUBLE) ? clampExistence(tag.getDouble("Existence")) : 100.0D;
        depth = sanitizeDepth(tag.getDouble("Depth"));
        timeOffset = sanitizeTimeOffset(tag.getDouble("TimeOffset"));
        invulnerabilityTicks = Math.max(0, Math.min(ENTRY_INVULNERABILITY_TICKS, tag.getInt("InvulnerabilityTicks")));
        previousNoGravity = tag.getBoolean("PreviousNoGravity");
        sessionTicks = Math.max(0, tag.getInt("SessionTicks"));
        existenceTicker = Math.max(0, tag.getInt("ExistenceTicker"));
        timeTicker = Math.max(0, tag.getInt("TimeTicker"));
        generation = Math.max(0, tag.getInt("Generation"));
        outerGodCountdown = tag.contains("OuterGodCountdown", Tag.TAG_INT) ? tag.getInt("OuterGodCountdown") : -1;
        contentsCleared = tag.getBoolean("ContentsCleared");
        highDepthOverride = tag.getBoolean("HighDepthOverride");
        previousSelectedSlot = Math.max(-1, Math.min(8, tag.getInt("PreviousSelectedSlot")));
    }

    private static double clampExistence(double value) {
        if (!Double.isFinite(value)) {
            return 100.0D;
        }
        return Math.max(0.0D, Math.min(100.0D, value));
    }

    private static double sanitizeDepth(double value) {
        return Double.isFinite(value) ? Math.max(0.0D, Math.min(MAX_DEPTH, value)) : 0.0D;
    }

    private static double sanitizeTimeOffset(double value) {
        if (!Double.isFinite(value)) {
            return 0.0D;
        }
        return Math.max(-MAX_TIME_OFFSET, Math.min(MAX_TIME_OFFSET, value));
    }

    private static double finiteOrZero(double value) {
        return Double.isFinite(value) ? value : 0.0D;
    }
}
