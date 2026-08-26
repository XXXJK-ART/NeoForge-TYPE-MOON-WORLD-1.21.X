package io.github.typemoonaddon.data;

import io.github.typemoonaddon.config.GameplayConfig;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.common.util.INBTSerializable;

/** Permanent spiritual-origin damage; the visible effect is only a presentation layer. */
public final class SpiritualDamageData implements INBTSerializable<CompoundTag> {
    public static final StreamCodec<RegistryFriendlyByteBuf, SpiritualDamageData> STREAM_CODEC = StreamCodec.of(
        (buffer, data) -> {
            buffer.writeFloat(data.percent);
            buffer.writeVarLong(data.collapseStartTick);
            buffer.writeVarLong(data.deathDeadline);
        },
        buffer -> {
            SpiritualDamageData data = new SpiritualDamageData();
            data.percent = Mth.clamp(buffer.readFloat(), 0, 100);
            data.collapseStartTick = Math.max(0, buffer.readVarLong());
            data.deathDeadline = Math.max(0, buffer.readVarLong());
            return data;
        }
    );
    private float percent;
    private double manaRemainder;
    private long collapseStartTick;
    private long deathDeadline;
    private boolean collapseExecuting;

    public float percent() { return percent; }
    public long collapseStartTick() { return collapseStartTick; }
    public long deathDeadline() { return deathDeadline; }
    public boolean collapseExecuting() { return collapseExecuting; }
    public void setCollapseExecuting(boolean value) { collapseExecuting = value; }

    public boolean add(float amount, long now) {
        if (amount <= 0 || percent >= 100) return false;
        percent = Mth.clamp(percent + amount, 0, 100);
        updateDeadline(now);
        return true;
    }
    public boolean recordMana(double spent, long now) {
        if (spent <= 0) return false;
        manaRemainder += spent;
        int gained = (int)(manaRemainder / GameplayConfig.SPIRIT_ORIGIN_MANA_PER_PERCENT);
        if (gained <= 0) return false;
        manaRemainder -= gained * GameplayConfig.SPIRIT_ORIGIN_MANA_PER_PERCENT;
        return add(gained, now);
    }
    private void updateDeadline(long now) {
        if (percent <= 50) return;
        if (collapseStartTick == 0) collapseStartTick = Math.max(1L, now);
        long remaining = (long)Math.floor(GameplayConfig.SPIRIT_ORIGIN_MAX_DEATH_TICKS * (100.0 - percent) / 50.0);
        long candidate = Math.max(1L, now + Math.max(0, remaining));
        deathDeadline = deathDeadline == 0 ? candidate : Math.min(deathDeadline, candidate);
    }
    public float collapseProgress(long now) {
        if (percent <= 50 || collapseStartTick <= 0 || deathDeadline <= 0) return 0.0F;
        if (deathDeadline <= collapseStartTick) return 1.0F;
        return Mth.clamp((float)(now - collapseStartTick) / (float)(deathDeadline - collapseStartTick), 0.0F, 1.0F);
    }
    public void clearAfterCollapse() {
        percent = 0.0F;
        manaRemainder = 0.0D;
        collapseStartTick = 0L;
        deathDeadline = 0L;
        collapseExecuting = false;
    }
    @Override public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag(); tag.putFloat("Percent", percent); tag.putDouble("ManaRemainder", manaRemainder); tag.putLong("CollapseStartTick", collapseStartTick); tag.putLong("DeathDeadline", deathDeadline); return tag;
    }
    @Override public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        percent = Mth.clamp(tag.getFloat("Percent"), 0, 100);
        manaRemainder = Math.max(0, tag.getDouble("ManaRemainder"));
        collapseStartTick = Math.max(0, tag.getLong("CollapseStartTick"));
        deathDeadline = Math.max(0, tag.getLong("DeathDeadline"));
        if (percent > 50 && collapseStartTick == 0 && deathDeadline > 0) {
            long legacyDuration = (long)Math.floor(GameplayConfig.SPIRIT_ORIGIN_MAX_DEATH_TICKS * (100.0 - percent) / 50.0);
            collapseStartTick = Math.max(1, deathDeadline - Math.max(0, legacyDuration));
        }
        collapseExecuting = false;
    }
}
