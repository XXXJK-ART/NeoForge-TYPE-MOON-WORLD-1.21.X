package net.xxxjk.TYPE_MOON_WORLD.network;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.core.BlockPos;

import net.xxxjk.TYPE_MOON_WORLD.procedures.To_magical_attributes;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import org.jetbrains.annotations.NotNull;

public record Basic_information_Button_Message(int buttonID, int x, int y, int z) implements CustomPacketPayload {
    public static final Type<Basic_information_Button_Message> TYPE
            = new Type<>(ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID,
            "basicinformation_buttons"));
    public static final StreamCodec<RegistryFriendlyByteBuf, Basic_information_Button_Message> STREAM_CODEC
            = StreamCodec.of((RegistryFriendlyByteBuf buffer, Basic_information_Button_Message message) -> {
        buffer.writeInt(message.buttonID);
        buffer.writeInt(message.x);
        buffer.writeInt(message.y);
        buffer.writeInt(message.z);
    }, (RegistryFriendlyByteBuf buffer) -> new Basic_information_Button_Message(buffer.readInt(), buffer.readInt(), buffer.readInt(), buffer.readInt()));
    @Override
    public @NotNull Type<Basic_information_Button_Message> type() {
        return TYPE;
    }

    public static void handleData(final Basic_information_Button_Message message, final IPayloadContext context) {
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
        if (!world.isClientSide()) {
            if (buttonID == 0 || buttonID == 1) {
                To_magical_attributes.execute(world, x, y, z, entity, buttonID);
            }
        }
    }
}
