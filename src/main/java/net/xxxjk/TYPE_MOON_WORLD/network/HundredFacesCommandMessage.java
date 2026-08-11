package net.xxxjk.TYPE_MOON_WORLD.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardHundredFacesHassanSkills;

public record HundredFacesCommandMessage(int scope, int entityId, int command) implements CustomPacketPayload {
   public static final Type<HundredFacesCommandMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("typemoonworld", "hundred_faces_command"));
   public static final StreamCodec<RegistryFriendlyByteBuf, HundredFacesCommandMessage> STREAM_CODEC = StreamCodec.of((buf, msg) -> {
      buf.writeVarInt(msg.scope);
      buf.writeVarInt(msg.entityId);
      buf.writeVarInt(msg.command);
   }, buf -> new HundredFacesCommandMessage(buf.readVarInt(), buf.readVarInt(), buf.readVarInt()));

   @Override
   public Type<HundredFacesCommandMessage> type() {
      return TYPE;
   }

   public static void handleData(HundredFacesCommandMessage msg, IPayloadContext ctx) {
      if (ctx.flow() != PacketFlow.SERVERBOUND) return;
      ctx.enqueueWork(() -> {
         if (ctx.player() instanceof ServerPlayer player && ServerPacketRateLimiter.allow(player, "hundred_faces_command", 8)) {
            if (msg.scope == 0) ServantCardHundredFacesHassanSkills.setGlobalCommand(player, msg.command);
            else ServantCardHundredFacesHassanSkills.setPersonaCommand(player, msg.entityId, msg.command);
         }
      });
   }
}
