package net.xxxjk.TYPE_MOON_WORLD.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardPaleRiderSkills;

public record PaleRiderSelectMessage(int entityId) implements CustomPacketPayload {
   public static final Type<PaleRiderSelectMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("typemoonworld", "pale_rider_select"));
   public static final StreamCodec<RegistryFriendlyByteBuf, PaleRiderSelectMessage> STREAM_CODEC = StreamCodec.of(
      (buf, msg) -> buf.writeVarInt(msg.entityId), buf -> new PaleRiderSelectMessage(buf.readVarInt()));
   @Override public Type<PaleRiderSelectMessage> type() { return TYPE; }
   public static void handleData(PaleRiderSelectMessage msg, IPayloadContext ctx) {
      if (ctx.flow() != PacketFlow.SERVERBOUND || msg.entityId < 0) return;
      ctx.enqueueWork(() -> {
         if (ctx.player() instanceof ServerPlayer player
            && ServerPacketRateLimiter.allow(player, "pale_rider_select", 4)
            && ServantCardPaleRiderSkills.possess(player, msg.entityId)) {
            ServantCardPaleRiderSkills.sendIfSupported(player,
               new PaleRiderOpenScreenMessage(4, java.util.List.of(new PaleRiderOpenScreenMessage.Target(msg.entityId, 0, 0, ""))));
         }
      });
   }
}
