package net.xxxjk.TYPE_MOON_WORLD.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.procedures.CastMagic;

@SuppressWarnings("null")
public record CastMagicMessage(int eventType, int pressedms) implements CustomPacketPayload {
    public static final Type<CastMagicMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "cast_magic"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CastMagicMessage> STREAM_CODEC = StreamCodec.of(
            (RegistryFriendlyByteBuf buffer, CastMagicMessage message) -> {
                buffer.writeInt(message.eventType);
                buffer.writeInt(message.pressedms);
            },
            (RegistryFriendlyByteBuf buffer) -> new CastMagicMessage(buffer.readInt(), buffer.readInt())
    );

    public CastMagicMessage() {
        this(0, 0);
    }

    public static void pressAction(net.minecraft.world.entity.player.Player entity) {
        CastMagic.execute(entity);
    }

    @Override
    public Type<CastMagicMessage> type() {
        return TYPE;
    }

    public static void handleData(final CastMagicMessage message, final IPayloadContext context) {
        if (context.flow() == PacketFlow.SERVERBOUND) {
            context.enqueueWork(() -> {
                CastMagic.execute(context.player());
            }).exceptionally(e -> {
                context.connection().disconnect(net.minecraft.network.chat.Component.literal(e.getMessage()));
                return null;
            });
        }
    }


}
