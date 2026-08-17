package net.xxxjk.TYPE_MOON_WORLD.chain.network;

import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.chain.service.ChainControlService;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ChainInputPayload(boolean held) implements CustomPacketPayload {
    public static final Type<ChainInputPayload> TYPE = new Type<>(TYPE_MOON_WORLD.id("chain_input"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ChainInputPayload> STREAM_CODEC = StreamCodec.of(
        (buffer, payload) -> buffer.writeBoolean(payload.held),
        buffer -> new ChainInputPayload(buffer.readBoolean())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleData(ChainInputPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player && player.isAlive() && !player.isSpectator()) {
                ChainControlService.handlePlayerInput(player, payload.held());
            }
        });
    }
}


