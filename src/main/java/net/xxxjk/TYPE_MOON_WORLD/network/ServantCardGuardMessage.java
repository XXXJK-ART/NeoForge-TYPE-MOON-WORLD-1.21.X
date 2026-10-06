package net.xxxjk.TYPE_MOON_WORLD.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatDefenseService;

public record ServantCardGuardMessage(boolean pressed) implements CustomPacketPayload {
   public static final Type<ServantCardGuardMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("typemoonworld", "servant_card_guard"));
   public static final StreamCodec<RegistryFriendlyByteBuf, ServantCardGuardMessage> STREAM_CODEC = StreamCodec.of(
      (buf, msg) -> buf.writeBoolean(msg.pressed), buf -> new ServantCardGuardMessage(buf.readBoolean()));
   @Override public Type<ServantCardGuardMessage> type() { return TYPE; }
   public static void handleData(ServantCardGuardMessage msg, IPayloadContext context) {
      if (context.flow() != PacketFlow.SERVERBOUND) return;
      context.enqueueWork(() -> {
         if (context.player() instanceof ServerPlayer player
            && (!msg.pressed || ServerPacketRateLimiter.allow(player, "servant_card_guard", 1))) {
            ServantCombatDefenseService.setGuarding(player, msg.pressed);
         }
      });
   }
}
