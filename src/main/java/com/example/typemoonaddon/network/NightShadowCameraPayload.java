package com.example.typemoonaddon.network;

import com.example.typemoonaddon.TypeMoonAddon;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Switches only the local render camera, leaving the server player asleep in bed. */
public record NightShadowCameraPayload(int shadowEntityId, boolean active) implements CustomPacketPayload {
    public static final Type<NightShadowCameraPayload> TYPE = new Type<>(TypeMoonAddon.id("night_shadow_camera"));
    public static final StreamCodec<RegistryFriendlyByteBuf, NightShadowCameraPayload> STREAM_CODEC = StreamCodec.of(
        (buffer, payload) -> {
            buffer.writeVarInt(payload.shadowEntityId());
            buffer.writeBoolean(payload.active());
        },
        buffer -> new NightShadowCameraPayload(buffer.readVarInt(), buffer.readBoolean())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
