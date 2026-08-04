package net.xxxjk.TYPE_MOON_WORLD.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardFlightController;
import org.jetbrains.annotations.NotNull;

public record ServantCardFlightMessage(boolean toggle, float forward, float strafe, float vertical) implements CustomPacketPayload {
   public static final Type<ServantCardFlightMessage> TYPE = new Type<>(
      ResourceLocation.fromNamespaceAndPath("typemoonworld", "servant_card_flight")
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, ServantCardFlightMessage> STREAM_CODEC = StreamCodec.of(
      (buffer, message) -> {
         buffer.writeBoolean(message.toggle);
         buffer.writeFloat(message.forward);
         buffer.writeFloat(message.strafe);
         buffer.writeFloat(message.vertical);
      },
      buffer -> new ServantCardFlightMessage(buffer.readBoolean(), buffer.readFloat(), buffer.readFloat(), buffer.readFloat())
   );

   @Override
   @NotNull
   public Type<ServantCardFlightMessage> type() {
      return TYPE;
   }

   public static void handleData(ServantCardFlightMessage message, IPayloadContext context) {
      if (context.flow() != PacketFlow.SERVERBOUND
         || !Float.isFinite(message.forward) || !Float.isFinite(message.strafe) || !Float.isFinite(message.vertical)) return;
      context.enqueueWork(() -> {
         if (context.player() instanceof ServerPlayer player
            && ServerPacketRateLimiter.allow(player, message.toggle ? "servant_card_flight_toggle" : "servant_card_flight_input",
               message.toggle ? 4 : 1)) {
            ServantCardFlightController.setInput(player, message.toggle, message.forward, message.strafe, message.vertical);
         }
      });
   }
}
