package net.xxxjk.TYPE_MOON_WORLD.network;

import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xxxjk.TYPE_MOON_WORLD.client.HeraclesActionAnimationClient;
import org.jetbrains.annotations.NotNull;

public record HeraclesActionAnimationMessage(UUID playerId, int actionId) implements CustomPacketPayload {
   public static final Type<HeraclesActionAnimationMessage> TYPE = new Type<>(
      ResourceLocation.fromNamespaceAndPath("typemoonworld", "heracles_action_animation")
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, HeraclesActionAnimationMessage> STREAM_CODEC = StreamCodec.of(
      HeraclesActionAnimationMessage::write,
      HeraclesActionAnimationMessage::read
   );

   @Override
   @NotNull
   public Type<HeraclesActionAnimationMessage> type() {
      return TYPE;
   }

   private static void write(RegistryFriendlyByteBuf buffer, HeraclesActionAnimationMessage message) {
      buffer.writeUUID(message.playerId);
      buffer.writeVarInt(message.actionId);
   }

   private static HeraclesActionAnimationMessage read(RegistryFriendlyByteBuf buffer) {
      return new HeraclesActionAnimationMessage(buffer.readUUID(), buffer.readVarInt());
   }

   public static void handleData(HeraclesActionAnimationMessage message, IPayloadContext context) {
      if (context.flow() != PacketFlow.CLIENTBOUND) return;
      context.enqueueWork(() -> {
         if (FMLEnvironment.dist == Dist.CLIENT) {
            HeraclesActionAnimationClient.play(message.playerId, message.actionId);
         }
      });
   }
}
