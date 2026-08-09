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

public record StorageModeSwitchPayload() implements CustomPacketPayload {
    public static final Type<StorageModeSwitchPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(TypeMoonAddon.MOD_ID, "storage_mode_switch")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, StorageModeSwitchPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, message) -> {
            },
            buffer -> new StorageModeSwitchPayload()
    );

    @Override
    public @NotNull Type<StorageModeSwitchPayload> type() {
        return TYPE;
    }

    public static void handle(StorageModeSwitchPayload message, IPayloadContext context) {
        if (context.flow() != PacketFlow.SERVERBOUND || !(context.player() instanceof ServerPlayer player)) {
            return;
        }
        context.enqueueWork(() -> StorageService.switchMode(player));
    }
}
