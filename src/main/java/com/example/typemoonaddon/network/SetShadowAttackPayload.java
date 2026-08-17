package com.example.typemoonaddon.network;

import com.example.typemoonaddon.TypeMoonAddon;
import com.example.typemoonaddon.magic.SakuraBlackMudControlService;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record SetShadowAttackPayload(boolean enabled) implements CustomPacketPayload {
    public static final Type<SetShadowAttackPayload> TYPE = new Type<>(TypeMoonAddon.id("set_shadow_attack"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SetShadowAttackPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> buffer.writeBoolean(payload.enabled),
            buffer -> new SetShadowAttackPayload(buffer.readBoolean())
    );

    @Override
    public @NotNull Type<SetShadowAttackPayload> type() {
        return TYPE;
    }

    public static void handle(SetShadowAttackPayload message, IPayloadContext context) {
        if (context.flow() != PacketFlow.SERVERBOUND || !(context.player() instanceof ServerPlayer player)) {
            return;
        }
        context.enqueueWork(() -> {
            if (player.isAlive() && !player.isSpectator()) {
                SakuraBlackMudControlService.setAttackAround(player, message.enabled);
            }
        });
    }
}
