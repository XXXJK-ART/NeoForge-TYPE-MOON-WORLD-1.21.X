package io.github.typemoonaddon.network;

import io.github.typemoonaddon.TypeMoonAddon;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record MagicOutputShockwavePayload(
    double x,
    double y,
    double z,
    float maximumRadius,
    int durationTicks
) implements CustomPacketPayload {
    public static final Type<MagicOutputShockwavePayload> TYPE = new Type<>(
        TypeMoonAddon.id("magic_output_shockwave")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, MagicOutputShockwavePayload> STREAM_CODEC = StreamCodec.of(
        (buffer, payload) -> {
            buffer.writeDouble(payload.x());
            buffer.writeDouble(payload.y());
            buffer.writeDouble(payload.z());
            buffer.writeFloat(payload.maximumRadius());
            buffer.writeVarInt(payload.durationTicks());
        },
        buffer -> new MagicOutputShockwavePayload(
            buffer.readDouble(),
            buffer.readDouble(),
            buffer.readDouble(),
            buffer.readFloat(),
            buffer.readVarInt()
        )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
