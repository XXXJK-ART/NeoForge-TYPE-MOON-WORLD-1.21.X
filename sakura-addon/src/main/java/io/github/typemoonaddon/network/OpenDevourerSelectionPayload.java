package io.github.typemoonaddon.network;

import io.github.typemoonaddon.TypeMoonAddon;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record OpenDevourerSelectionPayload(List<Entry> entries) implements CustomPacketPayload {
    private static final int MAX_ENTRIES = 256;

    public static final Type<OpenDevourerSelectionPayload> TYPE = new Type<>(
        TypeMoonAddon.id("open_devourer_selection")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenDevourerSelectionPayload> STREAM_CODEC = StreamCodec.of(
        (buffer, payload) -> {
            buffer.writeVarInt(payload.entries.size());
            for (Entry entry : payload.entries) {
                buffer.writeUUID(entry.rosterId);
                buffer.writeUtf(entry.displayName, 128);
                buffer.writeBoolean(entry.selected);
                buffer.writeBoolean(entry.active);
                buffer.writeBoolean(entry.player);
            }
        },
        buffer -> {
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
    );

    public OpenDevourerSelectionPayload {
        entries = List.copyOf(entries);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record Entry(UUID rosterId, String displayName, boolean selected, boolean active, boolean player) {
    }
}
