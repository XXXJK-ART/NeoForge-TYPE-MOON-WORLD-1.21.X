package net.xxxjk.TYPE_MOON_WORLD.servant.card;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.SasakiKojiroEntity;

public final class SowaExpertiseHelper {
   private SowaExpertiseHelper() {
   }

   public static boolean rollBypass(DamageSource source) {
      Entity attacker = source.getEntity();
      if (attacker instanceof SasakiKojiroEntity sasaki) {
         return sasaki.getRandom().nextFloat() < 0.15F;
      }
      if (attacker instanceof ServerPlayer player) {
         TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         return vars.servant_card_transformed && "sasaki_kojiro".equals(vars.servant_card_id) && player.getRandom().nextFloat() < 0.15F;
      }
      return false;
   }
}
