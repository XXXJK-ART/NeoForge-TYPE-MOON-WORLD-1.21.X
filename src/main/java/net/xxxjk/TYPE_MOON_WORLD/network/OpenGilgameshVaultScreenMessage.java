package net.xxxjk.TYPE_MOON_WORLD.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xxxjk.TYPE_MOON_WORLD.client.ClientPacketHandler;
import org.jetbrains.annotations.NotNull;

public record OpenGilgameshVaultScreenMessage(int usedMask) implements CustomPacketPayload {
   public static final Type<OpenGilgameshVaultScreenMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("typemoonworld", "open_gilgamesh_vault"));
   public static final StreamCodec<RegistryFriendlyByteBuf, OpenGilgameshVaultScreenMessage> STREAM_CODEC = StreamCodec.of(
      (buffer, message) -> buffer.writeVarInt(message.usedMask), buffer -> new OpenGilgameshVaultScreenMessage(buffer.readVarInt()));
   @Override @NotNull public Type<OpenGilgameshVaultScreenMessage> type() { return TYPE; }
   public static void handleData(OpenGilgameshVaultScreenMessage message, IPayloadContext context) { context.enqueueWork(() -> ClientPacketHandler.openGilgameshVaultScreen(message.usedMask)); }
}
