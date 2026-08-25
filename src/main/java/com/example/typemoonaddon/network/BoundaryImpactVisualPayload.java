package com.example.typemoonaddon.network;

import com.example.typemoonaddon.TypeMoonAddon;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record BoundaryImpactVisualPayload(double x, double y, double z, int argbColor) implements CustomPacketPayload {
    public static final Type<BoundaryImpactVisualPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(TypeMoonAddon.MOD_ID, "boundary_impact_visual")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, BoundaryImpactVisualPayload> STREAM_CODEC =
            StreamCodec.of(BoundaryImpactVisualPayload::encode, BoundaryImpactVisualPayload::decode);

    @Override
    public @NotNull Type<BoundaryImpactVisualPayload> type() {
        return TYPE;
    }

    public static void handle(BoundaryImpactVisualPayload message, IPayloadContext context) {
        if (context.flow() == PacketFlow.CLIENTBOUND) {
            context.enqueueWork(() -> com.example.typemoonaddon.client.BoundaryClientVisuals.spawnImpact(
                    new Vec3(message.x, message.y, message.z),
                    message.argbColor
            ));
        }
    }

    private static void encode(RegistryFriendlyByteBuf buffer, BoundaryImpactVisualPayload message) {
        buffer.writeDouble(message.x);
        buffer.writeDouble(message.y);
        buffer.writeDouble(message.z);
        buffer.writeInt(message.argbColor);
    }

    private static BoundaryImpactVisualPayload decode(RegistryFriendlyByteBuf buffer) {
        return new BoundaryImpactVisualPayload(buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readInt());
    }
}
