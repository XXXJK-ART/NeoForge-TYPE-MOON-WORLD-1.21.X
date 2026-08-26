package io.github.typemoonaddon.network;

import io.github.typemoonaddon.TypeMoonAddon;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record RequestDevourerSelectionPayload() implements CustomPacketPayload {
    public static final RequestDevourerSelectionPayload INSTANCE = new RequestDevourerSelectionPayload();
    public static final Type<RequestDevourerSelectionPayload> TYPE = new Type<>(
        TypeMoonAddon.id("request_devourer_selection")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, RequestDevourerSelectionPayload> STREAM_CODEC =
        StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
