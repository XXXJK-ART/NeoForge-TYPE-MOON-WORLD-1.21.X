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
import net.xxxjk.TYPE_MOON_WORLD.client.CircleRealmClient;

public record CircleRealmStateMessage(UUID playerId, boolean active) implements CustomPacketPayload {
   public static final Type<CircleRealmStateMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("typemoonworld", "circle_realm_state"));
   public static final StreamCodec<RegistryFriendlyByteBuf, CircleRealmStateMessage> STREAM_CODEC = StreamCodec.of(
      (buffer, message) -> { buffer.writeUUID(message.playerId); buffer.writeBoolean(message.active); },
      buffer -> new CircleRealmStateMessage(buffer.readUUID(), buffer.readBoolean())
   );

   @Override public Type<CircleRealmStateMessage> type() { return TYPE; }

   public static void handleData(CircleRealmStateMessage message, IPayloadContext context) {
      if (context.flow() != PacketFlow.CLIENTBOUND) return;
      context.enqueueWork(() -> {
         if (FMLEnvironment.dist == Dist.CLIENT) CircleRealmClient.set(message.playerId, message.active);
      });
   }
}
