package net.xxxjk.TYPE_MOON_WORLD.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardTransformManager;
import org.jetbrains.annotations.NotNull;

public record ServantCardActionMessage(int slot) implements CustomPacketPayload {
   public static final Type<ServantCardActionMessage> TYPE = new Type<>(
      ResourceLocation.fromNamespaceAndPath("typemoonworld", "servant_card_action")
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, ServantCardActionMessage> STREAM_CODEC = StreamCodec.of(
      (buffer, message) -> buffer.writeInt(message.slot),
      buffer -> new ServantCardActionMessage(buffer.readInt())
   );

   @Override
   @NotNull
   public Type<ServantCardActionMessage> type() {
      return TYPE;
   }

   public static void handleData(ServantCardActionMessage message, IPayloadContext context) {
      if (context.flow() != PacketFlow.SERVERBOUND || message.slot < -1 || message.slot > 9) return;
      context.enqueueWork(() -> {
         if (context.player() instanceof ServerPlayer player
            && ServerPacketRateLimiter.allow(player, "servant_card_action", 1)) {
            ServantCardTransformManager.triggerAction(player, message.slot);
         }
      });
   }
}
