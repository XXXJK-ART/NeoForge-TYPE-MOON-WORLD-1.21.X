package net.xxxjk.TYPE_MOON_WORLD.servant.card;

import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

public final class ServantCardSkillCostRules {
   private ServantCardSkillCostRules() {
   }

   public static double effectiveMpCost(TypeMoonWorldModVariables.PlayerVariables vars, ServantCardSkillAction action) {
      if ("emiya_archer".equals(vars.servant_card_id) && "emiya_cycle".equals(action.effectId())) {
         return ServantCardEmiyaSkills.nextEmiyaCycleMode(vars) == 1 ? 100.0 : 120.0;
      }
      return effectiveMpCost(vars.servant_card_id, vars.servant_card_transformed, action);
   }

   static double effectiveMpCost(String servantId, boolean transformed, ServantCardSkillAction action) {
      if ("medusa_mystic_eyes".equals(action.effectId()) && transformed) {
         return 0.0;
      }
      if (isReducedQuarterCostServant(servantId)) {
         return Math.round(action.mpCost() * 0.25);
      }
      return action.mpCost();
   }

   private static boolean isReducedQuarterCostServant(String servantId) {
      return "li_shuwen".equals(servantId) || "sasaki_kojiro".equals(servantId);
   }
}
