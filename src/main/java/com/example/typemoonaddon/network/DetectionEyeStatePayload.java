package com.example.typemoonaddon.network;

import com.example.typemoonaddon.TypeMoonAddon;
import com.example.typemoonaddon.client.DetectionClientState;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

/** Public visual-only heartbeat for a caster's ocular detection ring. */
public record DetectionEyeStatePayload(int entityId, boolean active, int ttlTicks)
        implements CustomPacketPayload {
    private static final int MAX_TTL_TICKS = 100;

    public static final Type<DetectionEyeStatePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(TypeMoonAddon.MOD_ID, "detection_eye_state")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, DetectionEyeStatePayload> STREAM_CODEC = StreamCodec.of(
            (buffer, message) -> {
                buffer.writeVarInt(Math.max(0, message.entityId));
                buffer.writeBoolean(message.active);
                buffer.writeVarInt(Mth.clamp(message.ttlTicks, 0, MAX_TTL_TICKS));
            },
            buffer -> new DetectionEyeStatePayload(
                    buffer.readVarInt(),
                    buffer.readBoolean(),
                    Mth.clamp(buffer.readVarInt(), 0, MAX_TTL_TICKS))
    );

    @Override
    public @NotNull Type<DetectionEyeStatePayload> type() {
        return TYPE;
    }

    public static void handle(DetectionEyeStatePayload message, IPayloadContext context) {
        if (context.flow() != PacketFlow.CLIENTBOUND) {
            return;
        }
        context.enqueueWork(() -> DetectionClientState.applyEyeState(
                message.entityId, message.active, message.ttlTicks));
    }
}
