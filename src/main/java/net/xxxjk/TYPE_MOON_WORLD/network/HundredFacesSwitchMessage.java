package net.xxxjk.TYPE_MOON_WORLD.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardHundredFacesHassanSkills;

public record HundredFacesSwitchMessage(int entityId) implements CustomPacketPayload {
   public static final Type<HundredFacesSwitchMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("typemoonworld", "hundred_faces_switch"));
   public static final StreamCodec<RegistryFriendlyByteBuf, HundredFacesSwitchMessage> STREAM_CODEC = StreamCodec.of(
      (buf, msg) -> buf.writeVarInt(msg.entityId), buf -> new HundredFacesSwitchMessage(buf.readVarInt()));

   @Override
   public Type<HundredFacesSwitchMessage> type() {
      return TYPE;
   }

   public static void handleData(HundredFacesSwitchMessage msg, IPayloadContext ctx) {
      if (ctx.flow() != PacketFlow.SERVERBOUND || msg.entityId < 0) return;
      ctx.enqueueWork(() -> {
         if (ctx.player() instanceof ServerPlayer player && ServerPacketRateLimiter.allow(player, "hundred_faces_switch", 4)) {
            ServantCardHundredFacesHassanSkills.switchTo(player, msg.entityId);
         }
      });
   }
}
