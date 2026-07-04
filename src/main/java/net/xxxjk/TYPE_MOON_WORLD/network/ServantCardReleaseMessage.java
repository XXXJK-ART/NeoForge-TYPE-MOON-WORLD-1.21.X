package net.xxxjk.TYPE_MOON_WORLD.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardTransformManager;
import org.jetbrains.annotations.NotNull;

public record ServantCardReleaseMessage() implements CustomPacketPayload {
   public static final Type<ServantCardReleaseMessage> TYPE = new Type<>(
      ResourceLocation.fromNamespaceAndPath("typemoonworld", "servant_card_release")
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, ServantCardReleaseMessage> STREAM_CODEC = StreamCodec.of(
      (buffer, message) -> {},
      buffer -> new ServantCardReleaseMessage()
   );

   @Override
   @NotNull
   public Type<ServantCardReleaseMessage> type() {
      return TYPE;
   }

   public static void handleData(ServantCardReleaseMessage message, IPayloadContext context) {
      context.enqueueWork(() -> {
         if (context.player() instanceof ServerPlayer player) {
            ServantCardTransformManager.release(player, false);
         }
      });
   }
}
