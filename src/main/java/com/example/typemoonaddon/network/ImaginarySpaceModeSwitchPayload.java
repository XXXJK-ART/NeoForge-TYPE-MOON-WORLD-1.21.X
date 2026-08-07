package com.example.typemoonaddon.network;

import com.example.typemoonaddon.TypeMoonAddon;
import com.example.typemoonaddon.imaginary_space.ImaginarySpaceService;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record ImaginarySpaceModeSwitchPayload() implements CustomPacketPayload {
    public static final Type<ImaginarySpaceModeSwitchPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(TypeMoonAddon.MOD_ID, "imaginary_space_mode_switch"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ImaginarySpaceModeSwitchPayload> STREAM_CODEC =
            StreamCodec.of((buffer, message) -> {
            }, buffer -> new ImaginarySpaceModeSwitchPayload());

    @Override
    public @NotNull Type<ImaginarySpaceModeSwitchPayload> type() {
        return TYPE;
    }

    public static void handle(ImaginarySpaceModeSwitchPayload message, IPayloadContext context) {
        if (context.flow() != PacketFlow.SERVERBOUND
                || !(context.player() instanceof ServerPlayer player)) {
            return;
        }
        context.enqueueWork(() -> ImaginarySpaceService.switchMode(player));
    }
}
