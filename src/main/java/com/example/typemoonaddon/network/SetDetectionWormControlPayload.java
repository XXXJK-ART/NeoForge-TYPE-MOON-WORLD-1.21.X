package com.example.typemoonaddon.network;

import com.example.typemoonaddon.TypeMoonAddon;
import com.example.typemoonaddon.magic.WormMagicIntegration;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record SetDetectionWormControlPayload(int entityId, int mode) implements CustomPacketPayload {
    public static final Type<SetDetectionWormControlPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(TypeMoonAddon.MOD_ID, "set_detection_worm_control")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, SetDetectionWormControlPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeVarInt(Math.max(0, payload.entityId));
                buffer.writeVarInt(Math.max(0, payload.mode));
            },
            buffer -> new SetDetectionWormControlPayload(buffer.readVarInt(), buffer.readVarInt())
    );

    @Override
    public @NotNull Type<SetDetectionWormControlPayload> type() {
        return TYPE;
    }

    public static void handle(SetDetectionWormControlPayload message, IPayloadContext context) {
        if (context.flow() != PacketFlow.SERVERBOUND || !(context.player() instanceof ServerPlayer player)) {
            return;
        }
        context.enqueueWork(() -> WormMagicIntegration.applyDetectionControl(player, message.entityId, message.mode));
    }
}
