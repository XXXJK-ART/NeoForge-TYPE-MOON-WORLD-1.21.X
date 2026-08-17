package net.xxxjk.TYPE_MOON_WORLD.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardHundredFacesHassanSkills;

public record HundredFacesSummonMessage(int requested) implements CustomPacketPayload {
   public static final Type<HundredFacesSummonMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("typemoonworld", "hundred_faces_summon"));
   public static final StreamCodec<RegistryFriendlyByteBuf, HundredFacesSummonMessage> STREAM_CODEC = StreamCodec.of(
      (buf, msg) -> buf.writeVarInt(msg.requested), buf -> new HundredFacesSummonMessage(buf.readVarInt()));

   @Override
   public Type<HundredFacesSummonMessage> type() {
      return TYPE;
   }

   public static void handleData(HundredFacesSummonMessage msg, IPayloadContext ctx) {
      if (ctx.flow() != PacketFlow.SERVERBOUND) return;
      ctx.enqueueWork(() -> {
         if (ctx.player() instanceof ServerPlayer player && ServerPacketRateLimiter.allow(player, "hundred_faces_summon", 4)) {
            ServantCardHundredFacesHassanSkills.summon(player, Math.max(1, Math.min(80, msg.requested)));
         }
      });
   }
}
