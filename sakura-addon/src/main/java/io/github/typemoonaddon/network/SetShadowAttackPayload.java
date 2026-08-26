package io.github.typemoonaddon.network;

import io.github.typemoonaddon.TypeMoonAddon;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Idempotent client request for the familiar active-attack toggle. */
public record SetShadowAttackPayload(boolean enabled) implements CustomPacketPayload {
    public static final Type<SetShadowAttackPayload> TYPE = new Type<>(TypeMoonAddon.id("set_shadow_attack"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SetShadowAttackPayload> STREAM_CODEC = StreamCodec.of(
        (buffer, payload) -> buffer.writeBoolean(payload.enabled),
        buffer -> new SetShadowAttackPayload(buffer.readBoolean())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
