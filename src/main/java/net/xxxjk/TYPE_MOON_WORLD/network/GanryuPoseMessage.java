package net.xxxjk.TYPE_MOON_WORLD.network;

import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xxxjk.TYPE_MOON_WORLD.client.GanryuPoseClient;
import net.xxxjk.TYPE_MOON_WORLD.martial.GanryuMove;

public record GanryuPoseMessage(UUID playerId, int move, int ticks) implements CustomPacketPayload {
   public static final Type<GanryuPoseMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("typemoonworld", "ganryu_pose"));
   public static final StreamCodec<RegistryFriendlyByteBuf, GanryuPoseMessage> STREAM_CODEC = StreamCodec.of(
      (buffer, message) -> {
         buffer.writeUUID(message.playerId);
         buffer.writeVarInt(message.move);
         buffer.writeVarInt(message.ticks);
      },
      buffer -> new GanryuPoseMessage(buffer.readUUID(), buffer.readVarInt(), buffer.readVarInt())
   );

   public GanryuPoseMessage(UUID playerId, GanryuMove move, int ticks) {
      this(playerId, move.ordinal(), ticks);
   }

   @Override public Type<GanryuPoseMessage> type() { return TYPE; }

   public static void handleData(GanryuPoseMessage message, IPayloadContext context) {
      if (context.flow() != PacketFlow.CLIENTBOUND) return;
      context.enqueueWork(() -> {
         if (FMLEnvironment.dist == Dist.CLIENT && message.move >= 0 && message.move < GanryuMove.values().length) {
            GanryuPoseClient.apply(message.playerId, GanryuMove.values()[message.move], message.ticks);
         }
      });
   }
}
