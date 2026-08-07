package com.example.typemoonaddon.network;

import com.example.typemoonaddon.TypeMoonAddon;
import com.example.typemoonaddon.airflow_blade.AirflowBladeService;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record AirflowBladeModeSwitchPayload() implements CustomPacketPayload {
    public static final Type<AirflowBladeModeSwitchPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(TypeMoonAddon.MOD_ID, "airflow_blade_mode_switch"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AirflowBladeModeSwitchPayload> STREAM_CODEC =
            StreamCodec.of((buffer, message) -> {
            }, buffer -> new AirflowBladeModeSwitchPayload());

    @Override
    public @NotNull Type<AirflowBladeModeSwitchPayload> type() {
        return TYPE;
    }

    public static void handle(AirflowBladeModeSwitchPayload message, IPayloadContext context) {
        if (context.flow() != PacketFlow.SERVERBOUND
                || !(context.player() instanceof ServerPlayer player)) {
            return;
        }
        context.enqueueWork(() -> AirflowBladeService.switchMode(player));
    }
}
