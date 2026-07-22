package net.xxxjk.TYPE_MOON_WORLD.client;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.entity.player.Player;

public final class CircleRealmClient {
   private static final Set<UUID> ACTIVE = ConcurrentHashMap.newKeySet();

   private CircleRealmClient() {}

   public static void set(UUID playerId, boolean active) {
      if (playerId == null) return;
      if (active) ACTIVE.add(playerId); else ACTIVE.remove(playerId);
   }

   public static boolean isActive(Player player) { return player != null && ACTIVE.contains(player.getUUID()); }

   public static void clear() { ACTIVE.clear(); }
}
