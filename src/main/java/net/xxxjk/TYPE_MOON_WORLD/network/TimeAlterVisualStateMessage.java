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
import net.xxxjk.TYPE_MOON_WORLD.client.TimeAlterVisualClient;
import org.jetbrains.annotations.NotNull;

public record TimeAlterVisualStateMessage(UUID playerId, boolean active, int mode, int remainingTicks, double actionRate) implements CustomPacketPayload {
   public static final Type<TimeAlterVisualStateMessage> TYPE = new Type<>(
      ResourceLocation.fromNamespaceAndPath("typemoonworld", "time_alter_visual_state")
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, TimeAlterVisualStateMessage> STREAM_CODEC = StreamCodec.of(
      TimeAlterVisualStateMessage::write,
      TimeAlterVisualStateMessage::read
   );

   @Override
   @NotNull
   public Type<TimeAlterVisualStateMessage> type() {
      return TYPE;
   }

   private static void write(RegistryFriendlyByteBuf buffer, TimeAlterVisualStateMessage message) {
      buffer.writeUUID(message.playerId);
      buffer.writeBoolean(message.active);
      buffer.writeVarInt(message.mode);
      buffer.writeVarInt(Math.max(0, message.remainingTicks));
      buffer.writeDouble(message.actionRate);
   }

   private static TimeAlterVisualStateMessage read(RegistryFriendlyByteBuf buffer) {
      return new TimeAlterVisualStateMessage(buffer.readUUID(), buffer.readBoolean(), buffer.readVarInt(), buffer.readVarInt(), buffer.readDouble());
   }

   public static void handleData(TimeAlterVisualStateMessage message, IPayloadContext context) {
      if (context.flow() == PacketFlow.CLIENTBOUND) {
         context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
               TimeAlterVisualClient.applyState(message.playerId, message.active, message.mode, message.remainingTicks, message.actionRate);
            }
         });
      }
   }
}
