package net.xxxjk.TYPE_MOON_WORLD.network;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xxxjk.TYPE_MOON_WORLD.client.CombatThreatClientState;

public record CombatThreatStateMessage(List<ThreatEntry> threats) implements CustomPacketPayload {
   public record ThreatEntry(String actionId, float dx, float dz, int danger, int ticksToImpact, boolean blockable, boolean dodgeable) { }
   public static final Type<CombatThreatStateMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("typemoonworld", "combat_threat_state"));
   public static final StreamCodec<RegistryFriendlyByteBuf, CombatThreatStateMessage> STREAM_CODEC = StreamCodec.of(
      (buf, msg) -> {
         List<ThreatEntry> list = msg.threats == null ? List.of() : msg.threats;
         buf.writeVarInt(Math.min(8, list.size()));
         for (int i = 0; i < Math.min(8, list.size()); i++) {
            ThreatEntry e = list.get(i);
            buf.writeUtf(e.actionId(), 96); buf.writeFloat(e.dx()); buf.writeFloat(e.dz());
            buf.writeVarInt(e.danger()); buf.writeVarInt(e.ticksToImpact());
            buf.writeBoolean(e.blockable()); buf.writeBoolean(e.dodgeable());
         }
      },
      buf -> {
         int count = Math.min(8, buf.readVarInt());
         List<ThreatEntry> list = new ArrayList<>(count);
         for (int i = 0; i < count; i++) list.add(new ThreatEntry(buf.readUtf(96), buf.readFloat(), buf.readFloat(),
            buf.readVarInt(), buf.readVarInt(), buf.readBoolean(), buf.readBoolean()));
         return new CombatThreatStateMessage(list);
      });
   @Override public Type<CombatThreatStateMessage> type() { return TYPE; }
   public static void handleData(CombatThreatStateMessage msg, IPayloadContext context) {
      if (context.flow() != PacketFlow.CLIENTBOUND) return;
      context.enqueueWork(() -> CombatThreatClientState.apply(msg.threats.stream()
         .map(e -> new CombatThreatClientState.Entry(e.actionId(), e.dx(), e.dz(), e.danger(), e.ticksToImpact(), e.blockable(), e.dodgeable())).toList()));
   }
}
