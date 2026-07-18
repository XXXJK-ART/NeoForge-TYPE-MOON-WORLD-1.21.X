package net.xxxjk.TYPE_MOON_WORLD.client;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

public final class ServantCardConcealmentClient {
   private ServantCardConcealmentClient() {
   }

   public static boolean isPerfectlyConcealed(Player player) {
      if (player == null) {
         return false;
      }
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (!vars.servant_card_transformed
         || (!"cursed_arm_hassan".equals(vars.servant_card_id) && !"sasaki_kojiro".equals(vars.servant_card_id))) {
         return false;
      }
      MobEffectInstance invisibility = player.getEffect(MobEffects.INVISIBILITY);
      return invisibility != null && !invisibility.isVisible();
   }
}
