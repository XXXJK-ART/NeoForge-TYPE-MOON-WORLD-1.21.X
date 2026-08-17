package com.example.typemoonaddon.network;

import com.example.typemoonaddon.TypeMoonAddon;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client-only visual event. The server has already decided and delayed the death. */
public record DeathFadePayload(
    int entityId,
    double x,
    double y,
    double z,
    float width,
    float height,
    byte palette
) implements CustomPacketPayload {
    public static final Type<DeathFadePayload> TYPE = new Type<>(TypeMoonAddon.id("shadow_death_fade"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DeathFadePayload> STREAM_CODEC = StreamCodec.of(
        (buffer, payload) -> {
            buffer.writeVarInt(payload.entityId);
            buffer.writeDouble(payload.x);
            buffer.writeDouble(payload.y);
            buffer.writeDouble(payload.z);
            buffer.writeFloat(payload.width);
            buffer.writeFloat(payload.height);
            buffer.writeByte(payload.palette);
        },
        buffer -> new DeathFadePayload(
            buffer.readVarInt(),
            buffer.readDouble(),
            buffer.readDouble(),
            buffer.readDouble(),
            buffer.readFloat(),
            buffer.readFloat(),
            buffer.readByte()
        )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
