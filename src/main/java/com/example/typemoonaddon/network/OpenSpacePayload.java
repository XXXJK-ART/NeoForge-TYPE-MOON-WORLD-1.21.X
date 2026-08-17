package com.example.typemoonaddon.network;

import com.example.typemoonaddon.TypeMoonAddon;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record OpenSpacePayload() implements CustomPacketPayload {
    public static final OpenSpacePayload INSTANCE = new OpenSpacePayload();
    public static final Type<OpenSpacePayload> TYPE = new Type<>(TypeMoonAddon.id("open_imaginary_space"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenSpacePayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
