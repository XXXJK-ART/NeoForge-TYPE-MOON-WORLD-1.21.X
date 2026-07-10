package net.xxxjk.TYPE_MOON_WORLD.client;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

public final class FirearmPoseClient {
   private static final Map<UUID, Long> POSE_UNTIL = new ConcurrentHashMap<>();

   private FirearmPoseClient() {
   }

   public static void apply(UUID playerId, int ticks) {
      if (playerId == null) {
         return;
      }
      long now = gameTime();
      POSE_UNTIL.put(playerId, now + Math.max(1, ticks));
   }

   public static boolean isPoseActive(Player player) {
      if (player == null) {
         return false;
      }
      Long until = POSE_UNTIL.get(player.getUUID());
      if (until == null) {
         return false;
      }
      if (until < gameTime()) {
         POSE_UNTIL.remove(player.getUUID());
         return false;
      }
      return true;
   }

   private static long gameTime() {
      Minecraft minecraft = Minecraft.getInstance();
      return minecraft.level == null ? 0L : minecraft.level.getGameTime();
   }
}
