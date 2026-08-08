package com.example.typemoonaddon.network;

import com.example.typemoonaddon.TypeMoonAddon;
import com.example.typemoonaddon.client.EntityDisplacementClientState;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record EntityDisplacementTargetPayload(int entityId, float width, float height, float depth)
        implements CustomPacketPayload {
    public static final Type<EntityDisplacementTargetPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(TypeMoonAddon.MOD_ID, "entity_displacement_target"));
    public static final StreamCodec<RegistryFriendlyByteBuf, EntityDisplacementTargetPayload> STREAM_CODEC =
            StreamCodec.of(EntityDisplacementTargetPayload::encode, EntityDisplacementTargetPayload::decode);

    @Override
    public @NotNull Type<EntityDisplacementTargetPayload> type() {
        return TYPE;
    }

    public static void handle(EntityDisplacementTargetPayload payload, IPayloadContext context) {
        if (context.flow() == PacketFlow.CLIENTBOUND) {
            context.enqueueWork(() -> EntityDisplacementClientState.apply(payload));
        }
    }

    private static void encode(RegistryFriendlyByteBuf buffer, EntityDisplacementTargetPayload payload) {
        buffer.writeVarInt(payload.entityId);
        buffer.writeFloat(payload.width);
        buffer.writeFloat(payload.height);
        buffer.writeFloat(payload.depth);
    }

    private static EntityDisplacementTargetPayload decode(RegistryFriendlyByteBuf buffer) {
        return new EntityDisplacementTargetPayload(buffer.readVarInt(), buffer.readFloat(), buffer.readFloat(), buffer.readFloat());
    }
}
