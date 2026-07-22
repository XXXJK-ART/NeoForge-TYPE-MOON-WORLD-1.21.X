package net.xxxjk.TYPE_MOON_WORLD.client;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.xxxjk.TYPE_MOON_WORLD.martial.BajiquanMove;

public final class BajiquanPoseClient {
   private static final Map<UUID, PoseState> POSES = new ConcurrentHashMap<>();

   private BajiquanPoseClient() {}

   public static void apply(UUID playerId, BajiquanMove move, int ticks) {
      if (playerId != null && move != null) POSES.put(playerId, new PoseState(move, gameTime() + Math.max(1, ticks)));
   }

   public static BajiquanMove getMove(Player player) {
      if (player == null) return null;
      PoseState state = POSES.get(player.getUUID());
      if (state == null) return null;
      if (state.until < gameTime()) {
         POSES.remove(player.getUUID());
         return null;
      }
      return state.move;
   }

   public static void clear() { POSES.clear(); }

   private static long gameTime() {
      Minecraft minecraft = Minecraft.getInstance();
      return minecraft.level == null ? 0L : minecraft.level.getGameTime();
   }

   private record PoseState(BajiquanMove move, long until) {}
}
