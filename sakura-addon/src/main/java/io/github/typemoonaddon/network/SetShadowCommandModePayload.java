package io.github.typemoonaddon.network;

import io.github.typemoonaddon.TypeMoonAddon;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client selection intent; the server verifies both the mode and learned magic. */
public record SetShadowCommandModePayload(int modeId) implements CustomPacketPayload {
    public static final Type<SetShadowCommandModePayload> TYPE = new Type<>(TypeMoonAddon.id("set_shadow_command_mode"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SetShadowCommandModePayload> STREAM_CODEC = StreamCodec.of(
        (buffer, payload) -> buffer.writeVarInt(payload.modeId),
        buffer -> new SetShadowCommandModePayload(buffer.readVarInt())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
