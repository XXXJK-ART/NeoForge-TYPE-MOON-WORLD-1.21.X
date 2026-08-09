package com.example.typemoonaddon.network;

import com.example.typemoonaddon.TypeMoonAddon;
import com.example.typemoonaddon.imaginary_space.ImaginarySpaceAttachments;
import com.example.typemoonaddon.imaginary_space.ImaginarySpaceData;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record ImaginarySpaceStatePayload(
        int mode,
        boolean active,
        double existence,
        double depth,
        double timeOffset,
        int invulnerabilityTicks
) implements CustomPacketPayload {
    public static final Type<ImaginarySpaceStatePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(TypeMoonAddon.MOD_ID, "imaginary_space_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ImaginarySpaceStatePayload> STREAM_CODEC =
            StreamCodec.of(
                    (buffer, message) -> {
                        buffer.writeVarInt(message.mode);
                        buffer.writeBoolean(message.active);
                        buffer.writeDouble(message.existence);
                        buffer.writeDouble(message.depth);
                        buffer.writeDouble(message.timeOffset);
                        buffer.writeVarInt(Math.max(0, message.invulnerabilityTicks));
                    },
                    buffer -> new ImaginarySpaceStatePayload(
                            buffer.readVarInt(),
                            buffer.readBoolean(),
                            buffer.readDouble(),
                            buffer.readDouble(),
                            buffer.readDouble(),
                            Math.max(0, buffer.readVarInt())));

    public ImaginarySpaceStatePayload(ImaginarySpaceData data) {
        this(data.mode(), data.active(), data.existence(), data.depth(),
                data.timeOffset(), data.invulnerabilityTicks());
    }

    @Override
    public @NotNull Type<ImaginarySpaceStatePayload> type() {
        return TYPE;
    }

    public static void handle(ImaginarySpaceStatePayload message, IPayloadContext context) {
        if (context.flow() != PacketFlow.CLIENTBOUND) {
            return;
        }
        context.enqueueWork(() -> context.player()
                .getData(ImaginarySpaceAttachments.PLAYER_STATE)
                .applyClientState(
                        message.mode,
                        message.active,
                        message.existence,
                        message.depth,
                        message.timeOffset,
                        message.invulnerabilityTicks));
    }
}
