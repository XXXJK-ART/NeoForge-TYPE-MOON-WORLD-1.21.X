package net.xxxjk.TYPE_MOON_WORLD.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xxxjk.TYPE_MOON_WORLD.entity.ZhaoYunHakuryuEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.zhaoyun.ZhaoYunHakuryuRideService;
import org.jetbrains.annotations.NotNull;

public record HakuryuRideMessage(int mountEntityId) implements CustomPacketPayload {
   public static final Type<HakuryuRideMessage> TYPE = new Type<>(
      ResourceLocation.fromNamespaceAndPath("typemoonworld", "hakuryu_ride")
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, HakuryuRideMessage> STREAM_CODEC = StreamCodec.of(
      (buffer, message) -> buffer.writeInt(message.mountEntityId),
      buffer -> new HakuryuRideMessage(buffer.readInt())
   );

   @Override
   @NotNull
   public Type<HakuryuRideMessage> type() {
      return TYPE;
   }

   public static void handleData(HakuryuRideMessage message, IPayloadContext context) {
      if (context.flow() != PacketFlow.SERVERBOUND || message.mountEntityId < 0) return;
      context.enqueueWork(() -> {
         if (context.player() instanceof ServerPlayer player
            && ServerPacketRateLimiter.allow(player, "hakuryu_ride", 2)) {
            Entity entity = player.level().getEntity(message.mountEntityId);
            if (entity instanceof ZhaoYunHakuryuEntity mount) {
               ZhaoYunHakuryuRideService.tryToggle(player, mount);
            }
         }
      });
   }
}
