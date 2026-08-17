package com.example.typemoonaddon.network;

import com.example.typemoonaddon.TypeMoonAddon;
import com.example.typemoonaddon.data.ImaginarySpaceData;
import com.example.typemoonaddon.magic.SakuraSummonBlackMudService;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record SetBlackMudSummonModePayload(int modeId) implements CustomPacketPayload {
    public static final Type<SetBlackMudSummonModePayload> TYPE = new Type<>(TypeMoonAddon.id("set_black_mud_summon_mode"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SetBlackMudSummonModePayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> buffer.writeVarInt(payload.modeId),
            buffer -> new SetBlackMudSummonModePayload(buffer.readVarInt())
    );

    @Override
    public @NotNull Type<SetBlackMudSummonModePayload> type() {
        return TYPE;
    }

    public static void handle(SetBlackMudSummonModePayload message, IPayloadContext context) {
        if (context.flow() != PacketFlow.SERVERBOUND || !(context.player() instanceof ServerPlayer player)) {
            return;
        }
        context.enqueueWork(() -> {
            ImaginarySpaceData.BlackMudSummonMode[] modes = ImaginarySpaceData.BlackMudSummonMode.values();
            if (player.isAlive() && !player.isSpectator() && message.modeId >= 0 && message.modeId < modes.length) {
                SakuraSummonBlackMudService.selectMode(player, modes[message.modeId]);
            }
        });
    }
}
