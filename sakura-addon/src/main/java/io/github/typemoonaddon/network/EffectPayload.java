package io.github.typemoonaddon.network;

import io.github.typemoonaddon.TypeMoonAddon;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record EffectPayload(int entityId, byte effect, int durationTicks, byte palette)
    implements CustomPacketPayload {
    public static final byte ITEM_ABSORPTION = 0;
    public static final byte HAND_ABSORPTION = 2;

    public static final Type<EffectPayload> TYPE = new Type<>(TypeMoonAddon.id("absorption_effect"));
    public static final StreamCodec<RegistryFriendlyByteBuf, EffectPayload> STREAM_CODEC = StreamCodec.of(
        (buffer, payload) -> {
            buffer.writeVarInt(payload.entityId);
            buffer.writeByte(payload.effect);
            buffer.writeVarInt(payload.durationTicks);
            buffer.writeByte(payload.palette);
        },
        buffer -> new EffectPayload(buffer.readVarInt(), buffer.readByte(), buffer.readVarInt(), buffer.readByte())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
