package net.xxxjk.TYPE_MOON_WORLD.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.minecraft.network.protocol.PacketFlow;

import net.minecraft.network.codec.ByteBufCodecs;

public record SwitchMagicMessage(String magicId) implements CustomPacketPayload {
    public static final Type<SwitchMagicMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "switch_magic"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SwitchMagicMessage> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8,
        SwitchMagicMessage::magicId,
        SwitchMagicMessage::new
    );

    @Override
    public Type<SwitchMagicMessage> type() {
        return TYPE;
    }

    public static void handleData(final SwitchMagicMessage message, final IPayloadContext context) {
        if (context.flow() == PacketFlow.SERVERBOUND) {
            context.enqueueWork(() -> {
                net.minecraft.world.entity.player.Player player = context.player();
                TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
                
                if (vars.selected_magics.contains(message.magicId)) {
                    int index = vars.selected_magics.indexOf(message.magicId);
                    if (index >= 0) {
                        vars.current_magic_index = index;
                        vars.syncPlayerVariables(player);
                    }
                }
            }).exceptionally(e -> {
                context.connection().disconnect(net.minecraft.network.chat.Component.literal(e.getMessage()));
                return null;
            });
        }
    }
}
