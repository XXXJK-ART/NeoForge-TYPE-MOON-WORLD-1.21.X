package net.xxxjk.TYPE_MOON_WORLD.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.minecraft.resources.ResourceLocation;

import net.neoforged.neoforge.network.handling.IPayloadContext;
public record SelectMagicMessage(String magicId, boolean add) implements CustomPacketPayload {
    public static final Type<SelectMagicMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("type_moon_world", "select_magic"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SelectMagicMessage> STREAM_CODEC = StreamCodec.of(
            (RegistryFriendlyByteBuf buffer, SelectMagicMessage message) -> {
                buffer.writeUtf(message.magicId);
                buffer.writeBoolean(message.add);
            },
            (RegistryFriendlyByteBuf buffer) -> new SelectMagicMessage(buffer.readUtf(), buffer.readBoolean())
    );

    @Override
    public Type<SelectMagicMessage> type() {
        return TYPE;
    }

    public static void handleData(final SelectMagicMessage message, final IPayloadContext context) {
        if (context.flow() == PacketFlow.SERVERBOUND) {
            context.enqueueWork(() -> {
                net.minecraft.world.entity.player.Player player = context.player();
                TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
                if (message.add) {
                    if (!vars.selected_magics.contains(message.magicId) && vars.selected_magics.size() < 20) {
                        vars.selected_magics.add(message.magicId);
                    }
                } else {
                    vars.selected_magics.remove(message.magicId);
                    if (vars.current_magic_index >= vars.selected_magics.size()) {
                        vars.current_magic_index = Math.max(0, vars.selected_magics.size() - 1);
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
