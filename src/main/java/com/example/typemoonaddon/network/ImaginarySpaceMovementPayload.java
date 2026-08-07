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

/** Client movement intent while the player is swimming through imaginary space. */
public record ImaginarySpaceMovementPayload(byte verticalInput) implements CustomPacketPayload {
    public static final Type<ImaginarySpaceMovementPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(TypeMoonAddon.MOD_ID, "imaginary_space_movement"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ImaginarySpaceMovementPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buffer, message) -> buffer.writeByte(message.verticalInput),
                    buffer -> new ImaginarySpaceMovementPayload(buffer.readByte()));

    @Override
    public @NotNull Type<ImaginarySpaceMovementPayload> type() {
        return TYPE;
    }

    public static void handle(ImaginarySpaceMovementPayload message, IPayloadContext context) {
        if (context.flow() != PacketFlow.SERVERBOUND
                || !(context.player() instanceof ServerPlayer player)) {
            return;
        }
        int input = Math.max(-1, Math.min(1, message.verticalInput));
        context.enqueueWork(() -> ImaginarySpaceService.handleMovementInput(player, input));
    }
}
