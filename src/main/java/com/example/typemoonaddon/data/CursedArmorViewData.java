package com.example.typemoonaddon.data;

import com.example.typemoonaddon.data.ImaginarySpaceData.CursedArmorState;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/** Small public mirror of the private player attachment for tracking clients. */
public final class CursedArmorViewData {
    public static final StreamCodec<RegistryFriendlyByteBuf, CursedArmorViewData> STREAM_CODEC = StreamCodec.of(
        (buffer, data) -> {
            buffer.writeEnum(data.state);
            buffer.writeVarLong(data.stageStartTick);
        },
        buffer -> new CursedArmorViewData(
            buffer.readEnum(CursedArmorState.class),
            buffer.readVarLong()
        )
    );

    private CursedArmorState state;
    private long stageStartTick;

    public CursedArmorViewData() {
        this(CursedArmorState.NONE, 0L);
    }

    private CursedArmorViewData(CursedArmorState state, long stageStartTick) {
        this.state = state;
        this.stageStartTick = stageStartTick;
    }

    public CursedArmorState state() {
        return state;
    }

    public long stageStartTick() {
        return stageStartTick;
    }

    public void set(CursedArmorState state, long stageStartTick) {
        this.state = state == null ? CursedArmorState.NONE : state;
        this.stageStartTick = Math.max(0L, stageStartTick);
    }
}


