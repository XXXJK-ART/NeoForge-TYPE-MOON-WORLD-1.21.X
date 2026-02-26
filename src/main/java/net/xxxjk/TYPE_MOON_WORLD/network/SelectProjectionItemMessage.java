package net.xxxjk.TYPE_MOON_WORLD.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import org.jetbrains.annotations.NotNull;

public record SelectProjectionItemMessage(int index) implements CustomPacketPayload {
    public static final Type<SelectProjectionItemMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "select_projection_item"));
    public static final StreamCodec<FriendlyByteBuf, SelectProjectionItemMessage> STREAM_CODEC = StreamCodec.of((buffer, message) -> buffer.writeInt(message.index), buffer -> new SelectProjectionItemMessage(buffer.readInt()));

    @Override
    public @NotNull Type<SelectProjectionItemMessage> type() {
        return TYPE;
    }

    public static void handleData(final SelectProjectionItemMessage message, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
                if (message.index >= 0 && message.index < vars.analyzed_items.size()) {
                    vars.projection_selected_item = vars.analyzed_items.get(message.index).copy();
                    vars.projection_selected_structure_id = "";
                    vars.syncPlayerVariables(player);
                }
            }
        });
    }
}
