package net.xxxjk.TYPE_MOON_WORLD.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.magic.other.MagicGander;
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

    public static void pressAction(net.minecraft.world.entity.player.Player entity, int eventType, int pressedMs) {
        if (!(entity instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) {
            return;
        }
        handleEvent(serverPlayer, eventType);
    }

    @Override
    public Type<CastMagicMessage> type() {
        return TYPE;
    }

    public static void handleData(final CastMagicMessage message, final IPayloadContext context) {
        if (context.flow() == PacketFlow.SERVERBOUND) {
            context.enqueueWork(() -> {
                if (context.player() instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                    handleEvent(serverPlayer, message.eventType);
                }
            }).exceptionally(e -> {
                context.connection().disconnect(net.minecraft.network.chat.Component.literal(e.getMessage()));
                return null;
            });
        }
    }

    private static void handleEvent(net.minecraft.server.level.ServerPlayer player, int eventType) {
        if (eventType == 1) {
            MagicGander.beginCharge(player);
            return;
        }
        if (eventType == 2) {
            boolean released = MagicGander.releaseCharge(player);
            if (released) {
                net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables.PlayerVariables vars =
                        player.getData(net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables.PLAYER_VARIABLES);
                vars.magic_cooldown = 10.0D;
                vars.syncMana(player);
            }
            return;
        }
        CastMagic.execute(player);
    }


}
