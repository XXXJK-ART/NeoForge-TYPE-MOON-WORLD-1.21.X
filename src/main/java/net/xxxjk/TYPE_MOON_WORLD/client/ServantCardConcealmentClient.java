package net.xxxjk.TYPE_MOON_WORLD.client;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.minecraft.client.Minecraft;
import net.xxxjk.TYPE_MOON_WORLD.magic.PlayerMagicSelectionService;

public final class ServantCardConcealmentClient {
   private ServantCardConcealmentClient() {
   }

   public static boolean isPerfectlyConcealed(Player player) {
      if (player == null) {
         return false;
      }
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      Player observer = Minecraft.getInstance().player;
      if (observer != null) {
         TypeMoonWorldModVariables.PlayerVariables observerVars = observer.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         if (observerVars.servant_card_transformed && "li_shuwen".equals(observerVars.servant_card_id)) return false;
      }
      boolean servantConcealment = vars.servant_card_transformed
         && ("cursed_arm_hassan".equals(vars.servant_card_id) || "sasaki_kojiro".equals(vars.servant_card_id) || "li_shuwen".equals(vars.servant_card_id));
      boolean bajiquanCircle = CircleRealmClient.isActive(player);
      if (!servantConcealment && !bajiquanCircle) {
         return false;
      }
      MobEffectInstance invisibility = player.getEffect(MobEffects.INVISIBILITY);
      return invisibility != null && !invisibility.isVisible();
   }
}
