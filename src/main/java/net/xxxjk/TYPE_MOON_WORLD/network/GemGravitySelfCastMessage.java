package net.xxxjk.TYPE_MOON_WORLD.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.magic.jewel.GemEngravingService;
import org.jetbrains.annotations.NotNull;

public record GemGravitySelfCastMessage(int hand, int targetMode, int mode) implements CustomPacketPayload {
   public static final Type<GemGravitySelfCastMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("typemoonworld", "gem_gravity_self_cast"));
   public static final StreamCodec<RegistryFriendlyByteBuf, GemGravitySelfCastMessage> STREAM_CODEC = StreamCodec.of((buffer, message) -> {
      buffer.writeInt(message.hand);
      buffer.writeInt(message.targetMode);
      buffer.writeInt(message.mode);
   }, buffer -> new GemGravitySelfCastMessage(buffer.readInt(), buffer.readInt(), buffer.readInt()));

   @NotNull
   public Type<GemGravitySelfCastMessage> type() {
      return TYPE;
   }

   public static void handleData(GemGravitySelfCastMessage message, IPayloadContext context) {
      if (context.flow() == PacketFlow.SERVERBOUND) {
         context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
               if (message.mode >= -2 && message.mode <= 2) {
                  if (message.targetMode == 0) {
                     InteractionHand interactionHand = message.hand == 1 ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
                     GemEngravingService.castGravitySelfFromGem(player, interactionHand, message.mode);
                  }
               }
            }
         }).exceptionally(e -> {
            TYPE_MOON_WORLD.LOGGER.error("Failed to handle GemGravitySelfCastMessage", e);
            return null;
         });
      }
   }
}
