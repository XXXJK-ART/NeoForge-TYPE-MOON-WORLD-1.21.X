package com.captain.servant.network.payload;

import com.captain.servant.network.SpiritualizationClientState;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import static com.captain.servant.TypeMoonServant.MOD_ID;

public record SpiritualizationStatePayload(
        UUID playerId,
        int phase,
        boolean invisible,
        boolean restricted,
        int cooldownTicks
) implements CustomPacketPayload {
    public static final Type<SpiritualizationStatePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "spiritualization_state")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, SpiritualizationStatePayload> STREAM_CODEC =
            StreamCodec.of(SpiritualizationStatePayload::write, SpiritualizationStatePayload::read);

    private static void write(RegistryFriendlyByteBuf buffer, SpiritualizationStatePayload payload) {
        buffer.writeUUID(payload.playerId());
        buffer.writeVarInt(payload.phase());
        buffer.writeBoolean(payload.invisible());
        buffer.writeBoolean(payload.restricted());
        buffer.writeVarInt(Math.max(0, payload.cooldownTicks()));
    }

    private static SpiritualizationStatePayload read(RegistryFriendlyByteBuf buffer) {
        return new SpiritualizationStatePayload(
                buffer.readUUID(),
                buffer.readVarInt(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readVarInt()
        );
    }

    @Override
    public Type<SpiritualizationStatePayload> type() {
        return TYPE;
    }

    public static void handleData(SpiritualizationStatePayload payload, IPayloadContext context) {
        if (context.flow() != PacketFlow.CLIENTBOUND) {
            return;
        }
        context.enqueueWork(() -> SpiritualizationClientState.apply(payload));
    }
}
