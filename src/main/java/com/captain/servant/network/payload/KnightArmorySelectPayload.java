package com.captain.servant.network.payload;

import com.captain.servant.lancelot.KnightArmoryService;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import static com.captain.servant.TypeMoonServant.MOD_ID;

public record KnightArmorySelectPayload(int entryId) implements CustomPacketPayload {
    public static final Type<KnightArmorySelectPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "knight_armory_select")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, KnightArmorySelectPayload> STREAM_CODEC =
            StreamCodec.of((buffer, payload) -> buffer.writeVarInt(payload.entryId()),
                    buffer -> new KnightArmorySelectPayload(buffer.readVarInt()));

    @Override
    public Type<KnightArmorySelectPayload> type() {
        return TYPE;
    }

    public static void handleData(KnightArmorySelectPayload payload, IPayloadContext context) {
        if (context.flow() == PacketFlow.SERVERBOUND) {
            context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer player) {
                    KnightArmoryService.select(player, payload.entryId());
                }
            });
        }
    }
}
