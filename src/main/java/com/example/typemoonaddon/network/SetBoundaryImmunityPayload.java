package com.example.typemoonaddon.network;

import com.example.typemoonaddon.TypeMoonAddon;
import com.example.typemoonaddon.magic.BoundaryMagicIntegration;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record SetBoundaryImmunityPayload(int entityId, List<String> boundaryIds) implements CustomPacketPayload {
    private static final int MAX_BOUNDARIES = 32;

    public static final Type<SetBoundaryImmunityPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(TypeMoonAddon.MOD_ID, "set_boundary_immunity")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, SetBoundaryImmunityPayload> STREAM_CODEC = StreamCodec.of(
            SetBoundaryImmunityPayload::encode,
            SetBoundaryImmunityPayload::decode
    );

    public SetBoundaryImmunityPayload {
        boundaryIds = List.copyOf(boundaryIds);
    }

    @Override
    public @NotNull Type<SetBoundaryImmunityPayload> type() {
        return TYPE;
    }

    public static void handle(SetBoundaryImmunityPayload message, IPayloadContext context) {
        if (context.flow() != PacketFlow.SERVERBOUND || !(context.player() instanceof ServerPlayer player)) {
            return;
        }
        context.enqueueWork(() -> {
            if (player.serverLevel().getEntity(message.entityId) instanceof net.minecraft.world.entity.LivingEntity target) {
                BoundaryMagicIntegration.setBoundaryImmunity(target, message.boundaryIds);
            }
        });
    }

    private static void encode(RegistryFriendlyByteBuf buffer, SetBoundaryImmunityPayload payload) {
        buffer.writeVarInt(Math.max(0, payload.entityId));
        buffer.writeVarInt(Math.min(MAX_BOUNDARIES, payload.boundaryIds.size()));
        for (int index = 0; index < Math.min(MAX_BOUNDARIES, payload.boundaryIds.size()); index++) {
            buffer.writeUtf(payload.boundaryIds.get(index), 64);
        }
    }

    private static SetBoundaryImmunityPayload decode(RegistryFriendlyByteBuf buffer) {
        int entityId = buffer.readVarInt();
        int count = Math.max(0, Math.min(MAX_BOUNDARIES, buffer.readVarInt()));
        List<String> ids = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            ids.add(buffer.readUtf(64));
        }
        return new SetBoundaryImmunityPayload(entityId, ids);
    }
}
