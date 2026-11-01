package net.xxxjk.TYPE_MOON_WORLD.client;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

public final class ServantCardConcealmentClient {
   private ServantCardConcealmentClient() {
   }

   public static boolean isPerfectlyConcealed(Player player) {
      if (player == null) {
         return false;
      }
      return ObserverConcealmentClient.isPerfectlyConcealed(player.getUUID()) || player.isInvisible();
   }

   public static boolean isPerfectlyConcealed(Entity entity) {
      if (entity == null) return false;
      if (entity instanceof Player player) return isPerfectlyConcealed(player);
      return ObserverConcealmentClient.isPerfectlyConcealed(entity.getUUID()) || entity.isInvisible();
   }

   public static boolean isShadowHassanMaskOnly(Player player) {
      if (player == null) return false;
      var vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      return vars.servant_card_transformed && "shadow_hassan".equals(vars.servant_card_id);
   }

   public static boolean isShadowHassanStealthActive(Player player) {
      return isShadowHassanMaskOnly(player) && ObserverConcealmentClient.isConcealed(player.getUUID());
   }
}
