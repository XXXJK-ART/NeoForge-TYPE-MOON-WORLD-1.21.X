package com.example.typemoonaddon.network;

import com.example.typemoonaddon.TypeMoonAddon;
import com.example.typemoonaddon.data.ImaginarySpaceData;
import com.example.typemoonaddon.magic.SakuraImaginaryStorageService;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record SetImaginaryModePayload(int modeId) implements CustomPacketPayload {
    public static final Type<SetImaginaryModePayload> TYPE = new Type<>(TypeMoonAddon.id("set_imaginary_mode"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SetImaginaryModePayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> buffer.writeVarInt(payload.modeId),
            buffer -> new SetImaginaryModePayload(buffer.readVarInt())
    );

    @Override
    public @NotNull Type<SetImaginaryModePayload> type() {
        return TYPE;
    }

    public static void handle(SetImaginaryModePayload message, IPayloadContext context) {
        if (context.flow() != PacketFlow.SERVERBOUND || !(context.player() instanceof ServerPlayer player)) {
            return;
        }
        context.enqueueWork(() -> {
            ImaginarySpaceData.MagicMode[] modes = ImaginarySpaceData.MagicMode.values();
            if (player.isAlive() && !player.isSpectator() && message.modeId >= 0 && message.modeId < modes.length) {
                SakuraImaginaryStorageService.selectMode(player, modes[message.modeId]);
            }
        });
    }
}
