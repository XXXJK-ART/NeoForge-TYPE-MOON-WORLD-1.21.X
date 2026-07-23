package net.xxxjk.TYPE_MOON_WORLD.network;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xxxjk.TYPE_MOON_WORLD.client.ClientPacketHandler;

public record PaleRiderOpenScreenMessage(int kind, List<Target> targets) implements CustomPacketPayload {
   public static final Type<PaleRiderOpenScreenMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("typemoonworld", "pale_rider_open_screen"));
   public static final StreamCodec<RegistryFriendlyByteBuf, PaleRiderOpenScreenMessage> STREAM_CODEC = StreamCodec.of((buf, msg) -> {
      buf.writeVarInt(msg.kind);
      buf.writeVarInt(msg.targets.size());
      for (Target target : msg.targets) {
         buf.writeVarInt(target.entityId);
         buf.writeInt(target.x);
         buf.writeInt(target.z);
         buf.writeUtf(target.name, 64);
      }
   }, buf -> {
      int kind = buf.readVarInt();
      int count = Math.min(256, buf.readVarInt());
      List<Target> targets = new ArrayList<>();
      for (int i = 0; i < count; i++) targets.add(new Target(buf.readVarInt(), buf.readInt(), buf.readInt(), buf.readUtf(64)));
      return new PaleRiderOpenScreenMessage(kind, targets);
   });
   @Override public Type<PaleRiderOpenScreenMessage> type() { return TYPE; }
   public static void handleData(PaleRiderOpenScreenMessage msg, IPayloadContext ctx) {
      ctx.enqueueWork(() -> ClientPacketHandler.openPaleRiderScreen(msg.kind, msg.targets));
   }

   public record Target(int entityId, int x, int z, String name) {}
}
