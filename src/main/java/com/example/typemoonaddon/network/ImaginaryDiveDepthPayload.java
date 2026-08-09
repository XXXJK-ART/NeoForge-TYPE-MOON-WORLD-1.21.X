package com.example.typemoonaddon.network;

import com.example.typemoonaddon.TypeMoonAddon;
import com.example.typemoonaddon.magic.ImaginaryDiveMagic;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

/** Serverbound depth value from the imaginary-dive editor. */
public record ImaginaryDiveDepthPayload(double depth) implements CustomPacketPayload {
    public static final Type<ImaginaryDiveDepthPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(TypeMoonAddon.MOD_ID, "imaginary_dive_depth"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ImaginaryDiveDepthPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buffer, message) -> buffer.writeDouble(message.depth),
                    buffer -> new ImaginaryDiveDepthPayload(buffer.readDouble()));

    @Override
    public @NotNull Type<ImaginaryDiveDepthPayload> type() {
        return TYPE;
    }

    public static void handle(ImaginaryDiveDepthPayload message, IPayloadContext context) {
        if (context.flow() != PacketFlow.SERVERBOUND
                || !(context.player() instanceof ServerPlayer player)) {
            return;
        }
        context.enqueueWork(() -> ImaginaryDiveMagic.submitDepth(player, message.depth()));
    }
}
