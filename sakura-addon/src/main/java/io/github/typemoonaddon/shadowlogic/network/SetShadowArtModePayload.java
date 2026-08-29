package io.github.typemoonaddon.shadowlogic.network;

import io.github.typemoonaddon.TypeMoonAddon;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record SetShadowArtModePayload(int modeId) implements CustomPacketPayload {
    public static final Type<SetShadowArtModePayload> TYPE = new Type<>(TypeMoonAddon.id("set_shadow_art_mode"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SetShadowArtModePayload> STREAM_CODEC = StreamCodec.of(
        (buffer, payload) -> buffer.writeVarInt(payload.modeId), buffer -> new SetShadowArtModePayload(buffer.readVarInt())
    );
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
