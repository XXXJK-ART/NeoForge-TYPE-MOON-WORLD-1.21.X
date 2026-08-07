package com.example.typemoonaddon.magic;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.neoforged.neoforge.common.util.INBTSerializable;
import org.jetbrains.annotations.NotNull;

/** Server-owned remaining-time state for Imaginary Number Displacement. */
public final class ImaginaryDisplacementData implements INBTSerializable<CompoundTag> {
    public static final int ACTIVE_DURATION_TICKS = 200;
    public static final int COOLDOWN_TICKS = 300;

    private static final String ACTIVE_KEY = "ActiveRemainingTicks";
    private static final String COOLDOWN_KEY = "CooldownRemainingTicks";

    private int activeRemainingTicks;
    private int cooldownRemainingTicks;
    private long lastReflectEffectTick = Long.MIN_VALUE;

    public int activeRemainingTicks() {
        return activeRemainingTicks;
    }

    public int cooldownRemainingTicks() {
        return cooldownRemainingTicks;
    }

    public boolean isActive() {
        return activeRemainingTicks > 0;
    }

    public boolean isCoolingDown() {
        return cooldownRemainingTicks > 0;
    }

    public void activate() {
        activeRemainingTicks = ACTIVE_DURATION_TICKS;
        cooldownRemainingTicks = COOLDOWN_TICKS;
    }

    /** Advances both timers by one server tick and reports an active-state transition. */
    public boolean tick() {
        boolean ended = activeRemainingTicks == 1;
        if (activeRemainingTicks > 0) {
            activeRemainingTicks--;
        }
        if (cooldownRemainingTicks > 0) {
            cooldownRemainingTicks--;
        }
        return ended;
    }

    public void clearActive() {
        activeRemainingTicks = 0;
    }

    public boolean tryStartReflectEffect(long currentTick) {
        if (lastReflectEffectTick != Long.MIN_VALUE
                && currentTick >= lastReflectEffectTick
                && currentTick - lastReflectEffectTick < 2L) {
            return false;
        }
        lastReflectEffectTick = currentTick;
        return true;
    }

    @Override
    public @NotNull CompoundTag serializeNBT(@NotNull HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putInt(ACTIVE_KEY, Math.max(0, Math.min(ACTIVE_DURATION_TICKS, activeRemainingTicks)));
        tag.putInt(COOLDOWN_KEY, Math.max(0, Math.min(COOLDOWN_TICKS, cooldownRemainingTicks)));
        return tag;
    }

    @Override
    public void deserializeNBT(@NotNull HolderLookup.Provider provider, @NotNull CompoundTag tag) {
        activeRemainingTicks = readClamped(tag, ACTIVE_KEY, ACTIVE_DURATION_TICKS);
        cooldownRemainingTicks = readClamped(tag, COOLDOWN_KEY, COOLDOWN_TICKS);
    }

    private static int readClamped(CompoundTag tag, String key, int max) {
        if (!tag.contains(key, Tag.TAG_INT)) {
            return 0;
        }
        return Math.max(0, Math.min(max, tag.getInt(key)));
    }
}
