package net.xxxjk.TYPE_MOON_WORLD.client;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ShadowHassanEntity;

public final class ServantCardConcealmentClient {
   private ServantCardConcealmentClient() {
   }

   public static boolean isPerfectlyConcealed(Player player) {
      if (player == null) {
         return false;
      }
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      boolean bajiquanCircle = CircleRealmClient.isActive(player);
      if (!vars.servant_card_transformed && !bajiquanCircle) {
         return false;
      }
      if (vars.servant_card_transformed && "shadow_hassan".equals(vars.servant_card_id)) {
         MobEffectInstance invisibility = player.getEffect(MobEffects.INVISIBILITY);
         return invisibility != null && invisibility.getAmplifier() >= 1;
      }
      return player.isInvisible();
   }

   public static boolean isPerfectlyConcealed(Entity entity) {
      if (entity instanceof Player player) return isPerfectlyConcealed(player);
      if (entity instanceof ShadowHassanEntity hassan) return hassan.isPresenceConcealed();
      return entity instanceof ServantEntity servant && servant.isInvisible();
   }
}
