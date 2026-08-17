package net.xxxjk.TYPE_MOON_WORLD.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/** Connection-aware sends for optional client payloads and test/compatibility players. */
public final class ModNetwork {
   private ModNetwork() { }

   public static boolean supports(ServerPlayer player, CustomPacketPayload.Type<?> type) {
      return player != null && !(player instanceof FakePlayer) && type != null
         && player.connection.hasChannel(type);
   }

   public static boolean sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
      if (payload == null || !supports(player, payload.type())) return false;
      PacketDistributor.sendToPlayer(player, payload);
      return true;
   }
}
