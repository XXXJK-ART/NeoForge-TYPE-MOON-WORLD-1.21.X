package net.xxxjk.TYPE_MOON_WORLD.servant.card;

import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

public final class ServantCardSkillCostRules {
   private ServantCardSkillCostRules() {
   }

   public static double effectiveMpCost(TypeMoonWorldModVariables.PlayerVariables vars, ServantCardSkillAction action) {
      if ("emiya_archer".equals(vars.servant_card_id) && "emiya_cycle".equals(action.effectId())) {
         return ServantCardEmiyaSkills.nextEmiyaCycleMode(vars) == 1 ? 100.0 : 120.0;
      }
      if ("medusa_mystic_eyes".equals(action.effectId()) && vars.servant_card_transformed) {
         return 0.0;
      }
      return action.mpCost();
   }
}
