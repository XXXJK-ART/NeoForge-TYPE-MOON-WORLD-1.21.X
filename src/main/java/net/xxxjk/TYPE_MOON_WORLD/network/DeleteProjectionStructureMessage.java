package net.xxxjk.TYPE_MOON_WORLD.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import org.jetbrains.annotations.NotNull;

public record DeleteProjectionStructureMessage(String structureId) implements CustomPacketPayload {
    public static final Type<DeleteProjectionStructureMessage> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "delete_projection_structure"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DeleteProjectionStructureMessage> STREAM_CODEC = StreamCodec.of(
            (buffer, message) -> buffer.writeUtf(message.structureId == null ? "" : message.structureId, 128),
            buffer -> new DeleteProjectionStructureMessage(buffer.readUtf(128))
    );

    @Override
    public @NotNull Type<DeleteProjectionStructureMessage> type() {
        return TYPE;
    }

    public static void handleData(final DeleteProjectionStructureMessage message, final IPayloadContext context) {
        if (context.flow() != PacketFlow.SERVERBOUND) {
            return;
        }
        context.enqueueWork(() -> {
            TypeMoonWorldModVariables.PlayerVariables vars = context.player().getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
            String id = message.structureId == null ? "" : message.structureId;
            if (id.isEmpty()) {
                return;
            }

            if (vars.removeStructureById(id)) {
                context.player().displayClientMessage(Component.translatable("message.typemoonworld.structure.deleted"), true);
                vars.syncPlayerVariables(context.player());
            }
        });
    }
}
