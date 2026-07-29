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
import net.xxxjk.TYPE_MOON_WORLD.client.ClientPacketHandler;
import org.jetbrains.annotations.NotNull;

public record ConcealmentStateMessage(UUID entityId, boolean concealed) implements CustomPacketPayload {
   public static final Type<ConcealmentStateMessage> TYPE = new Type<>(
      ResourceLocation.fromNamespaceAndPath("typemoonworld", "concealment_state"));
   public static final StreamCodec<RegistryFriendlyByteBuf, ConcealmentStateMessage> STREAM_CODEC = StreamCodec.of(
      (buffer, message) -> {
         buffer.writeUUID(message.entityId());
         buffer.writeBoolean(message.concealed());
      },
      buffer -> new ConcealmentStateMessage(buffer.readUUID(), buffer.readBoolean()));

   @Override
   @NotNull
   public Type<ConcealmentStateMessage> type() {
      return TYPE;
   }

   public static void handleData(ConcealmentStateMessage message, IPayloadContext context) {
      if (context.flow() != PacketFlow.CLIENTBOUND) return;
      context.enqueueWork(() -> {
         if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientPacketHandler.handleConcealmentState(message.entityId(), message.concealed());
         }
      });
   }
}
