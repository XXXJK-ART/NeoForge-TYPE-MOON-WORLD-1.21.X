package com.captain.servant.network.payload;

import com.captain.servant.voice.VoiceCommunicationService;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import static com.captain.servant.TypeMoonServant.MOD_ID;

public record TelepathyTogglePayload() implements CustomPacketPayload {
    public static final Type<TelepathyTogglePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "telepathy_toggle")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, TelepathyTogglePayload> STREAM_CODEC =
            StreamCodec.of((ignored, payload) -> { }, ignored -> new TelepathyTogglePayload());

    @Override
    public Type<TelepathyTogglePayload> type() {
        return TYPE;
    }

    public static void handleData(TelepathyTogglePayload payload, IPayloadContext context) {
        if (context.flow() != PacketFlow.SERVERBOUND) {
            return;
        }
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                VoiceCommunicationService.toggleTelepathy(player);
            }
        });
    }
}
