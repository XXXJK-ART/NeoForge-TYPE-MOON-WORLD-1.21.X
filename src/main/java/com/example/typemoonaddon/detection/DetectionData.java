package com.example.typemoonaddon.detection;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/** Non-persistent, server-owned state for one player's detection toggle. */
public final class DetectionData {
    private boolean active;
    private ResourceKey<Level> dimension;
    private long nextScanTick;
    private long nextEyeSyncTick;
    private final Map<Integer, Byte> targets = new LinkedHashMap<>();

    public boolean isActive() {
        return active;
    }

    public ResourceKey<Level> dimension() {
        return dimension;
    }

    public long nextScanTick() {
        return nextScanTick;
    }

    public long nextEyeSyncTick() {
        return nextEyeSyncTick;
    }

    public void activate(ResourceKey<Level> dimension, long gameTime) {
        this.active = dimension != null;
        this.dimension = dimension;
        this.nextScanTick = Math.max(0L, gameTime);
        this.nextEyeSyncTick = Math.max(0L, gameTime);
        this.targets.clear();
    }

    public void scheduleNextScan(long gameTime, int intervalTicks) {
        nextScanTick = Math.max(0L, gameTime) + Math.max(1, intervalTicks);
    }

    public void scheduleNextEyeSync(long gameTime, int intervalTicks) {
        nextEyeSyncTick = Math.max(0L, gameTime) + Math.max(1, intervalTicks);
    }

    public Map<Integer, Byte> targets() {
        return Map.copyOf(targets);
    }

    public boolean replaceTargets(Map<Integer, Byte> replacement) {
        Map<Integer, Byte> safe = replacement == null ? Map.of() : replacement;
        if (targets.equals(safe)) {
            return false;
        }
        targets.clear();
        targets.putAll(safe);
        return true;
    }

    public void deactivate() {
        active = false;
        dimension = null;
        nextScanTick = 0L;
        nextEyeSyncTick = 0L;
        targets.clear();
    }
}
