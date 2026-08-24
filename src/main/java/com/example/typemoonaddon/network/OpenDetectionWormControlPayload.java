package com.example.typemoonaddon.network;

import com.example.typemoonaddon.TypeMoonAddon;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record OpenDetectionWormControlPayload(int entityId, UUID ownerId, String displayName, boolean controlled,
                                              boolean sharingVision, int guPower, List<String> modeHints)
        implements CustomPacketPayload {
    private static final int MAX_HINTS = 8;

    public static final Type<OpenDetectionWormControlPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(TypeMoonAddon.MOD_ID, "open_detection_worm_control")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenDetectionWormControlPayload> STREAM_CODEC = StreamCodec.of(
            OpenDetectionWormControlPayload::encode,
            OpenDetectionWormControlPayload::decode
    );

    public OpenDetectionWormControlPayload {
        modeHints = List.copyOf(modeHints);
    }

    @Override
    public @NotNull Type<OpenDetectionWormControlPayload> type() {
        return TYPE;
    }

    public static void handle(OpenDetectionWormControlPayload message, IPayloadContext context) {
        if (context.flow() != PacketFlow.CLIENTBOUND) {
            return;
        }
        context.enqueueWork(() -> ClientScreenAccess.openDetectionWormControl(message));
    }

    private static void encode(RegistryFriendlyByteBuf buffer, OpenDetectionWormControlPayload payload) {
        buffer.writeVarInt(Math.max(0, payload.entityId));
        buffer.writeBoolean(payload.ownerId != null);
        if (payload.ownerId != null) {
            buffer.writeUUID(payload.ownerId);
        }
        buffer.writeUtf(payload.displayName == null ? "" : payload.displayName, 128);
        buffer.writeBoolean(payload.controlled);
        buffer.writeBoolean(payload.sharingVision);
        buffer.writeVarInt(Math.max(0, payload.guPower));
        buffer.writeVarInt(Math.min(MAX_HINTS, payload.modeHints.size()));
        for (int index = 0; index < Math.min(MAX_HINTS, payload.modeHints.size()); index++) {
            buffer.writeUtf(payload.modeHints.get(index), 48);
        }
    }

    private static OpenDetectionWormControlPayload decode(RegistryFriendlyByteBuf buffer) {
        int entityId = buffer.readVarInt();
        UUID ownerId = buffer.readBoolean() ? buffer.readUUID() : null;
        String displayName = buffer.readUtf(128);
        boolean controlled = buffer.readBoolean();
        boolean sharingVision = buffer.readBoolean();
        int guPower = buffer.readVarInt();
        int count = Math.max(0, Math.min(MAX_HINTS, buffer.readVarInt()));
        List<String> hints = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            hints.add(buffer.readUtf(48));
        }
        return new OpenDetectionWormControlPayload(entityId, ownerId, displayName, controlled, sharingVision, guPower, hints);
    }
}
