package com.example.typemoonaddon.shadowlogic.network;

import com.example.typemoonaddon.TypeMoonAddon;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ShadowBindingEffectPayload(int sourceEntityId, int targetEntityId, int durationTicks, byte palette)
    implements CustomPacketPayload {
    public static final Type<ShadowBindingEffectPayload> TYPE = new Type<>(TypeMoonAddon.id("shadow_binding_effect"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ShadowBindingEffectPayload> STREAM_CODEC = StreamCodec.of(
        (buffer, payload) -> {
            buffer.writeVarInt(payload.sourceEntityId());
            buffer.writeVarInt(payload.targetEntityId());
            buffer.writeVarInt(payload.durationTicks());
            buffer.writeByte(payload.palette());
        },
        buffer -> new ShadowBindingEffectPayload(
            buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readByte()
        )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
