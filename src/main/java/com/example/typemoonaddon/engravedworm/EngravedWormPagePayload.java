package com.example.typemoonaddon.engravedworm;

import com.example.typemoonaddon.TypeMoonAddon;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record EngravedWormPagePayload(int page) implements CustomPacketPayload {
    public static final Type<EngravedWormPagePayload> TYPE =
            new Type<>(TypeMoonAddon.id("engraved_worm_page"));
    public static final StreamCodec<RegistryFriendlyByteBuf, EngravedWormPagePayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> buffer.writeVarInt(payload.page()),
            buffer -> new EngravedWormPagePayload(buffer.readVarInt())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(EngravedWormPagePayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            context.enqueueWork(() -> {
                if (player.containerMenu instanceof EngravedWormMenu menu) {
                    menu.setPage(payload.page());
                }
            });
        }
    }
}
