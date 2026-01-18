package net.xxxjk.TYPE_MOON_WORLD.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.procedures.ToggleMagicCircuit;

public record MagicCircuitSwitchMessage(int eventType, int pressedms) implements CustomPacketPayload {
    public static final Type<MagicCircuitSwitchMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "magic_circuit_switch"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MagicCircuitSwitchMessage> STREAM_CODEC = StreamCodec.of(
            (RegistryFriendlyByteBuf buffer, MagicCircuitSwitchMessage message) -> {
                buffer.writeInt(message.eventType);
                buffer.writeInt(message.pressedms);
            },
            (RegistryFriendlyByteBuf buffer) -> new MagicCircuitSwitchMessage(buffer.readInt(), buffer.readInt())
    );

    @Override
    public Type<MagicCircuitSwitchMessage> type() {
        return TYPE;
    }

    public static void handleData(final MagicCircuitSwitchMessage message, final IPayloadContext context) {
        if (context.flow() == PacketFlow.SERVERBOUND) {
            context.enqueueWork(() -> {
                ToggleMagicCircuit.execute(context.player());
            }).exceptionally(e -> {
                context.connection().disconnect(net.minecraft.network.chat.Component.literal(e.getMessage()));
                return null;
            });
        }
    }

    public static void pressAction(net.minecraft.world.entity.player.Player entity, int type, int pressedms) {
        ToggleMagicCircuit.execute(entity);
    }
}
