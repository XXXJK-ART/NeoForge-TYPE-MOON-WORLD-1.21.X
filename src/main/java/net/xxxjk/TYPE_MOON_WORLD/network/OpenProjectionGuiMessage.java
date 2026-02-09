package net.xxxjk.TYPE_MOON_WORLD.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import org.jetbrains.annotations.NotNull;

public record OpenProjectionGuiMessage() implements CustomPacketPayload {
    public static final Type<OpenProjectionGuiMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "open_projection_gui"));
    public static final StreamCodec<FriendlyByteBuf, OpenProjectionGuiMessage> STREAM_CODEC = StreamCodec.unit(new OpenProjectionGuiMessage());

    @Override
    public @NotNull Type<OpenProjectionGuiMessage> type() {
        return TYPE;
    }

    public static void handleData(final OpenProjectionGuiMessage message, final IPayloadContext context) {
        if (context.flow() == net.minecraft.network.protocol.PacketFlow.CLIENTBOUND) {
            context.enqueueWork(() -> {
                // Client Side
                // Use fully qualified name to avoid import causing server-side class loading issues
                // And check if we are physically on client to be safe (though flow check is usually enough)
                if (net.neoforged.fml.loading.FMLEnvironment.dist == net.neoforged.api.distmarker.Dist.CLIENT) {
                     ClientHandler.handle();
                }
            });
        }
    }

    // Inner class to isolate client code
    private static class ClientHandler {
        public static void handle() {
            net.xxxjk.TYPE_MOON_WORLD.client.ClientPacketHandler.openProjectionGui();
        }
    }
}
