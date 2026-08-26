package io.github.typemoonaddon.network;

import io.github.typemoonaddon.TypeMoonAddon;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record VoidAbsorptionLinkPayload(int sourceEntityId, int targetEntityId, int durationTicks, byte palette)
    implements CustomPacketPayload {
    public static final Type<VoidAbsorptionLinkPayload> TYPE = new Type<>(TypeMoonAddon.id("void_absorption_link"));
    public static final StreamCodec<RegistryFriendlyByteBuf, VoidAbsorptionLinkPayload> STREAM_CODEC = StreamCodec.of(
        (buffer, payload) -> {
            buffer.writeVarInt(payload.sourceEntityId());
            buffer.writeVarInt(payload.targetEntityId());
            buffer.writeVarInt(payload.durationTicks());
            buffer.writeByte(payload.palette());
        },
        buffer -> new VoidAbsorptionLinkPayload(
            buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readByte()
        )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
