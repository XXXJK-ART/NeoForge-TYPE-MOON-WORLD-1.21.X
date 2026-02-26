package net.xxxjk.TYPE_MOON_WORLD.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import org.jetbrains.annotations.NotNull;

public record SelectProjectionStructureMessage(String structureId) implements CustomPacketPayload {
    public static final Type<SelectProjectionStructureMessage> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "select_projection_structure"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SelectProjectionStructureMessage> STREAM_CODEC = StreamCodec.of(
            (buffer, message) -> buffer.writeUtf(message.structureId == null ? "" : message.structureId, 128),
            buffer -> new SelectProjectionStructureMessage(buffer.readUtf(128))
    );

    @Override
    public @NotNull Type<SelectProjectionStructureMessage> type() {
        return TYPE;
    }

    public static void handleData(final SelectProjectionStructureMessage message, final IPayloadContext context) {
        if (context.flow() != PacketFlow.SERVERBOUND) {
            return;
        }
        context.enqueueWork(() -> {
            TypeMoonWorldModVariables.PlayerVariables vars = context.player().getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
            String id = message.structureId == null ? "" : message.structureId;
            if (id.isEmpty() || vars.getStructureById(id) == null) {
                vars.projection_selected_structure_id = "";
            } else {
                vars.projection_selected_structure_id = id;
                vars.projection_selected_item = net.minecraft.world.item.ItemStack.EMPTY;
            }
            vars.syncPlayerVariables(context.player());
        });
    }
}
