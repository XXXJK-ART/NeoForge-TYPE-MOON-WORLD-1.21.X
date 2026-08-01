package net.xxxjk.TYPE_MOON_WORLD.network;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerPlayer;

public final class ServerPacketRateLimiter {
   private static final Map<ServerPlayer, Map<String, Long>> LAST_REQUEST_TICKS = new WeakHashMap<>();

   private ServerPacketRateLimiter() {
   }

   public static boolean allow(ServerPlayer player, String action, int intervalTicks) {
      if (player == null || action == null || action.isBlank()) return false;
      long now = player.level().getGameTime();
      Map<String, Long> playerRequests = LAST_REQUEST_TICKS.computeIfAbsent(player, ignored -> new HashMap<>());
      Long previous = playerRequests.get(action);
      if (previous != null && now >= previous && now - previous < Math.max(1, intervalTicks)) return false;
      playerRequests.put(action, now);
      return true;
   }
}
