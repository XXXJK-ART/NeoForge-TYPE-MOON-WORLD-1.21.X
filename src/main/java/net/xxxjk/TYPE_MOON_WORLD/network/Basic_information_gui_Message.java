package net.xxxjk.TYPE_MOON_WORLD.network;

import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.network.RegistryFriendlyByteBuf;

import net.xxxjk.TYPE_MOON_WORLD.procedures.Open_basic_information;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import org.jetbrains.annotations.NotNull;

public record Basic_information_gui_Message(int eventType, int pressedms) implements CustomPacketPayload {
    public static final Type<Basic_information_gui_Message> TYPE
            = new Type<>(ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID,
            "key_basicinformationgui"));
    public static final StreamCodec<RegistryFriendlyByteBuf, Basic_information_gui_Message> STREAM_CODEC
            = StreamCodec.of((RegistryFriendlyByteBuf buffer, Basic_information_gui_Message message) -> {
        buffer.writeInt(message.eventType);
        buffer.writeInt(message.pressedms);
    }, (RegistryFriendlyByteBuf buffer)
            -> new Basic_information_gui_Message(buffer.readInt(), buffer.readInt()));

    @Override
    public @NotNull Type<Basic_information_gui_Message> type() {
        return TYPE;
    }

    public static void handleData(final Basic_information_gui_Message message, final IPayloadContext context) {
        if (context.flow() == PacketFlow.SERVERBOUND) {
            context.enqueueWork(() ->
                    pressAction(context.player(), message.eventType, message.pressedms)).exceptionally(e -> {
                context.connection().disconnect(Component.literal(e.getMessage()));
                return null;
            });
        }
    }

    public static void pressAction(Player entity, int type, int pressedms) {
        Level world = entity.level();
        double x = entity.getX();
        double y = entity.getY();
        double z = entity.getZ();
        // security measure to prevent arbitrary chunk generation
        if (world.isLoaded(entity.blockPosition())) if (type == 0) {
            Open_basic_information.execute(world, x, y, z, entity);
        }
    }
}