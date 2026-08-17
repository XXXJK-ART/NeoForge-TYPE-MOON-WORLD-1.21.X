package com.example.typemoonaddon.data;

import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.common.util.INBTSerializable;

/** Persistent corruption state, deliberately independent from removable mob effects. */
public final class PollutionData implements INBTSerializable<CompoundTag> {
    public static final StreamCodec<RegistryFriendlyByteBuf, PollutionData> STREAM_CODEC = StreamCodec.of(
        (buffer, data) -> data.write(buffer),
        PollutionData::read
    );

    private float progress;
    private float originHeight = 0.5F;
    private boolean fullyCorrupted;
    @Nullable
    private UUID controllerId;
    @Nullable
    private UUID rosterId;
    private long continuousExposureUntil;
    private float continuousExposureMultiplier = 1.0F;
    private long lastContinuousExposureTick = Long.MIN_VALUE;
    private long lastSingleExposureTick = Long.MIN_VALUE;

    public float progress() {
        return progress;
    }

    public boolean fullyCorrupted() {
        return fullyCorrupted;
    }

    @Nullable
    public UUID controllerId() {
        return controllerId;
    }

    @Nullable
    public UUID rosterId() {
        return rosterId;
    }

    public float originHeight() {
        return originHeight;
    }

    public boolean continuouslyExposed(long gameTime) {
        return gameTime <= continuousExposureUntil;
    }

    public float exposureMultiplier(long gameTime) {
        return continuouslyExposed(gameTime) ? Math.max(1.0F, continuousExposureMultiplier) : 1.0F;
    }

    public boolean begin(UUID controllerId, float relativeOriginHeight) {
        if (this.controllerId != null || fullyCorrupted) {
            return false;
        }
        this.controllerId = controllerId;
        this.originHeight = Mth.clamp(relativeOriginHeight, 0.0F, 1.0F);
        this.progress = Math.max(progress, 0.01F);
        return true;
    }

    public boolean expose(long gameTime, boolean continuous, float multiplier) {
        boolean changed = false;
        if (continuous) {
            if (gameTime != lastContinuousExposureTick) {
                continuousExposureMultiplier = 1.0F;
            }
            lastContinuousExposureTick = gameTime;
            continuousExposureUntil = Math.max(continuousExposureUntil, gameTime + 5L);
            continuousExposureMultiplier = Math.max(continuousExposureMultiplier, Math.max(1.0F, multiplier));
        } else if (lastSingleExposureTick != gameTime) {
            lastSingleExposureTick = gameTime;
            changed = addProgress(0.025F);
        }
        return changed;
    }

    public boolean addProgress(float amount) {
        if (fullyCorrupted || amount <= 0.0F) {
            return false;
        }
        float previous = progress;
        progress = Mth.clamp(progress + amount, 0.0F, 1.0F);
        return progress != previous;
    }

    public boolean reduceProgress(float amount) {
        if (fullyCorrupted || amount <= 0.0F || progress <= 0.0F) {
            return false;
        }
        float previous = progress;
        progress = Math.max(0.0F, progress - amount);
        if (progress <= 0.0F) {
            controllerId = null;
            rosterId = null;
            continuousExposureUntil = 0L;
            continuousExposureMultiplier = 1.0F;
            lastContinuousExposureTick = Long.MIN_VALUE;
        }
        return progress != previous;
    }

    public boolean capBelowFull() {
        if (progress < 0.99F) {
            return false;
        }
        progress = 0.99F;
        return true;
    }

    public void complete(@Nullable UUID rosterId) {
        progress = 1.0F;
        fullyCorrupted = true;
        this.rosterId = rosterId;
    }

    public void restoreComplete(UUID controllerId, UUID rosterId) {
        this.controllerId = controllerId;
        this.rosterId = rosterId;
        this.progress = 1.0F;
        this.fullyCorrupted = true;
    }

    public void clearForImmunity() {
        progress = 0.0F;
        fullyCorrupted = false;
        controllerId = null;
        rosterId = null;
        continuousExposureUntil = 0L;
        continuousExposureMultiplier = 1.0F;
        lastContinuousExposureTick = Long.MIN_VALUE;
        lastSingleExposureTick = Long.MIN_VALUE;
    }

    public void clearForDispel() {
        clearForImmunity();
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putFloat("Progress", progress);
        tag.putFloat("OriginHeight", originHeight);
        tag.putBoolean("FullyCorrupted", fullyCorrupted);
        if (controllerId != null) {
            tag.putUUID("Controller", controllerId);
        }
        if (rosterId != null) {
            tag.putUUID("RosterId", rosterId);
        }
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        progress = Mth.clamp(tag.getFloat("Progress"), 0.0F, 1.0F);
        originHeight = tag.contains("OriginHeight") ? Mth.clamp(tag.getFloat("OriginHeight"), 0.0F, 1.0F) : 0.5F;
        fullyCorrupted = tag.getBoolean("FullyCorrupted");
        controllerId = tag.hasUUID("Controller") ? tag.getUUID("Controller") : null;
        rosterId = tag.hasUUID("RosterId") ? tag.getUUID("RosterId") : null;
        if (fullyCorrupted) {
            progress = 1.0F;
        }
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeFloat(progress);
        buffer.writeFloat(originHeight);
        buffer.writeBoolean(fullyCorrupted);
        buffer.writeBoolean(controllerId != null);
        if (controllerId != null) {
            buffer.writeUUID(controllerId);
        }
        buffer.writeBoolean(rosterId != null);
        if (rosterId != null) {
            buffer.writeUUID(rosterId);
        }
    }

    private static PollutionData read(RegistryFriendlyByteBuf buffer) {
        PollutionData data = new PollutionData();
        data.progress = Mth.clamp(buffer.readFloat(), 0.0F, 1.0F);
        data.originHeight = Mth.clamp(buffer.readFloat(), 0.0F, 1.0F);
        data.fullyCorrupted = buffer.readBoolean();
        data.controllerId = buffer.readBoolean() ? buffer.readUUID() : null;
        data.rosterId = buffer.readBoolean() ? buffer.readUUID() : null;
        return data;
    }
}


