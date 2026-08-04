package net.xxxjk.TYPE_MOON_WORLD.client;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public final class ServantCardConcealmentClient {
   private ServantCardConcealmentClient() {
   }

   public static boolean isPerfectlyConcealed(Player player) {
      if (player == null) {
         return false;
      }
      return ObserverConcealmentClient.isConcealed(player.getUUID()) || player.isInvisible();
   }

   public static boolean isPerfectlyConcealed(Entity entity) {
      if (entity == null) return false;
      if (entity instanceof Player player) return isPerfectlyConcealed(player);
      return ObserverConcealmentClient.isConcealed(entity.getUUID()) || entity.isInvisible();
   }
}
