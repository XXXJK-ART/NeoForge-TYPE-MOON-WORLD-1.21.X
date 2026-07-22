package net.xxxjk.TYPE_MOON_WORLD.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xxxjk.TYPE_MOON_WORLD.martial.BodyTrainingService;

public record BodyTrainingPointMessage(String stat) implements CustomPacketPayload {
   public static final Type<BodyTrainingPointMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("typemoonworld", "body_training_point"));
   public static final StreamCodec<RegistryFriendlyByteBuf, BodyTrainingPointMessage> STREAM_CODEC = StreamCodec.of(
      (buffer, message) -> buffer.writeUtf(message.stat, 16), buffer -> new BodyTrainingPointMessage(buffer.readUtf(16))
   );
   @Override public Type<BodyTrainingPointMessage> type() { return TYPE; }
   public static void handleData(BodyTrainingPointMessage message, IPayloadContext context) {
      if (context.flow() == PacketFlow.SERVERBOUND && context.player() instanceof ServerPlayer player) {
         context.enqueueWork(() -> BodyTrainingService.allocate(player, message.stat));
      }
   }
}
