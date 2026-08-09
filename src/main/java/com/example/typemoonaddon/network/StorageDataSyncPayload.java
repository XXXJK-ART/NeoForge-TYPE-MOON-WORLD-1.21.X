package com.example.typemoonaddon.network;

import com.example.typemoonaddon.TypeMoonAddon;
import com.example.typemoonaddon.storage.StorageAttachments;
import com.example.typemoonaddon.storage.StorageData;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record StorageDataSyncPayload(int mode, double storedMana) implements CustomPacketPayload {
    public static final Type<StorageDataSyncPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(TypeMoonAddon.MOD_ID, "storage_data_sync")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, StorageDataSyncPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, message) -> {
                buffer.writeInt(message.mode);
                buffer.writeDouble(message.storedMana);
            },
            buffer -> new StorageDataSyncPayload(buffer.readInt(), buffer.readDouble())
    );

    public StorageDataSyncPayload(StorageData data) {
        this(data.getMode(), data.getStoredMana());
    }

    @Override
    public @NotNull Type<StorageDataSyncPayload> type() {
        return TYPE;
    }

    public static void handle(StorageDataSyncPayload message, IPayloadContext context) {
        if (context.flow() != PacketFlow.CLIENTBOUND) {
            return;
        }
        context.enqueueWork(() -> {
            StorageData data = context.player().getData(StorageAttachments.PLAYER_STORAGE);
            data.setMode(message.mode);
            data.setStoredMana(message.storedMana);
        });
    }
}
