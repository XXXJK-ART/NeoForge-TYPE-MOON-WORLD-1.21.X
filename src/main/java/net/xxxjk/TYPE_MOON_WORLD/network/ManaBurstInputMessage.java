package net.xxxjk.TYPE_MOON_WORLD.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xxxjk.TYPE_MOON_WORLD.magic.basic.ManaBurstService;
import org.jetbrains.annotations.NotNull;

public record ManaBurstInputMessage(float forward, float strafe, boolean jump, boolean sneak) implements CustomPacketPayload {
   public static final Type<ManaBurstInputMessage> TYPE = new Type<>(
      ResourceLocation.fromNamespaceAndPath("typemoonworld", "mana_burst_input")
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, ManaBurstInputMessage> STREAM_CODEC = StreamCodec.of(
      (buffer, message) -> {
         buffer.writeByte(encodeUnit(message.forward));
         buffer.writeByte(encodeUnit(message.strafe));
         buffer.writeBoolean(message.jump);
         buffer.writeBoolean(message.sneak);
      },
      buffer -> new ManaBurstInputMessage(decodeUnit(buffer.readByte()), decodeUnit(buffer.readByte()), buffer.readBoolean(), buffer.readBoolean())
   );

   @Override
   @NotNull
   public Type<ManaBurstInputMessage> type() {
      return TYPE;
   }

   public static void handleData(ManaBurstInputMessage message, IPayloadContext context) {
      if (context.flow() != PacketFlow.SERVERBOUND || !Float.isFinite(message.forward) || !Float.isFinite(message.strafe)) {
         return;
      }
      context.enqueueWork(() -> {
         if (context.player() instanceof ServerPlayer player && ServerPacketRateLimiter.allow(player, "mana_burst_input", 1)) {
            ManaBurstService.setInput(player, message.forward, message.strafe, message.jump, message.sneak);
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
