package com.captain.servant.network.payload;

import com.captain.servant.spiritual.SpiritualizationService;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import static com.captain.servant.TypeMoonServant.MOD_ID;

public record SpiritualizationTogglePayload() implements CustomPacketPayload {
    public static final Type<SpiritualizationTogglePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "spiritualization_toggle")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, SpiritualizationTogglePayload> STREAM_CODEC =
            StreamCodec.of((ignored, payload) -> { }, ignored -> new SpiritualizationTogglePayload());

    @Override
    public Type<SpiritualizationTogglePayload> type() {
        return TYPE;
    }

    public static void handleData(SpiritualizationTogglePayload payload, IPayloadContext context) {
        if (context.flow() != PacketFlow.SERVERBOUND) {
            return;
        }
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                SpiritualizationService.toggle(player);
            }
        });
    }
}
