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

public record ServantCardHoldActionMessage(int slot, boolean pressed) implements CustomPacketPayload {
   public static final Type<ServantCardHoldActionMessage> TYPE = new Type<>(
      ResourceLocation.fromNamespaceAndPath("typemoonworld", "servant_card_hold_action")
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, ServantCardHoldActionMessage> STREAM_CODEC = StreamCodec.of(
      (buffer, message) -> {
         buffer.writeInt(message.slot);
         buffer.writeBoolean(message.pressed);
      },
      buffer -> new ServantCardHoldActionMessage(buffer.readInt(), buffer.readBoolean())
   );

   @Override
   @NotNull
   public Type<ServantCardHoldActionMessage> type() {
      return TYPE;
   }

   public static void handleData(ServantCardHoldActionMessage message, IPayloadContext context) {
      if (context.flow() != PacketFlow.SERVERBOUND || message.slot < 0 || message.slot > 9) return;
      context.enqueueWork(() -> {
         if (context.player() instanceof ServerPlayer player
            && ServerPacketRateLimiter.allow(player, "servant_card_hold_" + message.slot, 1)) {
            ServantCardTransformManager.handleHoldAction(player, message.slot, message.pressed);
         }
      });
   }
}
