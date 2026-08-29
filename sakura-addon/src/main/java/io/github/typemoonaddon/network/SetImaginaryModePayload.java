package io.github.typemoonaddon.network;

import io.github.typemoonaddon.TypeMoonAddon;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client intent only; the server validates the sender and mode before applying it. */
public record SetImaginaryModePayload(int modeId) implements CustomPacketPayload {
    public static final Type<SetImaginaryModePayload> TYPE = new Type<>(TypeMoonAddon.id("set_imaginary_mode"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SetImaginaryModePayload> STREAM_CODEC = StreamCodec.of(
        (buffer, payload) -> buffer.writeVarInt(payload.modeId),
        buffer -> new SetImaginaryModePayload(buffer.readVarInt())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
