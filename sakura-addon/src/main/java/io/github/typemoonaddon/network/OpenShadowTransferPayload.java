package io.github.typemoonaddon.network;

import io.github.typemoonaddon.TypeMoonAddon;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record OpenShadowTransferPayload(int originX, int originZ, List<Target> targets)
    implements CustomPacketPayload {
    private static final int MAX_TARGETS = 256;

    public static final Type<OpenShadowTransferPayload> TYPE = new Type<>(TypeMoonAddon.id("open_shadow_transfer"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenShadowTransferPayload> STREAM_CODEC = StreamCodec.of(
        (buffer, payload) -> {
            buffer.writeInt(payload.originX);
            buffer.writeInt(payload.originZ);
            buffer.writeVarInt(payload.targets.size());
            for (Target target : payload.targets) {
                buffer.writeUUID(target.familiarId);
                buffer.writeInt(target.x);
                buffer.writeInt(target.y);
                buffer.writeInt(target.z);
                buffer.writeVarInt(target.distance);
                buffer.writeVarInt(target.manaCost);
            }
        },
        buffer -> {
            int originX = buffer.readInt();
            int originZ = buffer.readInt();
            int count = buffer.readVarInt();
            if (count < 0 || count > MAX_TARGETS) {
                throw new IllegalArgumentException("Invalid Shadow Transfer target count: " + count);
            }
            List<Target> targets = new ArrayList<>(count);
            for (int index = 0; index < count; index++) {
                targets.add(new Target(
                    buffer.readUUID(),
                    buffer.readInt(),
                    buffer.readInt(),
                    buffer.readInt(),
                    buffer.readVarInt(),
                    buffer.readVarInt()
                ));
            }
            return new OpenShadowTransferPayload(originX, originZ, List.copyOf(targets));
        }
    );

    public OpenShadowTransferPayload {
        targets = List.copyOf(targets);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record Target(UUID familiarId, int x, int y, int z, int distance, int manaCost) {
    }
}
