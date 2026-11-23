package net.xxxjk.TYPE_MOON_WORLD.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xxxjk.TYPE_MOON_WORLD.client.PaleRiderGrayscaleClient;

public record PaleRiderGrayscaleMessage(boolean active) implements CustomPacketPayload {
   public static final Type<PaleRiderGrayscaleMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("typemoonworld", "pale_rider_grayscale"));
   public static final StreamCodec<RegistryFriendlyByteBuf, PaleRiderGrayscaleMessage> STREAM_CODEC = StreamCodec.of(
      (buffer, message) -> buffer.writeBoolean(message.active),
      buffer -> new PaleRiderGrayscaleMessage(buffer.readBoolean())
   );

   @Override public Type<PaleRiderGrayscaleMessage> type() { return TYPE; }

   public static void handleData(PaleRiderGrayscaleMessage message, IPayloadContext context) {
      context.enqueueWork(() -> {
         if (FMLEnvironment.dist == Dist.CLIENT) PaleRiderGrayscaleClient.setActive(message.active);
      });
   }
}
