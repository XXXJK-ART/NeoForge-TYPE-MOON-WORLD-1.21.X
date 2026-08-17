package com.example.typemoonaddon.network;

import com.example.typemoonaddon.TypeMoonAddon;
import com.example.typemoonaddon.data.ImaginarySpaceData;
import com.example.typemoonaddon.magic.SakuraBlackMudControlService;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record SetShadowCommandModePayload(int modeId) implements CustomPacketPayload {
    public static final Type<SetShadowCommandModePayload> TYPE = new Type<>(TypeMoonAddon.id("set_shadow_command_mode"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SetShadowCommandModePayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> buffer.writeVarInt(payload.modeId),
            buffer -> new SetShadowCommandModePayload(buffer.readVarInt())
    );

    @Override
    public @NotNull Type<SetShadowCommandModePayload> type() {
        return TYPE;
    }

    public static void handle(SetShadowCommandModePayload message, IPayloadContext context) {
        if (context.flow() != PacketFlow.SERVERBOUND || !(context.player() instanceof ServerPlayer player)) {
            return;
        }
        context.enqueueWork(() -> {
            ImaginarySpaceData.ShadowCommandMode[] modes = ImaginarySpaceData.ShadowCommandMode.values();
            if (player.isAlive() && !player.isSpectator() && message.modeId >= 0 && message.modeId < modes.length) {
                SakuraBlackMudControlService.selectMode(player, modes[message.modeId]);
            }
        });
    }
}
