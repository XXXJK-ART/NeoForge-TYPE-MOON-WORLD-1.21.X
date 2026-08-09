package com.example.typemoonaddon.network;

import com.example.typemoonaddon.TypeMoonAddon;
import com.example.typemoonaddon.storage.StorageService;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record StorageCastInputPayload(boolean pressed) implements CustomPacketPayload {
    public static final Type<StorageCastInputPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(TypeMoonAddon.MOD_ID, "storage_cast_input")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, StorageCastInputPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, message) -> buffer.writeBoolean(message.pressed),
            buffer -> new StorageCastInputPayload(buffer.readBoolean())
    );

    @Override
    public @NotNull Type<StorageCastInputPayload> type() {
        return TYPE;
    }

    public static void handle(StorageCastInputPayload message, IPayloadContext context) {
        if (context.flow() != PacketFlow.SERVERBOUND || !(context.player() instanceof ServerPlayer player)) {
            return;
        }
        context.enqueueWork(() -> StorageService.handleCastInput(player, message.pressed));
    }
}
