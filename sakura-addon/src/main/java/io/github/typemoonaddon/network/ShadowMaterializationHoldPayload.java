package io.github.typemoonaddon.network;

import io.github.typemoonaddon.TypeMoonAddon;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** A key-state heartbeat. Summoning still starts only through the registered magic executor. */
public record ShadowMaterializationHoldPayload(boolean held) implements CustomPacketPayload {
    public static final Type<ShadowMaterializationHoldPayload> TYPE = new Type<>(
        TypeMoonAddon.id("shadow_materialization_hold")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, ShadowMaterializationHoldPayload> STREAM_CODEC = StreamCodec.of(
        (buffer, payload) -> buffer.writeBoolean(payload.held),
        buffer -> new ShadowMaterializationHoldPayload(buffer.readBoolean())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
