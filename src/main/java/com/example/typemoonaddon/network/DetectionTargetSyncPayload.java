package com.example.typemoonaddon.network;

import com.example.typemoonaddon.TypeMoonAddon;
import com.example.typemoonaddon.client.DetectionClientState;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

/** Compact, private full replacement of the targets authorized for one detecting client. */
public record DetectionTargetSyncPayload(boolean active, List<TargetMarker> targets)
        implements CustomPacketPayload {
    public static final int MAX_TARGETS = 128;
    public static final byte CATEGORY_NEUTRAL = 0;
    public static final byte CATEGORY_HOSTILE = 1;
    public static final byte CATEGORY_FRIENDLY = 2;
    public static final byte FLAG_INVISIBLE = 4;

    public static final Type<DetectionTargetSyncPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(TypeMoonAddon.MOD_ID, "detection_targets")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, DetectionTargetSyncPayload> STREAM_CODEC =
            StreamCodec.of(DetectionTargetSyncPayload::encode, DetectionTargetSyncPayload::decode);

    public DetectionTargetSyncPayload {
        targets = targets == null
                ? List.of()
                : List.copyOf(targets.subList(0, Math.min(MAX_TARGETS, targets.size())));
    }

    @Override
    public @NotNull Type<DetectionTargetSyncPayload> type() {
        return TYPE;
    }

    public static void handle(DetectionTargetSyncPayload message, IPayloadContext context) {
        if (context.flow() != PacketFlow.CLIENTBOUND) {
            return;
        }
        context.enqueueWork(() -> DetectionClientState.applyTargets(message.active, message.targets));
    }

    private static void encode(RegistryFriendlyByteBuf buffer, DetectionTargetSyncPayload message) {
        buffer.writeBoolean(message.active);
        buffer.writeVarInt(message.targets.size());
        for (TargetMarker marker : message.targets) {
            buffer.writeVarInt(Math.max(0, marker.entityId));
            buffer.writeByte(marker.category & 0x07);
        }
    }

    private static DetectionTargetSyncPayload decode(RegistryFriendlyByteBuf buffer) {
        boolean active = buffer.readBoolean();
        int size = buffer.readVarInt();
        if (size < 0 || size > MAX_TARGETS) {
            throw new IllegalArgumentException("Invalid detection target count: " + size);
        }
        List<TargetMarker> targets = new ArrayList<>(size);
        for (int index = 0; index < size; index++) {
            targets.add(new TargetMarker(buffer.readVarInt(), buffer.readByte()));
        }
        return new DetectionTargetSyncPayload(active, targets);
    }

    public record TargetMarker(int entityId, byte category) {
    }
}
