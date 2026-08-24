package net.xxxjk.TYPE_MOON_WORLD.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantCommandMode;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.personality.CombatDisposition;
import net.xxxjk.TYPE_MOON_WORLD.servant.personality.SocialDisposition;
import org.jetbrains.annotations.NotNull;

public record ServantCommandMessage(int entityId, int action) implements CustomPacketPayload {
   public static final Type<ServantCommandMessage> TYPE = new Type<>(
      ResourceLocation.fromNamespaceAndPath("typemoonworld", "servant_command")
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, ServantCommandMessage> STREAM_CODEC = StreamCodec.of(
      (buffer, message) -> {
         buffer.writeVarInt(message.entityId);
         buffer.writeVarInt(message.action);
      },
      buffer -> new ServantCommandMessage(buffer.readVarInt(), buffer.readVarInt())
   );

   @Override
   @NotNull
   public Type<ServantCommandMessage> type() {
      return TYPE;
   }

   public static void handleData(ServantCommandMessage message, IPayloadContext context) {
      if (context.flow() != PacketFlow.SERVERBOUND || message.entityId < 0 || message.action < 0 || message.action > 5) return;
      context.enqueueWork(() -> {
         if (!(context.player() instanceof ServerPlayer player)
            || !ServerPacketRateLimiter.allow(player, "servant_command", 8)) {
            return;
         }
         Entity target = player.level().getEntity(message.entityId);
         if (!(target instanceof ServantEntity servant)
            || !servant.isAlive()
            || !servant.isBoundTo(player)
            || servant.distanceToSqr(player) > 36.0) {
            return;
         }

         switch (message.action) {
            case 0 -> servant.setCommandMode(ServantCommandMode.FOLLOW);
            case 1 -> servant.setCommandMode(ServantCommandMode.GUARD);
            case 2 -> servant.setCommandMode(ServantCommandMode.STAY);
            case 3 -> servant.toggleMasterNoblePhantasmPermission();
            case 4 -> servant.setCombatDisposition(next(CombatDisposition.values(), servant.getCombatDisposition()));
            case 5 -> servant.setSocialDisposition(next(SocialDisposition.values(), servant.getSocialDisposition()));
            default -> {
            }
         }
      });
   }

   private static <T> T next(T[] values, T current) {
      int index = 0;
      for (int i = 0; i < values.length; i++) {
         if (values[i].equals(current)) {
            index = i;
            break;
         }
      }
      return values[(index + 1) % values.length];
   }
}
