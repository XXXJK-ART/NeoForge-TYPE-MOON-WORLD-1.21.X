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
import net.minecraft.core.BlockPos;

import net.xxxjk.TYPE_MOON_WORLD.procedures.To_basic_information;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import org.jetbrains.annotations.NotNull;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public record Magical_attributes_Button_Message(int buttonID, int x, int y, int z) implements CustomPacketPayload {
    public static final Type<Magical_attributes_Button_Message> TYPE
            = new Type<>(ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID,
            "magicalattributes_buttons"));
    public static final StreamCodec<RegistryFriendlyByteBuf, Magical_attributes_Button_Message> STREAM_CODEC
            = StreamCodec.of((RegistryFriendlyByteBuf buffer, Magical_attributes_Button_Message message) -> {
        buffer.writeInt(message.buttonID);
        buffer.writeInt(message.x);
        buffer.writeInt(message.y);
        buffer.writeInt(message.z);
    }, (RegistryFriendlyByteBuf buffer) -> new Magical_attributes_Button_Message(buffer.readInt(), buffer.readInt(), buffer.readInt(), buffer.readInt()));
    @Override
    public @NotNull Type<Magical_attributes_Button_Message> type() {
        return TYPE;
    }

    public static void handleData(final Magical_attributes_Button_Message message, final IPayloadContext context) {
        if (context.flow() == PacketFlow.SERVERBOUND) {
            context.enqueueWork(() -> {
                Player entity = context.player();
                int buttonID = message.buttonID;
                int x = message.x;
                int y = message.y;
                int z = message.z;
                handleButtonAction(entity, buttonID, x, y, z);
            }).exceptionally(e -> {
                context.connection().disconnect(Component.literal(e.getMessage()));
                return null;
            });
        }
    }

    public static void handleButtonAction(Player entity, int buttonID, int x, int y, int z) {
        Level world = entity.getCommandSenderWorld();
        BlockPos pos = BlockPos.containing(x, y, z);
        if (!world.isLoaded(pos)) {
            return;
        }
        if (buttonID == 0 && !world.isClientSide()) {
            To_basic_information.execute(world, x, y, z, entity);
        }
    }

    @SubscribeEvent
    public static void registerMessage(FMLCommonSetupEvent event) {
        TYPE_MOON_WORLD.addNetworkMessage(Magical_attributes_Button_Message.TYPE,
                Magical_attributes_Button_Message.STREAM_CODEC, Magical_attributes_Button_Message::handleData);
    }
}