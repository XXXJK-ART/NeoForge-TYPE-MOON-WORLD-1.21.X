package net.xxxjk.TYPE_MOON_WORLD.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatDefenseService;

public record ServantCardDodgeMessage(float forward, float strafe) implements CustomPacketPayload {
   public static final Type<ServantCardDodgeMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("typemoonworld", "servant_card_dodge"));
   public static final StreamCodec<RegistryFriendlyByteBuf, ServantCardDodgeMessage> STREAM_CODEC = StreamCodec.of(
      (buf, msg) -> { buf.writeFloat(msg.forward); buf.writeFloat(msg.strafe); },
      buf -> new ServantCardDodgeMessage(buf.readFloat(), buf.readFloat()));
   @Override public Type<ServantCardDodgeMessage> type() { return TYPE; }
   public static void handleData(ServantCardDodgeMessage msg, IPayloadContext context) {
      if (context.flow() != PacketFlow.SERVERBOUND || !Float.isFinite(msg.forward) || !Float.isFinite(msg.strafe)
         || Math.abs(msg.forward) > 1.0F || Math.abs(msg.strafe) > 1.0F) return;
      context.enqueueWork(() -> {
         if (context.player() instanceof ServerPlayer player
            && ServerPacketRateLimiter.allow(player, "servant_card_dodge", 4)) {
            ServantCombatDefenseService.requestDodge(player, msg.forward, msg.strafe);
         }
      });
   }
}
