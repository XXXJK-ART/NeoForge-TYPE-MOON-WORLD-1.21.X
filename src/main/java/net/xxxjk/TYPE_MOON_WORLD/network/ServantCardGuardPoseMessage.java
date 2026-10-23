package net.xxxjk.TYPE_MOON_WORLD.network;

import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xxxjk.TYPE_MOON_WORLD.client.ServantGuardPoseClient;

public record ServantCardGuardPoseMessage(UUID playerId, boolean guarding) implements CustomPacketPayload {
   public static final Type<ServantCardGuardPoseMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("typemoonworld", "servant_card_guard_pose"));
   public static final StreamCodec<RegistryFriendlyByteBuf, ServantCardGuardPoseMessage> STREAM_CODEC = StreamCodec.of(
      (buf, msg) -> { buf.writeUUID(msg.playerId); buf.writeBoolean(msg.guarding); },
      buf -> new ServantCardGuardPoseMessage(buf.readUUID(), buf.readBoolean()));

   @Override public Type<ServantCardGuardPoseMessage> type() { return TYPE; }

   public static void handleData(ServantCardGuardPoseMessage msg, IPayloadContext context) {
      if (context.flow() != PacketFlow.CLIENTBOUND) return;
      context.enqueueWork(() -> ServantGuardPoseClient.apply(msg.playerId, msg.guarding));
   }
}
