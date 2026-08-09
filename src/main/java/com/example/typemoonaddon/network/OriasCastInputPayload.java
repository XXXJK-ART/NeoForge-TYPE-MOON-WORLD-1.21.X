package com.example.typemoonaddon.network;

import com.example.typemoonaddon.TypeMoonAddon;
import com.example.typemoonaddon.orias.OriasService;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record OriasCastInputPayload(byte action) implements CustomPacketPayload {
    public static final byte PRESS = 0;
    public static final byte RELEASE = 1;
    public static final byte CANCEL = 2;

    public static final Type<OriasCastInputPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(TypeMoonAddon.MOD_ID, "orias_cast_input")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, OriasCastInputPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, message) -> buffer.writeByte(message.action),
            buffer -> new OriasCastInputPayload(buffer.readByte())
    );

    @Override
    public @NotNull Type<OriasCastInputPayload> type() {
        return TYPE;
    }

    public static void handle(OriasCastInputPayload message, IPayloadContext context) {
        if (context.flow() != PacketFlow.SERVERBOUND
                || !(context.player() instanceof ServerPlayer player)
                || message.action < PRESS
                || message.action > CANCEL) {
            return;
        }
        context.enqueueWork(() -> OriasService.handleCastInput(player, message.action));
    }
}
