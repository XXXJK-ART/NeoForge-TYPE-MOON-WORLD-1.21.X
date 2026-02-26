package net.xxxjk.TYPE_MOON_WORLD.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.magic.projection.StructureProjectionBuildHandler;
import org.jetbrains.annotations.NotNull;

public record StartStructureProjectionMessage(
        String structureId,
        int anchorX,
        int anchorY,
        int anchorZ,
        int rotationIndex
) implements CustomPacketPayload {
    public static final Type<StartStructureProjectionMessage> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "start_structure_projection"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StartStructureProjectionMessage> STREAM_CODEC = StreamCodec.of(
            (buffer, message) -> {
                buffer.writeUtf(message.structureId == null ? "" : message.structureId, 128);
                buffer.writeInt(message.anchorX);
                buffer.writeInt(message.anchorY);
                buffer.writeInt(message.anchorZ);
                buffer.writeInt(message.rotationIndex);
            },
            buffer -> new StartStructureProjectionMessage(
                    buffer.readUtf(128),
                    buffer.readInt(),
                    buffer.readInt(),
                    buffer.readInt(),
                    buffer.readInt()
            )
    );

    @Override
    public @NotNull Type<StartStructureProjectionMessage> type() {
        return TYPE;
    }

    public static void handleData(final StartStructureProjectionMessage message, final IPayloadContext context) {
        if (context.flow() != PacketFlow.SERVERBOUND) {
            return;
        }
        context.enqueueWork(() -> {
            if (!StructureProjectionBuildHandler.startProjection(context.player(),
                    message.structureId == null ? "" : message.structureId,
                    new BlockPos(message.anchorX, message.anchorY, message.anchorZ),
                    message.rotationIndex)) {
                context.player().displayClientMessage(Component.translatable("message.typemoonworld.projection.structure.start_failed"), true);
            }
        });
    }
}
