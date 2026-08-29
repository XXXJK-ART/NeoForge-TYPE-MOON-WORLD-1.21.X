package io.github.typemoonaddon.network;

import io.github.typemoonaddon.TypeMoonAddon;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** The server resolves this UUID again and never trusts coordinates from the client. */
public record SelectShadowTransferPayload(UUID familiarId) implements CustomPacketPayload {
    public static final Type<SelectShadowTransferPayload> TYPE = new Type<>(TypeMoonAddon.id("select_shadow_transfer"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SelectShadowTransferPayload> STREAM_CODEC = StreamCodec.of(
        (buffer, payload) -> buffer.writeUUID(payload.familiarId),
        buffer -> new SelectShadowTransferPayload(buffer.readUUID())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
