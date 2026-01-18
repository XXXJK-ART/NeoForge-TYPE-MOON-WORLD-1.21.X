package net.xxxjk.TYPE_MOON_WORLD.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

public record CycleMagicMessage(boolean forward) implements CustomPacketPayload {
    public static final Type<CycleMagicMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "cycle_magic"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CycleMagicMessage> STREAM_CODEC = StreamCodec.of(
            (RegistryFriendlyByteBuf buffer, CycleMagicMessage message) -> {
                buffer.writeBoolean(message.forward);
            },
            (RegistryFriendlyByteBuf buffer) -> new CycleMagicMessage(buffer.readBoolean())
    );

    @Override
    public Type<CycleMagicMessage> type() {
        return TYPE;
    }

    public static void handleData(final CycleMagicMessage message, final IPayloadContext context) {
        if (context.flow() == PacketFlow.SERVERBOUND) {
            context.enqueueWork(() -> {
                net.minecraft.world.entity.player.Player player = context.player();
                TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
                if (vars.selected_magics.isEmpty()) {
                    vars.current_magic_index = 0;
                } else {
                    if (message.forward) {
                        vars.current_magic_index++;
                        if (vars.current_magic_index >= vars.selected_magics.size()) {
                            vars.current_magic_index = 0;
                        }
                    } else {
                        vars.current_magic_index--;
                        if (vars.current_magic_index < 0) {
                            vars.current_magic_index = vars.selected_magics.size() - 1;
                        }
                    }
                }
                vars.syncPlayerVariables(player);
            }).exceptionally(e -> {
                context.connection().disconnect(net.minecraft.network.chat.Component.literal(e.getMessage()));
                return null;
            });
        }
    }
}
