package io.github.typemoonaddon.network;

import io.github.typemoonaddon.TypeMoonAddon;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record SetDevourerSelectionPayload(List<UUID> rosterIds) implements CustomPacketPayload {
    private static final int MAX_SELECTIONS = 256;

    public static final Type<SetDevourerSelectionPayload> TYPE = new Type<>(
        TypeMoonAddon.id("set_devourer_selection")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, SetDevourerSelectionPayload> STREAM_CODEC = StreamCodec.of(
        (buffer, payload) -> {
            buffer.writeVarInt(payload.rosterIds.size());
            for (UUID rosterId : payload.rosterIds) {
                buffer.writeUUID(rosterId);
            }
        },
        buffer -> {
            int count = buffer.readVarInt();
            if (count < 0 || count > MAX_SELECTIONS) {
                throw new IllegalArgumentException("Invalid devourer selection size: " + count);
            }
            List<UUID> rosterIds = new ArrayList<>(count);
            for (int index = 0; index < count; index++) {
                rosterIds.add(buffer.readUUID());
            }
            return new SetDevourerSelectionPayload(rosterIds);
        }
    );

    public SetDevourerSelectionPayload {
        rosterIds = List.copyOf(rosterIds);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
