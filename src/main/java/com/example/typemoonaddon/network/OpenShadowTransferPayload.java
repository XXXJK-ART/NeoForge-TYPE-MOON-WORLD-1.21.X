package com.example.typemoonaddon.network;

import com.example.typemoonaddon.TypeMoonAddon;
import com.example.typemoonaddon.client.ShadowTransferScreen;
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

public record OpenShadowTransferPayload(int originX, int originZ, List<Target> targets) implements CustomPacketPayload {
    private static final int MAX_TARGETS = 256;

    public static final Type<OpenShadowTransferPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(TypeMoonAddon.MOD_ID, "open_shadow_transfer")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenShadowTransferPayload> STREAM_CODEC = StreamCodec.of(
            OpenShadowTransferPayload::encode,
            OpenShadowTransferPayload::decode
    );

    public OpenShadowTransferPayload {
        targets = List.copyOf(targets);
    }

    @Override
    public @NotNull Type<OpenShadowTransferPayload> type() {
        return TYPE;
    }

    public static void handle(OpenShadowTransferPayload message, IPayloadContext context) {
        if (context.flow() != PacketFlow.CLIENTBOUND) {
            return;
        }
        context.enqueueWork(() -> Minecraft.getInstance().setScreen(new ShadowTransferScreen(message)));
    }

    private static void encode(RegistryFriendlyByteBuf buffer, OpenShadowTransferPayload payload) {
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
    }

    private static OpenShadowTransferPayload decode(RegistryFriendlyByteBuf buffer) {
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
        return new OpenShadowTransferPayload(originX, originZ, targets);
    }

    public record Target(UUID familiarId, int x, int y, int z, int distance, int manaCost) {
    }
}
