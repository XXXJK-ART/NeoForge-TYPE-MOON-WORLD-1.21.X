package com.example.typemoonaddon.network;

import com.example.typemoonaddon.TypeMoonAddon;
import com.example.typemoonaddon.magic.SakuraShadowTransferService;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record SelectShadowTransferPayload(UUID familiarId) implements CustomPacketPayload {
    public static final Type<SelectShadowTransferPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(TypeMoonAddon.MOD_ID, "select_shadow_transfer")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, SelectShadowTransferPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> buffer.writeUUID(payload.familiarId),
            buffer -> new SelectShadowTransferPayload(buffer.readUUID())
    );

    @Override
    public @NotNull Type<SelectShadowTransferPayload> type() {
        return TYPE;
    }

    public static void handle(SelectShadowTransferPayload message, IPayloadContext context) {
        if (context.flow() != PacketFlow.SERVERBOUND || !(context.player() instanceof ServerPlayer player)) {
            return;
        }
        context.enqueueWork(() -> SakuraShadowTransferService.begin(player, message.familiarId));
    }
}
