package net.xxxjk.TYPE_MOON_WORLD.client;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

public final class ServantCardConcealmentClient {
   private ServantCardConcealmentClient() {
   }

   public static boolean isPerfectlyConcealed(Player player) {
      if (player == null) {
         return false;
      }
      MobEffectInstance invisibility = player.getEffect(MobEffects.INVISIBILITY);
      return invisibility != null && !invisibility.isVisible();
   }
}
