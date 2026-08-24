package net.xxxjk.TYPE_MOON_WORLD.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xxxjk.TYPE_MOON_WORLD.client.ClientPacketHandler;
import org.jetbrains.annotations.NotNull;

public record OpenServantCommandScreenMessage(int entityId) implements CustomPacketPayload {
   public static final Type<OpenServantCommandScreenMessage> TYPE = new Type<>(
      ResourceLocation.fromNamespaceAndPath("typemoonworld", "open_servant_command_screen")
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, OpenServantCommandScreenMessage> STREAM_CODEC = StreamCodec.of(
      (buffer, message) -> buffer.writeVarInt(message.entityId),
      buffer -> new OpenServantCommandScreenMessage(buffer.readVarInt())
   );

   @Override
   @NotNull
   public Type<OpenServantCommandScreenMessage> type() {
      return TYPE;
   }

   public static void handleData(OpenServantCommandScreenMessage message, IPayloadContext context) {
      context.enqueueWork(() -> ClientPacketHandler.openServantCommandScreen(message.entityId));
   }
}
