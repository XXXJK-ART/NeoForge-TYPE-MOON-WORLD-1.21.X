package io.github.typemoonaddon.network;

import io.github.typemoonaddon.TypeMoonAddon;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record SetBlackMudSummonModePayload(int modeId) implements CustomPacketPayload {
    public static final Type<SetBlackMudSummonModePayload> TYPE = new Type<>(
        TypeMoonAddon.id("set_black_mud_summon_mode")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, SetBlackMudSummonModePayload> STREAM_CODEC = StreamCodec.of(
        (buffer, payload) -> buffer.writeVarInt(payload.modeId),
        buffer -> new SetBlackMudSummonModePayload(buffer.readVarInt())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
