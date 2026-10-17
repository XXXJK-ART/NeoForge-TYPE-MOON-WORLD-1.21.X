package com.captain.servant.network.payload;

import com.captain.servant.network.FateSupportNetwork;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import static com.captain.servant.TypeMoonServant.MOD_ID;

public record KnightArmoryOpenPayload(int unlockedMask) implements CustomPacketPayload {
    public static final Type<KnightArmoryOpenPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "knight_armory_open")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, KnightArmoryOpenPayload> STREAM_CODEC =
            StreamCodec.of((buffer, payload) -> buffer.writeVarInt(payload.unlockedMask()),
                    buffer -> new KnightArmoryOpenPayload(buffer.readVarInt()));

    @Override
    public Type<KnightArmoryOpenPayload> type() {
        return TYPE;
    }

    public static void handleData(KnightArmoryOpenPayload payload, IPayloadContext context) {
        if (context.flow() == PacketFlow.CLIENTBOUND) {
            context.enqueueWork(() -> FateSupportNetwork.openArmoryScreen(payload.unlockedMask()));
        }
    }
}
