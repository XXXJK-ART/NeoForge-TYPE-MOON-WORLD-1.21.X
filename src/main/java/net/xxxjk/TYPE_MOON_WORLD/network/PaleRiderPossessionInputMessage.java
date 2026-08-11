package net.xxxjk.TYPE_MOON_WORLD.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardPaleRiderSkills;
import org.jetbrains.annotations.NotNull;

public record PaleRiderPossessionInputMessage(float forward, float strafe, float vertical, float yaw, float pitch) implements CustomPacketPayload {
   public static final Type<PaleRiderPossessionInputMessage> TYPE = new Type<>(
      ResourceLocation.fromNamespaceAndPath("typemoonworld", "pale_rider_possession_input")
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, PaleRiderPossessionInputMessage> STREAM_CODEC = StreamCodec.of(
      (buf, msg) -> {
         buf.writeByte(encodeUnit(msg.forward));
         buf.writeByte(encodeUnit(msg.strafe));
         buf.writeByte(encodeUnit(msg.vertical));
         buf.writeFloat(msg.yaw);
         buf.writeFloat(msg.pitch);
      },
      buf -> new PaleRiderPossessionInputMessage(decodeUnit(buf.readByte()), decodeUnit(buf.readByte()), decodeUnit(buf.readByte()), buf.readFloat(), buf.readFloat())
   );

   @Override
   @NotNull
   public Type<PaleRiderPossessionInputMessage> type() {
      return TYPE;
   }

   public static void handleData(PaleRiderPossessionInputMessage msg, IPayloadContext ctx) {
      if (ctx.flow() != PacketFlow.SERVERBOUND
         || !Float.isFinite(msg.forward) || !Float.isFinite(msg.strafe) || !Float.isFinite(msg.vertical)
         || !Float.isFinite(msg.yaw) || !Float.isFinite(msg.pitch)) {
         return;
      }
      ctx.enqueueWork(() -> {
         if (ctx.player() instanceof ServerPlayer player && ServerPacketRateLimiter.allow(player, "pale_rider_possession_input", 2)) {
            ServantCardPaleRiderSkills.applyPossessionInput(player, msg);
         }
      });
   }

   private static byte encodeUnit(float value) {
      return (byte)Math.round(Math.max(-1.0F, Math.min(1.0F, value)) * 127.0F);
   }

   private static float decodeUnit(byte value) {
      return value / 127.0F;
   }
}
