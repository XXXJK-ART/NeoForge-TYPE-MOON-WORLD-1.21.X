package com.example.typemoonaddon.network;

import com.example.typemoonaddon.TypeMoonAddon;
import com.example.typemoonaddon.client.HeroicSpiritDevourerScreen;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record OpenDevourerSelectionPayload(List<Entry> entries) implements CustomPacketPayload {
    private static final int MAX_ENTRIES = 256;

    public static final Type<OpenDevourerSelectionPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(TypeMoonAddon.MOD_ID, "open_devourer_selection")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenDevourerSelectionPayload> STREAM_CODEC = StreamCodec.of(
            OpenDevourerSelectionPayload::encode,
            OpenDevourerSelectionPayload::decode
    );

    public OpenDevourerSelectionPayload {
        entries = List.copyOf(entries);
    }

    @Override
    public @NotNull Type<OpenDevourerSelectionPayload> type() {
        return TYPE;
    }

    public static void handle(OpenDevourerSelectionPayload message, IPayloadContext context) {
        if (context.flow() != PacketFlow.CLIENTBOUND) {
            return;
        }
        context.enqueueWork(() -> Minecraft.getInstance().setScreen(new HeroicSpiritDevourerScreen(message)));
    }

    private static void encode(RegistryFriendlyByteBuf buffer, OpenDevourerSelectionPayload payload) {
        buffer.writeVarInt(payload.entries.size());
        for (Entry entry : payload.entries) {
            buffer.writeUUID(entry.rosterId);
            buffer.writeUtf(entry.displayName, 128);
            buffer.writeBoolean(entry.selected);
            buffer.writeBoolean(entry.active);
            buffer.writeBoolean(entry.player);
        }
    }

    private static OpenDevourerSelectionPayload decode(RegistryFriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        if (count < 0 || count > MAX_ENTRIES) {
            throw new IllegalArgumentException("Invalid devourer roster size: " + count);
        }
        List<Entry> entries = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            entries.add(new Entry(
                    buffer.readUUID(),
                    buffer.readUtf(128),
                    buffer.readBoolean(),
                    buffer.readBoolean(),
                    buffer.readBoolean()
            ));
        }
        return new OpenDevourerSelectionPayload(entries);
    }

    public record Entry(UUID rosterId, String displayName, boolean selected, boolean active, boolean player) {
    }
}
