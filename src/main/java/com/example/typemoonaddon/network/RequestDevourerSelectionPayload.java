package com.example.typemoonaddon.network;

import com.example.typemoonaddon.TypeMoonAddon;
import com.example.typemoonaddon.magic.SakuraPollutionService;
import com.example.typemoonaddon.magic.SakuraTypeMoonIntegration;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record RequestDevourerSelectionPayload() implements CustomPacketPayload {
    public static final RequestDevourerSelectionPayload INSTANCE = new RequestDevourerSelectionPayload();
    public static final Type<RequestDevourerSelectionPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(TypeMoonAddon.MOD_ID, "request_devourer_selection")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, RequestDevourerSelectionPayload> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public @NotNull Type<RequestDevourerSelectionPayload> type() {
        return TYPE;
    }

    public static void handle(RequestDevourerSelectionPayload message, IPayloadContext context) {
        if (context.flow() != PacketFlow.SERVERBOUND || !(context.player() instanceof ServerPlayer player)) {
            return;
        }
        context.enqueueWork(() -> {
            if (player.isAlive() && !player.isSpectator() && SakuraTypeMoonIntegration.isHeroicSpiritDevourerLearned(player)) {
                SakuraPollutionService.openDevourerSelection(player);
            }
        });
    }
}
