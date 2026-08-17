package com.example.typemoonaddon.network;

import com.example.typemoonaddon.TypeMoonAddon;
import com.example.typemoonaddon.magic.SakuraPollutionService;
import com.example.typemoonaddon.magic.SakuraTypeMoonIntegration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record SetDevourerSelectionPayload(List<UUID> rosterIds) implements CustomPacketPayload {
    private static final int MAX_SELECTIONS = 256;

    public static final Type<SetDevourerSelectionPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(TypeMoonAddon.MOD_ID, "set_devourer_selection")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, SetDevourerSelectionPayload> STREAM_CODEC = StreamCodec.of(
            SetDevourerSelectionPayload::encode,
            SetDevourerSelectionPayload::decode
    );

    public SetDevourerSelectionPayload {
        rosterIds = List.copyOf(rosterIds);
    }

    @Override
    public @NotNull Type<SetDevourerSelectionPayload> type() {
        return TYPE;
    }

    public static void handle(SetDevourerSelectionPayload message, IPayloadContext context) {
        if (context.flow() != PacketFlow.SERVERBOUND || !(context.player() instanceof ServerPlayer player)) {
            return;
        }
        context.enqueueWork(() -> {
            if (player.isAlive() && !player.isSpectator() && SakuraTypeMoonIntegration.isHeroicSpiritDevourerLearned(player)) {
                SakuraPollutionService.setDevourerSelection(player, message.rosterIds);
            }
        });
    }

    private static void encode(RegistryFriendlyByteBuf buffer, SetDevourerSelectionPayload payload) {
        buffer.writeVarInt(payload.rosterIds.size());
        for (UUID rosterId : payload.rosterIds) {
            buffer.writeUUID(rosterId);
        }
    }

    private static SetDevourerSelectionPayload decode(RegistryFriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        if (count < 0 || count > MAX_SELECTIONS) {
            throw new IllegalArgumentException("Invalid devourer selection size: " + count);
        }
        List<UUID> rosterIds = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            rosterIds.add(buffer.readUUID());
        }
        return new SetDevourerSelectionPayload(rosterIds);
    }
}
