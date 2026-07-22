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
import net.xxxjk.TYPE_MOON_WORLD.client.BajiquanPoseClient;
import net.xxxjk.TYPE_MOON_WORLD.martial.BajiquanMove;

public record BajiquanPoseMessage(UUID playerId, int move, int ticks) implements CustomPacketPayload {
   public static final Type<BajiquanPoseMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("typemoonworld", "bajiquan_pose"));
   public static final StreamCodec<RegistryFriendlyByteBuf, BajiquanPoseMessage> STREAM_CODEC = StreamCodec.of(
      (buffer, message) -> {
         buffer.writeUUID(message.playerId);
         buffer.writeVarInt(message.move);
         buffer.writeVarInt(message.ticks);
      },
      buffer -> new BajiquanPoseMessage(buffer.readUUID(), buffer.readVarInt(), buffer.readVarInt())
   );

   public BajiquanPoseMessage(UUID playerId, BajiquanMove move, int ticks) {
      this(playerId, move.ordinal(), ticks);
   }

   @Override public Type<BajiquanPoseMessage> type() { return TYPE; }

   public static void handleData(BajiquanPoseMessage message, IPayloadContext context) {
      if (context.flow() != PacketFlow.CLIENTBOUND) return;
      context.enqueueWork(() -> {
         if (FMLEnvironment.dist == Dist.CLIENT && message.move >= 0 && message.move < BajiquanMove.values().length) {
            BajiquanPoseClient.apply(message.playerId, BajiquanMove.values()[message.move], message.ticks);
         }
      });
   }
}
