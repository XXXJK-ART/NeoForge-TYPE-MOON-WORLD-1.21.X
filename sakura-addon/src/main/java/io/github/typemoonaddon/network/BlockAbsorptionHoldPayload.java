package io.github.typemoonaddon.network;

import io.github.typemoonaddon.TypeMoonAddon;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** A client key-state heartbeat; it never contains a target or an outcome. */
public record BlockAbsorptionHoldPayload(boolean held) implements CustomPacketPayload {
    public static final Type<BlockAbsorptionHoldPayload> TYPE = new Type<>(TypeMoonAddon.id("block_absorption_hold"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BlockAbsorptionHoldPayload> STREAM_CODEC = StreamCodec.of(
        (buffer, payload) -> buffer.writeBoolean(payload.held),
        buffer -> new BlockAbsorptionHoldPayload(buffer.readBoolean())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
