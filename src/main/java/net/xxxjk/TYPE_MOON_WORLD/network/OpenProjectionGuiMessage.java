package net.xxxjk.TYPE_MOON_WORLD.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.client.gui.ProjectionPresetScreen;
import org.jetbrains.annotations.NotNull;

public record OpenProjectionGuiMessage() implements CustomPacketPayload {
    public static final Type<OpenProjectionGuiMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "open_projection_gui"));
    public static final StreamCodec<FriendlyByteBuf, OpenProjectionGuiMessage> STREAM_CODEC = StreamCodec.unit(new OpenProjectionGuiMessage());

    @Override
    public @NotNull Type<OpenProjectionGuiMessage> type() {
        return TYPE;
    }

    public static void handleData(final OpenProjectionGuiMessage message, final IPayloadContext context) {
        context.enqueueWork(() -> {
            // Client Side
            Minecraft.getInstance().setScreen(new ProjectionPresetScreen(Minecraft.getInstance().player));
        });
    }
}
