package net.xxxjk.TYPE_MOON_WORLD.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.world.inventory.MagicalattributesMenu;

public record PageChangeMessage(int page) implements CustomPacketPayload {
    public static final Type<PageChangeMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "page_change"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PageChangeMessage> STREAM_CODEC = StreamCodec.of(
            (RegistryFriendlyByteBuf buffer, PageChangeMessage message) -> buffer.writeInt(message.page),
            (RegistryFriendlyByteBuf buffer) -> new PageChangeMessage(buffer.readInt())
    );

    @Override
    public Type<PageChangeMessage> type() {
        return TYPE;
    }

    public static void handleData(final PageChangeMessage message, final IPayloadContext context) {
        if (context.flow() == PacketFlow.SERVERBOUND) {
            context.enqueueWork(() -> {
                if (context.player().containerMenu instanceof MagicalattributesMenu menu) {
                    menu.setPage(message.page);
                }
            });
        }
    }
}
