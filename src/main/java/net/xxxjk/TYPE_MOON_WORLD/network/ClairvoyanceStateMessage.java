package net.xxxjk.TYPE_MOON_WORLD.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xxxjk.TYPE_MOON_WORLD.client.ClairvoyanceClientState;

public record ClairvoyanceStateMessage(boolean toggle, int maxZoom) implements CustomPacketPayload {
   public static final Type<ClairvoyanceStateMessage> TYPE = new Type<>(
      ResourceLocation.fromNamespaceAndPath("typemoonworld", "clairvoyance_state")
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, ClairvoyanceStateMessage> STREAM_CODEC = StreamCodec.of(
      (buffer, message) -> {
         buffer.writeBoolean(message.toggle);
         buffer.writeVarInt(message.maxZoom);
      },
      buffer -> new ClairvoyanceStateMessage(buffer.readBoolean(), buffer.readVarInt())
   );

   @Override
   public Type<ClairvoyanceStateMessage> type() {
      return TYPE;
   }

   public static void handleData(ClairvoyanceStateMessage message, IPayloadContext context) {
      if (context.flow() == PacketFlow.CLIENTBOUND) {
         context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
               ClairvoyanceClientState.apply(message.toggle, message.maxZoom);
            }
         });
      }
   }
}
