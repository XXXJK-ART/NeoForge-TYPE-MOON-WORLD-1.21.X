package net.xxxjk.TYPE_MOON_WORLD.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xxxjk.TYPE_MOON_WORLD.client.ServantCardDefenseClientState;

public record ServantCardDefenseStateMessage(double stamina, double staminaMax, double poise, double poiseMax,
                                             boolean guarding, long dodgeCooldownEnd, long guardBrokenEnd,
                                             String result, long resultUntil) implements CustomPacketPayload {
   public static final Type<ServantCardDefenseStateMessage> TYPE = new Type<>(
      ResourceLocation.fromNamespaceAndPath("typemoonworld", "servant_card_defense_state"));
   public static final StreamCodec<RegistryFriendlyByteBuf, ServantCardDefenseStateMessage> STREAM_CODEC = StreamCodec.of(
      (buf, msg) -> {
         buf.writeDouble(msg.stamina); buf.writeDouble(msg.staminaMax);
         buf.writeDouble(msg.poise); buf.writeDouble(msg.poiseMax);
         buf.writeBoolean(msg.guarding); buf.writeVarLong(msg.dodgeCooldownEnd);
         buf.writeVarLong(msg.guardBrokenEnd); buf.writeUtf(msg.result == null ? "" : msg.result, 32);
         buf.writeVarLong(msg.resultUntil);
      },
      buf -> new ServantCardDefenseStateMessage(buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readDouble(),
         buf.readBoolean(), buf.readVarLong(), buf.readVarLong(), buf.readUtf(32), buf.readVarLong()));

   @Override public Type<ServantCardDefenseStateMessage> type() { return TYPE; }

   public static void handleData(ServantCardDefenseStateMessage msg, IPayloadContext context) {
      if (context.flow() != PacketFlow.CLIENTBOUND) return;
      context.enqueueWork(() -> ServantCardDefenseClientState.apply(msg.stamina, msg.staminaMax, msg.poise, msg.poiseMax,
         msg.guarding, msg.dodgeCooldownEnd, msg.guardBrokenEnd, msg.result, msg.resultUntil));
   }
}
