package com.example.typemoonaddon.network;

import com.example.typemoonaddon.TypeMoonAddon;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record OpenBoundaryImmunityPayload(int entityId, String displayName, List<String> selectedBoundaryIds)
        implements CustomPacketPayload {
    private static final int MAX_SELECTED = 32;

    public static final Type<OpenBoundaryImmunityPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(TypeMoonAddon.MOD_ID, "open_boundary_immunity")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenBoundaryImmunityPayload> STREAM_CODEC = StreamCodec.of(
            OpenBoundaryImmunityPayload::encode,
            OpenBoundaryImmunityPayload::decode
    );

    public OpenBoundaryImmunityPayload {
        selectedBoundaryIds = List.copyOf(selectedBoundaryIds);
    }

    @Override
    public @NotNull Type<OpenBoundaryImmunityPayload> type() {
        return TYPE;
    }

    public static void handle(OpenBoundaryImmunityPayload message, IPayloadContext context) {
        if (context.flow() != PacketFlow.CLIENTBOUND) {
            return;
        }
        context.enqueueWork(() -> ClientScreenAccess.openBoundaryImmunity(message));
    }

    private static void encode(RegistryFriendlyByteBuf buffer, OpenBoundaryImmunityPayload payload) {
        buffer.writeVarInt(Math.max(0, payload.entityId));
        buffer.writeUtf(payload.displayName == null ? "" : payload.displayName, 128);
        buffer.writeVarInt(Math.min(MAX_SELECTED, payload.selectedBoundaryIds.size()));
        for (int index = 0; index < Math.min(MAX_SELECTED, payload.selectedBoundaryIds.size()); index++) {
            buffer.writeUtf(payload.selectedBoundaryIds.get(index), 64);
        }
    }

    private static OpenBoundaryImmunityPayload decode(RegistryFriendlyByteBuf buffer) {
        int entityId = buffer.readVarInt();
        String displayName = buffer.readUtf(128);
        int count = Math.max(0, Math.min(MAX_SELECTED, buffer.readVarInt()));
        List<String> selected = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            selected.add(buffer.readUtf(64));
        }
        return new OpenBoundaryImmunityPayload(entityId, displayName, selected);
    }
}
